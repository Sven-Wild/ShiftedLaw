package com.shiftedlaw.rolllaw.client;

import com.shiftedlaw.rolllaw.network.OpenAdminGuiPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only entrypoint. Registers the keybinding that requests the
 * OP-only Law assignment menu; the actual menu is a server-driven
 * generic container screen, so no custom rendering code lives here.
 */
public class ShiftedLawClient implements ClientModInitializer {

	private static final KeyBinding OPEN_ADMIN_GUI_KEY = new KeyBinding(
			"key.shiftedlaw.open_admin_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			"category.shiftedlaw.general"
	);

	@Override
	public void onInitializeClient() {
		PayloadTypeRegistry.playC2S().register(OpenAdminGuiPayload.ID, OpenAdminGuiPayload.CODEC);
		KeyBindingHelper.registerKeyBinding(OPEN_ADMIN_GUI_KEY);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_ADMIN_GUI_KEY.wasPressed()) {
				ClientPlayNetworking.send(new OpenAdminGuiPayload());
			}
		});
	}
}
