package com.shiftedlaw.rolllaw.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.shiftedlaw.rolllaw.RollLawMod;

/**
 * Client-to-server signal, carrying no data, requesting that the sender's
 * admin Law-assignment menu be opened. Permission checking happens entirely
 * server-side when this is received.
 */
public record OpenAdminGuiPayload() implements CustomPayload {
	public static final CustomPayload.Id<OpenAdminGuiPayload> ID =
			new CustomPayload.Id<>(Identifier.of(RollLawMod.MOD_ID, "open_admin_gui"));
	public static final PacketCodec<PacketByteBuf, OpenAdminGuiPayload> CODEC =
			PacketCodec.unit(new OpenAdminGuiPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
