package com.shiftedlaw.rolllaw;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

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
 * "? ? ?" roll animation, and the shared countdown once everyone has rolled.
 */
public class LawManager {

	private static final int ROLL_TOTAL_TICKS = 30;
	private static final int ROLL_PULSE_INTERVAL = 6;
	private static final int COUNTDOWN_TOTAL_TICKS = 200;

	private static final String SUSPENSE_TEXT = "> ? ? ? <";

	private final List<Law> pool = new ArrayList<>(List.of(Law.values()));
	private final Map<UUID, Law> assignedLaws = new HashMap<>();
	private final Set<UUID> eliminated = new HashSet<>();
	private final List<RollAnimation> activeRolls = new ArrayList<>();
	private final Random random = new Random();

	private boolean countdownActive = false;
	private int countdownTicks = 0;

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
			player.sendMessage(Text.literal("All 5 Laws have already been claimed!").formatted(Formatting.RED), false);
			return;
		}

		Law chosen = pool.remove(random.nextInt(pool.size()));
		activeRolls.add(new RollAnimation(player, chosen));

		broadcast(server, Text.literal(player.getName().getString() + " is rolling for a Law...").formatted(Formatting.GOLD));
	}

	public void tick(MinecraftServer server) {
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
		cancelCountdown(server);
		checkRoundReset(server);
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
		if (assignedLaws.isEmpty()) {
			return;
		}
		boolean everyoneEliminated = assignedLaws.keySet().stream().allMatch(eliminated::contains);
		if (everyoneEliminated) {
			resetRound(server);
		}
	}

	private void resetRound(MinecraftServer server) {
		assignedLaws.clear();
		eliminated.clear();
		pool.clear();
		pool.addAll(List.of(Law.values()));
		cancelCountdown(server);

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.changeGameMode(GameMode.SURVIVAL);
			player.setHealth(player.getMaxHealth());
			player.getHungerManager().setFoodLevel(20);
		}

		broadcast(server, Text.literal("Everyone has fallen! Starting a new round - roll your Law again with /rolllaw!")
				.formatted(Formatting.AQUA, Formatting.BOLD));
	}

	private void startCountdown(MinecraftServer server) {
		countdownActive = true;
		countdownTicks = COUNTDOWN_TOTAL_TICKS;
		broadcast(server, Text.literal("Everyone has their Law! Countdown starting...").formatted(Formatting.AQUA, Formatting.BOLD));
	}

	private void cancelCountdown(MinecraftServer server) {
		if (countdownActive) {
			countdownActive = false;
			countdownTicks = 0;
			broadcast(server, Text.literal("Countdown reset!").formatted(Formatting.RED, Formatting.BOLD));
		}
	}

	private void tickCountdown(MinecraftServer server) {
		countdownTicks--;

		if (countdownTicks <= 0) {
			countdownActive = false;
			unfreezeParticipants(server);
			broadcast(server, Text.literal("⚡ GO! ⚡").formatted(Formatting.YELLOW, Formatting.BOLD));
			return;
		}

		if (countdownTicks % 20 == 0) {
			int secondsRemaining = countdownTicks / 20;
			if (secondsRemaining >= 1 && secondsRemaining <= 5) {
				broadcast(server, Text.literal(String.valueOf(secondsRemaining)).formatted(Formatting.RED, Formatting.BOLD));
			}
		}
	}

	private void unfreezeParticipants(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (assignedLaws.containsKey(player.getUuid()) && !eliminated.contains(player.getUuid())) {
				player.removeStatusEffect(StatusEffects.SLOWNESS);
			}
		}
	}

	private void broadcast(MinecraftServer server, Text text) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(text, false);
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
