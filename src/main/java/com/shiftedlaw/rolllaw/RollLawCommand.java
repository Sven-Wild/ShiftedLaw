package com.shiftedlaw.rolllaw;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Registers the {@code /rolllaw} command. Kept in its own class so the
 * registration wiring (what gets bound to the dispatcher) is separate from
 * the execution logic (what runs when a player types the command).
 */
public final class RollLawCommand {

	private RollLawCommand() {
	}

	public static void register(LawManager lawManager) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				registerCommand(dispatcher, lawManager));
	}

	private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, LawManager lawManager) {
		dispatcher.register(CommandManager.literal("rolllaw")
				.requires(source -> true)
				.executes(context -> execute(context, lawManager)));

		RollLawMod.LOGGER.info("Registered /rolllaw command.");
	}

	private static int execute(CommandContext<ServerCommandSource> context, LawManager lawManager)
			throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		if (player == null) {
			source.sendError(Text.literal("Only players can roll for a Law."));
			return 0;
		}

		lawManager.startRoll(player, source.getServer());
		return 1;
	}
}
