package com.shiftedlaw.rolllaw;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which Law each player holds, the pool of unclaimed Laws, the
 * "? ? ?" roll animation, the shared countdown once everyone has rolled,
 * the running challenge timer, and the "everyone died" banishment
 * punishment before a fresh round begins.
 */
public class LawManager {

	private static final int ROLL_TOTAL_TICKS = 30;
	private static final int ROLL_PULSE_INTERVAL = 6;
	private static final int COUNTDOWN_TOTAL_TICKS = 200;
	private static final int PUNISHMENT_TOTAL_TICKS = 30 * 20;
	private static final int PUNISHMENT_DISTANCE = 6000;
	private static final String OBJECTIVE_TEXT = "Having everyone alive and beating the dragon";

	private static final String SUSPENSE_TEXT = "> ? ? ? <";

	private final List<Law> pool = new ArrayList<>(List.of(Law.values()));
	private final Map<UUID, Law> assignedLaws = new HashMap<>();
	private final Set<UUID> eliminated = new HashSet<>();
	private final List<RollAnimation> activeRolls = new ArrayList<>();
	private final Random random = new Random();

	private boolean countdownActive = false;
	private int countdownTicks = 0;
	private ServerBossBar countdownBossBar;

	private boolean punishmentActive = false;
	private int punishmentTicksRemaining = 0;
	private ServerWorld punishmentWorld;
	private double punishmentX;
	private double punishmentY;
	private double punishmentZ;

	private int elapsedTicks = 0;
	private int bestTimeTicks = -1;

	public Law getLaw(UUID playerId) {
		return assignedLaws.get(playerId);
	}

	public boolean isCountdownActive() {
		return countdownActive;
	}

	public boolean isEliminated(UUID playerId) {
		return eliminated.contains(playerId);
	}

	public void startRoll(ServerPlayerEntity player, MinecraftServer server) {
		if (assignedLaws.containsKey(player.getUuid())) {
			player.sendMessage(Text.literal("You already have a Law assigned!").formatted(Formatting.RED), false);
			return;
		}
		if (isRolling(player)) {
			player.sendMessage(Text.literal("You are already rolling!").formatted(Formatting.RED), false);
			return;
		}
		if (pool.isEmpty()) {
			player.sendMessage(Text.literal("All " + Law.values().length + " Laws have already been claimed!").formatted(Formatting.RED), false);
			return;
		}

		Law chosen = pool.remove(random.nextInt(pool.size()));
		beginRollAnimation(player, chosen, server);
	}

	/**
	 * OP-only: begin the roll animation for {@code player} locked to
	 * {@code requestedLaw}. Falls back to a random Law from the pool if
	 * {@code requestedLaw} is null or has already been claimed by someone
	 * else. Silently does nothing if the player already has a Law, is
	 * already rolling, or the pool is empty.
	 */
	public void adminAssignLaw(ServerPlayerEntity player, Law requestedLaw, MinecraftServer server) {
		if (assignedLaws.containsKey(player.getUuid()) || isRolling(player)) {
			return;
		}

		Law chosen;
		if (requestedLaw != null && pool.remove(requestedLaw)) {
			chosen = requestedLaw;
		} else if (!pool.isEmpty()) {
			chosen = pool.remove(random.nextInt(pool.size()));
		} else {
			return;
		}

		beginRollAnimation(player, chosen, server);
	}

	private void beginRollAnimation(ServerPlayerEntity player, Law chosen, MinecraftServer server) {
		activeRolls.add(new RollAnimation(player, chosen));
		broadcast(server, Text.literal(player.getName().getString() + " is rolling for a Law...").formatted(Formatting.GOLD));
	}

	public void tick(MinecraftServer server) {
		elapsedTicks++;
		if (elapsedTicks % 20 == 0) {
			broadcastActionBar(server, Text.literal("⏱ " + formatTimer(elapsedTicks) + " — " + OBJECTIVE_TEXT).formatted(Formatting.GOLD));
		}

		Iterator<RollAnimation> iterator = activeRolls.iterator();
		while (iterator.hasNext()) {
			RollAnimation animation = iterator.next();
			animation.ticksElapsed++;

			if (animation.ticksElapsed < ROLL_TOTAL_TICKS && animation.ticksElapsed % ROLL_PULSE_INTERVAL == 0) {
				broadcastPulse(server, animation);
			}

			if (animation.ticksElapsed >= ROLL_TOTAL_TICKS) {
				iterator.remove();
				finishRoll(server, animation);
			}
		}

		if (countdownActive) {
			tickCountdown(server);
		}
		if (punishmentActive) {
			tickPunishment(server);
		}
	}

	public void onPlayerDeath(ServerPlayerEntity player, MinecraftServer server) {
		if (server == null) {
			return;
		}
		Law law = assignedLaws.get(player.getUuid());
		if (law == null) {
			return;
		}

		eliminated.add(player.getUuid());
		player.changeGameMode(GameMode.SPECTATOR);

		broadcast(server, Text.literal(player.getName().getString() + " died and is now spectating! Their Law ("
				+ law.getDisplayName() + ") stays the same. Countdown reset.").formatted(Formatting.RED));
		broadcastTitle(server, Text.literal(player.getName().getString() + " eliminated!").formatted(Formatting.RED, Formatting.BOLD),
				Text.literal("Countdown reset").formatted(Formatting.GRAY));
		broadcastSound(server, SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
		LawScoreboard.show(server, this);

		checkLastStanding(server);
		cancelCountdown(server);
		checkRoundReset(server);
	}

	private void checkLastStanding(MinecraftServer server) {
		if (assignedLaws.size() <= 1) {
			return;
		}
		List<UUID> remaining = assignedLaws.keySet().stream().filter(id -> !eliminated.contains(id)).toList();
		if (remaining.size() == 1) {
			ServerPlayerEntity survivor = server.getPlayerManager().getPlayer(remaining.get(0));
			String name = survivor != null ? survivor.getName().getString() : "Someone";
			broadcastTitle(server, Text.literal("LAST ONE STANDING").formatted(Formatting.GOLD, Formatting.BOLD),
					Text.literal(name).formatted(Formatting.YELLOW));
			broadcastSound(server, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
		}
	}

	public void onPlayerJoin(ServerPlayerEntity player, MinecraftServer server) {
		checkCountdown(server);
	}

	public void onPlayerDisconnect(ServerPlayerEntity player, MinecraftServer server) {
		if (player == null) {
			return;
		}
		activeRolls.removeIf(animation -> animation.player.getUuid().equals(player.getUuid()));
		eliminated.remove(player.getUuid());
		Law law = assignedLaws.remove(player.getUuid());
		if (law != null) {
			pool.add(law);
		}
		checkCountdown(server);
		checkRoundReset(server);
	}

	private boolean isRolling(ServerPlayerEntity player) {
		for (RollAnimation animation : activeRolls) {
			if (animation.player.getUuid().equals(player.getUuid())) {
				return true;
			}
		}
		return false;
	}

	private void broadcastPulse(MinecraftServer server, RollAnimation animation) {
		broadcast(server, Text.literal(animation.player.getName().getString() + " " + SUSPENSE_TEXT).formatted(Formatting.GRAY, Formatting.BOLD));
	}

	private void finishRoll(MinecraftServer server, RollAnimation animation) {
		assignedLaws.put(animation.player.getUuid(), animation.resultLaw);

		broadcast(server, Text.literal("=== " + animation.player.getName().getString() + " rolled: "
				+ animation.resultLaw.getDisplayName() + " ===").formatted(Formatting.GOLD, Formatting.BOLD));

		for (String pro : animation.resultLaw.getPros()) {
			broadcast(server, Text.literal("  + " + pro).formatted(Formatting.GREEN));
		}
		for (String con : animation.resultLaw.getCons()) {
			broadcast(server, Text.literal("  - " + con).formatted(Formatting.RED));
		}

		broadcastTitle(server, Text.literal(animation.player.getName().getString() + " rolled!").formatted(Formatting.GOLD, Formatting.BOLD),
				Text.literal(animation.resultLaw.getDisplayName()).formatted(Formatting.YELLOW));
		broadcastSound(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
		LawScoreboard.show(server, this);

		checkCountdown(server);
	}

	private void checkCountdown(MinecraftServer server) {
		if (server == null) {
			return;
		}
		List<ServerPlayerEntity> active = server.getPlayerManager().getPlayerList().stream()
				.filter(p -> !eliminated.contains(p.getUuid()))
				.toList();
		boolean allHaveLaws = !active.isEmpty() && active.stream().allMatch(p -> assignedLaws.containsKey(p.getUuid()));

		if (allHaveLaws && !countdownActive) {
			startCountdown(server);
		} else if (!allHaveLaws && countdownActive) {
			cancelCountdown(server);
		}
	}

	private void checkRoundReset(MinecraftServer server) {
		if (assignedLaws.isEmpty() || punishmentActive) {
			return;
		}
		boolean everyoneEliminated = assignedLaws.keySet().stream().allMatch(eliminated::contains);
		if (everyoneEliminated) {
			startPunishment(server);
		}
	}

	/** Human-readable best successful run time, or a placeholder if none yet. */
	public String getBestTimeDisplay() {
		return bestTimeTicks < 0 ? "No successful run yet!" : formatTimer(bestTimeTicks);
	}

	/**
	 * Called when the Ender Dragon dies during an active run: stops and
	 * records the timer if it's a new best, celebrates, then starts a fresh
	 * round so the next attempt can begin.
	 */
	public void onDragonDefeated(MinecraftServer server) {
		if (server == null) {
			return;
		}
		String finalTime = formatTimer(elapsedTicks);
		boolean isNewBest = bestTimeTicks < 0 || elapsedTicks < bestTimeTicks;
		if (isNewBest) {
			bestTimeTicks = elapsedTicks;
		}

		broadcast(server, Text.literal("The dragon has been defeated in " + finalTime + "!"
				+ (isNewBest ? " New best time!" : "")).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
		broadcastTitle(server, Text.literal("VICTORY!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
				Text.literal(finalTime + (isNewBest ? " - New Best!" : "")).formatted(Formatting.GOLD));
		broadcastSound(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.7f);

		resetRound(server, Text.literal("Victory! Roll your Law again with /rolllaw to start a new run.")
				.formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
	}

	/**
	 * OP-only: force the countdown to start right now for whoever currently
	 * has a Law, without waiting for every online player to have rolled.
	 */
	public void adminForceStart(ServerPlayerEntity source, MinecraftServer server) {
		if (assignedLaws.isEmpty()) {
			source.sendMessage(Text.literal("Nobody has a Law yet - nothing to start.").formatted(Formatting.RED), false);
			return;
		}
		if (countdownActive) {
			source.sendMessage(Text.literal("The countdown is already running.").formatted(Formatting.RED), false);
			return;
		}
		startCountdown(server);
	}

	/**
	 * OP-only: force a full round reset right now, regardless of whether
	 * anyone has actually died. Cancels any banishment punishment in progress.
	 */
	public void adminForceReset(MinecraftServer server) {
		punishmentActive = false;
		resetRound(server, Text.literal("An operator reset the round - roll your Law again with /rolllaw!")
				.formatted(Formatting.AQUA, Formatting.BOLD));
	}

	/**
	 * Teleports everyone 6000 blocks from world spawn (one shared, randomly
	 * chosen direction), freezes them there via constant re-teleporting
	 * (works regardless of gamemode, unlike the Slowness-based freeze, since
	 * everyone is already in spectator by this point), and applies Darkness
	 * for the punishment's duration. Automatically rolls into a full round
	 * reset once the timer runs out.
	 */
	private void startPunishment(MinecraftServer server) {
		punishmentWorld = server.getOverworld();
		BlockPos spawn = punishmentWorld.getSpawnPos();
		double angle = random.nextDouble() * Math.PI * 2;
		int targetX = spawn.getX() + (int) Math.round(Math.cos(angle) * PUNISHMENT_DISTANCE);
		int targetZ = spawn.getZ() + (int) Math.round(Math.sin(angle) * PUNISHMENT_DISTANCE);

		punishmentWorld.getChunk(targetX >> 4, targetZ >> 4);
		int targetY = punishmentWorld.getTopY(Heightmap.Type.WORLD_SURFACE, targetX, targetZ);

		punishmentActive = true;
		punishmentTicksRemaining = PUNISHMENT_TOTAL_TICKS;
		punishmentX = targetX + 0.5;
		punishmentY = targetY;
		punishmentZ = targetZ + 0.5;

		elapsedTicks = 0;

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.teleport(punishmentWorld, punishmentX, punishmentY, punishmentZ, player.getYaw(), player.getPitch());
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, PUNISHMENT_TOTAL_TICKS + 20, 0, true, false, false));
		}

		broadcast(server, Text.literal("Everyone has fallen! Banished into the dark, 6000 blocks from home...")
				.formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
		broadcastTitle(server, Text.literal("YOU HAVE FAILED").formatted(Formatting.DARK_RED, Formatting.BOLD),
				Text.literal("Banished into the darkness...").formatted(Formatting.GRAY));
		broadcastSound(server, SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
	}

	private void tickPunishment(MinecraftServer server) {
		punishmentTicksRemaining--;

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.teleport(punishmentWorld, punishmentX, punishmentY, punishmentZ, player.getYaw(), player.getPitch());
		}

		if (punishmentTicksRemaining <= 0) {
			punishmentActive = false;
			resetRound(server, Text.literal("A new attempt begins - roll your Law again with /rolllaw!")
					.formatted(Formatting.AQUA, Formatting.BOLD));
		}
	}

	private void resetRound(MinecraftServer server, Text announcement) {
		assignedLaws.clear();
		eliminated.clear();
		pool.clear();
		pool.addAll(List.of(Law.values()));
		cancelCountdown(server);
		LawScoreboard.hide(server);

		ServerWorld overworld = server.getOverworld();
		BlockPos spawn = overworld.getSpawnPos();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.changeGameMode(GameMode.SURVIVAL);
			player.teleport(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
			player.setHealth(player.getMaxHealth());
			player.getHungerManager().setFoodLevel(20);
			player.removeStatusEffect(StatusEffects.DARKNESS);
		}

		broadcast(server, announcement);
		broadcastTitle(server, Text.literal("New Round!").formatted(Formatting.AQUA, Formatting.BOLD),
				Text.literal("Roll again with /rolllaw").formatted(Formatting.GRAY));
		broadcastSound(server, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
	}

	private void startCountdown(MinecraftServer server) {
		countdownActive = true;
		countdownTicks = COUNTDOWN_TOTAL_TICKS;
		broadcast(server, Text.literal("Everyone has their Law! Countdown starting...").formatted(Formatting.AQUA, Formatting.BOLD));
		broadcastSound(server, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.0f);

		countdownBossBar = new ServerBossBar(Text.literal("Get ready...").formatted(Formatting.GOLD),
				BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);
		countdownBossBar.setPercent(1.0f);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (assignedLaws.containsKey(player.getUuid()) && !eliminated.contains(player.getUuid())) {
				countdownBossBar.addPlayer(player);
			}
		}
	}

	private void cancelCountdown(MinecraftServer server) {
		if (countdownActive) {
			countdownActive = false;
			countdownTicks = 0;
			broadcast(server, Text.literal("Countdown reset!").formatted(Formatting.RED, Formatting.BOLD));
		}
		clearBossBar();
	}

	private void tickCountdown(MinecraftServer server) {
		countdownTicks--;

		if (countdownBossBar != null) {
			countdownBossBar.setPercent(Math.max(0.0f, (float) countdownTicks / COUNTDOWN_TOTAL_TICKS));
		}

		if (countdownTicks <= 0) {
			countdownActive = false;
			unfreezeParticipants(server);
			clearBossBar();
			broadcast(server, Text.literal("⚡ GO! ⚡").formatted(Formatting.YELLOW, Formatting.BOLD));
			broadcastTitle(server, Text.literal("⚡ GO! ⚡").formatted(Formatting.YELLOW, Formatting.BOLD), Text.empty());
			broadcastSound(server, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
			return;
		}

		if (countdownTicks % 20 == 0) {
			int secondsRemaining = countdownTicks / 20;
			if (secondsRemaining >= 1 && secondsRemaining <= 5) {
				if (countdownBossBar != null) {
					countdownBossBar.setName(Text.literal(secondsRemaining + "...").formatted(Formatting.GOLD, Formatting.BOLD));
				}
				broadcast(server, Text.literal(String.valueOf(secondsRemaining)).formatted(Formatting.RED, Formatting.BOLD));
				broadcastTitle(server, Text.literal(String.valueOf(secondsRemaining)).formatted(Formatting.RED, Formatting.BOLD), Text.empty());
				broadcastSound(server, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
			}
		}
	}

	private void clearBossBar() {
		if (countdownBossBar != null) {
			countdownBossBar.clearPlayers();
			countdownBossBar = null;
		}
	}

	private void unfreezeParticipants(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (assignedLaws.containsKey(player.getUuid()) && !eliminated.contains(player.getUuid())) {
				player.removeStatusEffect(StatusEffects.SLOWNESS);
			}
		}
	}

	private static String formatTimer(int ticks) {
		int totalSeconds = ticks / 20;
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format("%02d:%02d", minutes, seconds);
	}

	private void broadcast(MinecraftServer server, Text text) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(text, false);
		}
	}

	private void broadcastActionBar(MinecraftServer server, Text text) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(text, true);
		}
	}

	private void broadcastTitle(MinecraftServer server, Text title, Text subtitle) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			LawTitles.sendTitle(player, title, subtitle);
		}
	}

	private void broadcastSound(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.getWorld().playSound(null, player.getBlockPos(), sound, SoundCategory.PLAYERS, volume, pitch);
		}
	}

	private static final class RollAnimation {
		private final ServerPlayerEntity player;
		private final Law resultLaw;
		private int ticksElapsed = 0;

		private RollAnimation(ServerPlayerEntity player, Law resultLaw) {
			this.player = player;
			this.resultLaw = resultLaw;
		}
	}
}
