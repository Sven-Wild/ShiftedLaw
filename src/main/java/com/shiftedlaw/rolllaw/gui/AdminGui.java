package com.shiftedlaw.rolllaw.gui;

import com.shiftedlaw.rolllaw.LawManager;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class AdminGui {

	private AdminGui() {
	}

	public static void open(ServerPlayerEntity operator, LawManager lawManager, MinecraftServer server) {
		List<ServerPlayerEntity> targets = server.getPlayerManager().getPlayerList();

		operator.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, playerInventory, player) ->
						new LawAssignScreenHandler(syncId, playerInventory, targets, lawManager, server, operator),
				Text.literal("Assign Laws")));
	}
}
