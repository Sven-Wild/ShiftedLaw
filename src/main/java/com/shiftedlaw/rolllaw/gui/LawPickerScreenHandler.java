package com.shiftedlaw.rolllaw.gui;

import com.shiftedlaw.rolllaw.Law;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
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
 * Shows every Law as a coloured glass pane (cycling through all 16 dye
 * colours across the 20 Laws) plus a Random option and a Back button, so an
 * operator can see and pick from the full set at a glance instead of
 * clicking through them one at a time.
 */
public class LawPickerScreenHandler extends ScreenHandler {

	private static final int RANDOM_SLOT = 0;
	private static final int LAW_SLOT_START = 1;
	private static final int BACK_SLOT = 53;

	private static final Item[] GLASS_COLORS = {
			Items.WHITE_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE,
			Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE, Items.LIME_STAINED_GLASS_PANE,
			Items.PINK_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_STAINED_GLASS_PANE,
			Items.CYAN_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE,
			Items.BROWN_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE,
			Items.BLACK_STAINED_GLASS_PANE
	};

	private final SimpleInventory menuInventory = new SimpleInventory(54);
	private final AdminSession session;
	private final ServerPlayerEntity target;

	public LawPickerScreenHandler(int syncId, PlayerInventory playerInventory, AdminSession session, ServerPlayerEntity target) {
		super(ScreenHandlerType.GENERIC_9X6, syncId);
		this.session = session;
		this.target = target;

		populateMenuItems();

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

	private void populateMenuItems() {
		ItemStack randomStack = new ItemStack(Items.BARRIER);
		randomStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Random").formatted(Formatting.AQUA, Formatting.BOLD));
		menuInventory.setStack(RANDOM_SLOT, randomStack);

		Law[] laws = Law.values();
		for (int i = 0; i < laws.length; i++) {
			ItemStack stack = new ItemStack(GLASS_COLORS[i % GLASS_COLORS.length]);
			stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(laws[i].getDisplayName()).formatted(Formatting.YELLOW));
			menuInventory.setStack(LAW_SLOT_START + i, stack);
		}

		ItemStack backStack = new ItemStack(Items.ARROW);
		backStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("<- Back").formatted(Formatting.GRAY));
		menuInventory.setStack(BACK_SLOT, backStack);
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex < 0 || slotIndex >= 54 || actionType != SlotActionType.PICKUP) {
			return;
		}

		if (slotIndex == BACK_SLOT) {
			AdminGui.openMain(session);
			return;
		}

		if (slotIndex == RANDOM_SLOT) {
			session.selections.put(target.getUuid(), -1);
			AdminGui.openMain(session);
			return;
		}

		int lawIndex = slotIndex - LAW_SLOT_START;
		if (lawIndex >= 0 && lawIndex < Law.values().length) {
			session.selections.put(target.getUuid(), lawIndex);
			AdminGui.openMain(session);
		}
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
