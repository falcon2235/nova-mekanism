package com.falcon2235.moremultiblock.menu;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.blockentity.PbfBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class PbfMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 3; // input, fuel, output
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    public final PbfBlockEntity be;

    public PbfMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, (PbfBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public PbfMenu(int id, Inventory playerInventory, PbfBlockEntity be) {
        super(MMMRegistry.PBF_MENU.get(), id);
        this.be = be;

        addSlot(new SlotItemHandler(be.getInputs(), 0, 44, 25));  // iron
        addSlot(new SlotItemHandler(be.getInputs(), 1, 44, 47));  // coal
        addSlot(new SlotItemHandler(be.getOutputs(), 0, 116, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(be.dataAccess);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return be != null && be.stillValid(player);
    }
}
