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
 * Registers the {@code /rolllaw} command (open to everyone) and its
 * {@code admin} subcommands (operators only). Kept in its own class so the
 * registration wiring is separate from the execution logic.
 */
public final class RollLawCommand {

	private static final int OP_PERMISSION_LEVEL = 2;

	private RollLawCommand() {
	}

	public static void register(LawManager lawManager) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				registerCommand(dispatcher, lawManager));
	}

	private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, LawManager lawManager) {
		dispatcher.register(CommandManager.literal("rolllaw")
				.requires(source -> true)
				.executes(context -> executeRoll(context, lawManager))
				.then(CommandManager.literal("admin")
						.requires(source -> source.hasPermissionLevel(OP_PERMISSION_LEVEL))
						.then(CommandManager.literal("start")
								.executes(context -> executeAdminStart(context, lawManager)))
						.then(CommandManager.literal("reset")
								.executes(context -> executeAdminReset(context, lawManager)))));

		RollLawMod.LOGGER.info("Registered /rolllaw command.");
	}

	private static int executeRoll(CommandContext<ServerCommandSource> context, LawManager lawManager)
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

	private static int executeAdminStart(CommandContext<ServerCommandSource> context, LawManager lawManager)
			throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		if (player == null) {
			source.sendError(Text.literal("Only players can use this command."));
			return 0;
		}

		lawManager.adminForceStart(player, source.getServer());
		return 1;
	}

	private static int executeAdminReset(CommandContext<ServerCommandSource> context, LawManager lawManager) {
		lawManager.adminForceReset(context.getSource().getServer());
		return 1;
	}
}
