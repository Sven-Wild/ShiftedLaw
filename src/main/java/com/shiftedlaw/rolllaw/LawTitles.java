package com.shiftedlaw.rolllaw;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Sends big on-screen title/subtitle text to a player via raw title packets. */
final class LawTitles {

	private LawTitles() {
	}

	static void sendTitle(ServerPlayerEntity player, Text title, Text subtitle) {
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 10));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		player.networkHandler.sendPacket(new TitleS2CPacket(title));
	}
}
