package com.shiftedlaw.rolllaw.gui;

import com.shiftedlaw.rolllaw.Law;
import com.shiftedlaw.rolllaw.LawManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An OP-only Law assignment menu built as a plain generic 9x6 container, so
 * it renders with Minecraft's own built-in chest screen - no custom
 * client-side rendering code required. Clicking a player's slot cycles their
 * pending pick through "Random" and all 20 Laws; clicking Start locks in
 * every pick and hands the rest off to {@link LawManager#adminAssignLaw}.
 */
public class LawAssignScreenHandler extends ScreenHandler {

	private static final int START_SLOT = 53;

	private final SimpleInventory menuInventory = new SimpleInventory(54);
	private final List<ServerPlayerEntity> targets;
	private final Map<UUID, Integer> selections = new HashMap<>();
	private final LawManager lawManager;
	private final MinecraftServer server;
	private final ServerPlayerEntity operator;

	public LawAssignScreenHandler(int syncId, PlayerInventory playerInventory, List<ServerPlayerEntity> targets,
			LawManager lawManager, MinecraftServer server, ServerPlayerEntity operator) {
		super(ScreenHandlerType.GENERIC_9X6, syncId);
		this.targets = targets;
		this.lawManager = lawManager;
		this.server = server;
		this.operator = operator;

		for (ServerPlayerEntity target : targets) {
			selections.put(target.getUuid(), -1);
		}
		refreshMenuItems();

		for (int i = 0; i < 54; i++) {
			addSlot(new Slot(menuInventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
		}
	}

	private void refreshMenuItems() {
		for (int i = 0; i < targets.size() && i < START_SLOT; i++) {
			ServerPlayerEntity target = targets.get(i);
			int selection = selections.get(target.getUuid());
			String label = selection < 0 ? "Random" : Law.values()[selection].getDisplayName();

			ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
			stack.set(DataComponentTypes.CUSTOM_NAME,
					Text.literal(target.getName().getString() + " -> " + label).formatted(Formatting.YELLOW));
			menuInventory.setStack(i, stack);
		}

		ItemStack startStack = new ItemStack(Items.EMERALD_BLOCK);
		startStack.set(DataComponentTypes.CUSTOM_NAME,
				Text.literal("Start Round").formatted(Formatting.GREEN, Formatting.BOLD));
		menuInventory.setStack(START_SLOT, startStack);
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex < 0 || slotIndex >= 54 || actionType != SlotActionType.PICKUP) {
			return;
		}

		if (slotIndex == START_SLOT) {
			startRound();
			return;
		}

		if (slotIndex < targets.size()) {
			ServerPlayerEntity target = targets.get(slotIndex);
			int next = selections.get(target.getUuid()) + 1;
			if (next >= Law.values().length) {
				next = -1;
			}
			selections.put(target.getUuid(), next);
			refreshMenuItems();
			sendContentUpdates();
		}
	}

	private void startRound() {
		for (ServerPlayerEntity target : targets) {
			int selection = selections.get(target.getUuid());
			Law requested = selection < 0 ? null : Law.values()[selection];
			lawManager.adminAssignLaw(target, requested, server);
		}
		operator.closeHandledScreen();
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}
}
