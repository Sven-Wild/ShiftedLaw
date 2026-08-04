package com.shiftedlaw.rolllaw;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Applies the passive, per-tick gameplay effects for each Law and handles
 * the damage-related modifications (fall healing, halved fire damage, etc).
 */
public final class LawEffects {

	private static final Random RANDOM = new Random();
	private static final Map<UUID, Boolean> WAS_ON_GROUND = new HashMap<>();
	private static final int CROP_BOOST_RADIUS = 4;
	private static final float CROP_BOOST_CHANCE = 0.15f;
	private static final float FIRE_SPREAD_CHANCE = 0.5f;

	private static int tickCounter = 0;

	private LawEffects() {
	}

	public static void register(LawManager lawManager) {
		ServerTickEvents.END_SERVER_TICK.register(server -> onTick(server, lawManager));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> onAllowDamage(entity, source, amount, lawManager));
	}

	private static void onTick(MinecraftServer server, LawManager lawManager) {
		tickCounter++;

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			Law law = lawManager.getLaw(player.getUuid());
			if (law == null) {
				continue;
			}

			if (lawManager.isCountdownActive() && !lawManager.isEliminated(player.getUuid())) {
				freezePlayer(player);
			}

			switch (law) {
				case GRAVITY_ANCHOR -> handleGravityAnchor(player);
				case CULTIVATOR -> handleCultivator(player);
				case PHANTOM -> handlePhantom(player);
				case INFERNO -> handleInferno(player);
				case BEACON -> handleBeacon(server, player);
			}
		}
	}

	private static void freezePlayer(ServerPlayerEntity player) {
		// Slowness alone only throttles the client's own movement prediction; zeroing
		// velocity every tick also cancels any residual momentum server-side, and
		// velocityModified forces that zeroed velocity to actually sync to the client.
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 250, true, false, false));
		player.setVelocity(0.0, player.getVelocity().y, 0.0);
		player.velocityModified = true;
	}

	private static void handleGravityAnchor(ServerPlayerEntity player) {
		boolean onGround = player.isOnGround();
		boolean wasOnGround = WAS_ON_GROUND.getOrDefault(player.getUuid(), true);

		if (!onGround && wasOnGround && player.getVelocity().y > 0.1) {
			int foodLevel = player.getHungerManager().getFoodLevel();
			player.getHungerManager().setFoodLevel(Math.max(0, foodLevel - 1));
		}

		WAS_ON_GROUND.put(player.getUuid(), onGround);
	}

	private static void handleCultivator(ServerPlayerEntity player) {
		if (player.isSprinting()) {
			player.setSprinting(false);
			// The client predicts its own sprint speed locally, so just clearing the
			// server-side flag doesn't reliably stop it; a brief synced Slowness pulse
			// forces the client's effective speed back down every time sprint is tried.
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 1, true, false, false));
		}
		if (tickCounter % 20 == 0) {
			boostNearbyCrops(player);
		}
	}

	private static void boostNearbyCrops(ServerPlayerEntity player) {
		ServerWorld world = (ServerWorld) player.getWorld();
		BlockPos center = player.getBlockPos();

		for (int x = -CROP_BOOST_RADIUS; x <= CROP_BOOST_RADIUS; x++) {
			for (int z = -CROP_BOOST_RADIUS; z <= CROP_BOOST_RADIUS; z++) {
				for (int y = -1; y <= 1; y++) {
					BlockPos pos = center.add(x, y, z);
					BlockState state = world.getBlockState(pos);
					Block block = state.getBlock();
					if (block instanceof CropBlock && block instanceof Fertilizable fertilizable
							&& RANDOM.nextFloat() < CROP_BOOST_CHANCE
							&& fertilizable.isFertilizable(world, pos, state)) {
						fertilizable.grow(world, world.getRandom(), pos, state);
					}
				}
			}
		}
	}

	private static void handlePhantom(ServerPlayerEntity player) {
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, true, false, false));
		if (player.isSneaking()) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 0, true, false, false));
		}
	}

	private static void handleInferno(ServerPlayerEntity player) {
		if (tickCounter % 10 != 0 || !player.isOnFire()) {
			return;
		}

		ServerWorld world = (ServerWorld) player.getWorld();
		BlockPos base = player.getBlockPos();

		for (Direction direction : Direction.values()) {
			BlockPos neighbor = base.offset(direction);
			BlockState neighborState = world.getBlockState(neighbor);
			BlockState belowState = world.getBlockState(neighbor.down());

			if (neighborState.isAir() && !belowState.isAir() && RANDOM.nextFloat() < FIRE_SPREAD_CHANCE) {
				world.setBlockState(neighbor, Blocks.FIRE.getDefaultState());
				break;
			}
		}
	}

	private static void handleBeacon(MinecraftServer server, ServerPlayerEntity beaconHolder) {
		for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
			if (!other.getUuid().equals(beaconHolder.getUuid())) {
				other.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, false));
			}
		}
	}

	private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount, LawManager lawManager) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return true;
		}

		Law law = lawManager.getLaw(player.getUuid());
		if (law == null) {
			return true;
		}

		if (law == Law.GRAVITY_ANCHOR && source.isOf(DamageTypes.FALL)) {
			player.heal(amount);
			return false;
		}

		if (law == Law.INFERNO && isFireDamage(source)) {
			float reduced = amount * 0.5f;
			if (player.getHealth() - reduced > 0.5f) {
				player.setHealth(player.getHealth() - reduced);
				return false;
			}
			return true;
		}

		if (law == Law.BEACON) {
			float boosted = amount * 1.1f;
			if (player.getHealth() - boosted > 0.5f) {
				player.setHealth(player.getHealth() - boosted);
				return false;
			}
			return true;
		}

		return true;
	}

	private static boolean isFireDamage(DamageSource source) {
		return source.isOf(DamageTypes.IN_FIRE)
				|| source.isOf(DamageTypes.ON_FIRE)
				|| source.isOf(DamageTypes.LAVA)
				|| source.isOf(DamageTypes.HOT_FLOOR);
	}
}
