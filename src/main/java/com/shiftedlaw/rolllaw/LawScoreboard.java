package com.shiftedlaw.rolllaw;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Sidebar scoreboard listing every online player and their current Law
 * (or "Out" once eliminated) while a round is active. Rebuilt from scratch
 * on every refresh rather than mutating an existing objective's entries,
 * since that's simpler and this only updates on meaningful state changes
 * (a roll finishing, a death, a round start/reset), not every tick.
 */
final class LawScoreboard {

	private static final String OBJECTIVE_NAME = "shiftedlaw_status";

	private LawScoreboard() {
	}

	static void show(MinecraftServer server, LawManager lawManager) {
		Scoreboard scoreboard = server.getScoreboard();

		ScoreboardObjective existing = scoreboard.getNullableObjective(OBJECTIVE_NAME);
		if (existing != null) {
			scoreboard.removeObjective(existing);
		}

		ScoreboardObjective objective = scoreboard.addObjective(OBJECTIVE_NAME, ScoreboardCriterion.DUMMY,
				Text.literal("ShiftedLaw").formatted(Formatting.GOLD, Formatting.BOLD),
				ScoreboardCriterion.RenderType.INTEGER, false, null);
		scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);

		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		int score = players.size();
		for (ServerPlayerEntity player : players) {
			Law law = lawManager.getLaw(player.getUuid());
			String status = lawManager.isEliminated(player.getUuid()) ? "Out"
					: law != null ? law.getDisplayName() : "No Law";
			ScoreHolder holder = ScoreHolder.fromName(player.getName().getString() + ": " + status);
			scoreboard.getOrCreateScore(holder, objective).setScore(score--);
		}
	}

	static void hide(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();
		ScoreboardObjective objective = scoreboard.getNullableObjective(OBJECTIVE_NAME);
		if (objective != null) {
			scoreboard.removeObjective(objective);
		}
	}
}
