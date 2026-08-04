package com.shiftedlaw.rolllaw;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollLawMod implements ModInitializer {
	public static final String MOD_ID = "rolllaw";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final LawManager lawManager = new LawManager();

	@Override
	public void onInitialize() {
		LawEffects.register(lawManager);
		RollLawCommand.register(lawManager);

		ServerTickEvents.END_SERVER_TICK.register(lawManager::tick);

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayerEntity player) {
				lawManager.onPlayerDeath(player, player.getServer());
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				lawManager.onPlayerRespawn(newPlayer, newPlayer.getServer()));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				lawManager.onPlayerJoin(handler.player, server));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				lawManager.onPlayerDisconnect(handler.player, server));

		LOGGER.info("RollLaw initialized.");
	}
}
