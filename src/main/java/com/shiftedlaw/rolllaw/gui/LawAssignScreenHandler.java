package com.shiftedlaw.rolllaw.gui;

import com.shiftedlaw.rolllaw.Law;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The main Law-assignment menu: one real player-skin head per online
 * player. Clicking a head opens the Law picker for that player instead of
 * cycling in place, so every option is visible at once. Clicking Start
 * finalizes every pick.
 */
public class LawAssignScreenHandler extends ScreenHandler {

	private static final int START_SLOT = 53;

	private final SimpleInventory menuInventory = new SimpleInventory(54);
	private final AdminSession session;

	public LawAssignScreenHandler(int syncId, PlayerInventory playerInventory, AdminSession session) {
		super(ScreenHandlerType.GENERIC_9X6, syncId);
		this.session = session;

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
		for (int i = 0; i < session.targets.size() && i < START_SLOT; i++) {
			ServerPlayerEntity target = session.targets.get(i);
			int selection = session.selections.get(target.getUuid());
			String label = selection < 0 ? "Random" : Law.values()[selection].getDisplayName();

			ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
			stack.set(DataComponentTypes.PROFILE, new ProfileComponent(target.getGameProfile()));
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

		if (slotIndex < session.targets.size()) {
			AdminGui.openPicker(session, session.targets.get(slotIndex));
		}
	}

	private void startRound() {
		for (ServerPlayerEntity target : session.targets) {
			int selection = session.selections.get(target.getUuid());
			Law requested = selection < 0 ? null : Law.values()[selection];
			session.lawManager.adminAssignLaw(target, requested, session.server);
		}
		session.operator.closeHandledScreen();
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
