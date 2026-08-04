package com.shiftedlaw.rolllaw.gui;

import com.shiftedlaw.rolllaw.LawManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared state for one operator's Law-assignment session, kept alive across
 * the main assignment menu and the per-player Law picker menu so switching
 * between them doesn't lose anything. -1 in {@code selections} means Random.
 */
final class AdminSession {

	final List<ServerPlayerEntity> targets;
	final Map<UUID, Integer> selections = new HashMap<>();
	final LawManager lawManager;
	final MinecraftServer server;
	final ServerPlayerEntity operator;

	AdminSession(List<ServerPlayerEntity> targets, LawManager lawManager, MinecraftServer server, ServerPlayerEntity operator) {
		this.targets = targets;
		this.lawManager = lawManager;
		this.server = server;
		this.operator = operator;
		for (ServerPlayerEntity target : targets) {
			selections.put(target.getUuid(), -1);
		}
	}
}
