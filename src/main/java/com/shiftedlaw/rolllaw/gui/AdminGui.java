package com.shiftedlaw.rolllaw.gui;

import com.shiftedlaw.rolllaw.LawManager;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class AdminGui {

	private AdminGui() {
	}

	public static void open(ServerPlayerEntity operator, LawManager lawManager, MinecraftServer server) {
		AdminSession session = new AdminSession(server.getPlayerManager().getPlayerList(), lawManager, server, operator);
		openMain(session);
	}

	static void openMain(AdminSession session) {
		session.operator.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, playerInventory, player) -> new LawAssignScreenHandler(syncId, playerInventory, session),
				Text.literal("Assign Laws")));
	}

	static void openPicker(AdminSession session, ServerPlayerEntity target) {
		session.operator.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, playerInventory, player) -> new LawPickerScreenHandler(syncId, playerInventory, session, target),
				Text.literal("Pick a Law for " + target.getName().getString())));
	}
}
