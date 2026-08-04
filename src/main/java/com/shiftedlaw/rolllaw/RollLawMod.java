package com.shiftedlaw.rolllaw;

import com.shiftedlaw.rolllaw.gui.AdminGui;
import com.shiftedlaw.rolllaw.network.OpenAdminGuiPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollLawMod implements ModInitializer {
	public static final String MOD_ID = "shiftedlaw";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final int OP_PERMISSION_LEVEL = 2;

	private final LawManager lawManager = new LawManager();

	@Override
	public void onInitialize() {
		LawEffects.register(lawManager);
		RollLawCommand.register(lawManager);

		PayloadTypeRegistry.playC2S().register(OpenAdminGuiPayload.ID, OpenAdminGuiPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OpenAdminGuiPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (player.hasPermissionLevel(OP_PERMISSION_LEVEL)) {
				AdminGui.open(player, lawManager, player.getServer());
			} else {
				player.sendMessage(Text.literal("You must be an operator to open the Law menu.").formatted(Formatting.RED), false);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(lawManager::tick);

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayerEntity player) {
				lawManager.onPlayerDeath(player, player.getServer());
			} else if (entity instanceof EnderDragonEntity) {
				lawManager.onDragonDefeated(entity.getServer());
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				lawManager.onPlayerJoin(handler.player, server));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				lawManager.onPlayerDisconnect(handler.player, server));

		LOGGER.info("ShiftedLaw initialized.");
	}
}
