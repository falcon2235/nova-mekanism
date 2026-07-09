package com.falcon2235.moremultiblock.menu;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.blockentity.ControllerBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ControllerMenu extends AbstractContainerMenu {

    private static final int IN_START = 0;
    private static final int IN_END = ControllerBlockEntity.INPUT_SLOTS;
    private static final int OUT_START = IN_END;
    private static final int OUT_END = OUT_START + ControllerBlockEntity.OUTPUT_SLOTS;
    private static final int UPG_START = OUT_END;
    private static final int UPG_END = UPG_START + 2;
    private static final int PLAYER_START = UPG_END;
    private static final int PLAYER_END = PLAYER_START + 36;

    public final ControllerBlockEntity be;

    public ControllerMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, (ControllerBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ControllerMenu(int id, Inventory playerInventory, ControllerBlockEntity be) {
        super(MMMRegistry.CONTROLLER_MENU.get(), id);
        this.be = be;

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(be.getInputs(), row * 3 + col, 8 + col * 18, 25 + row * 18));
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(be.getOutputs(), row * 3 + col, 98 + col * 18, 25 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addSlot(new SlotItemHandler(be.getUpgrades(), 0, 155, 17));
        addSlot(new SlotItemHandler(be.getUpgrades(), 1, 155, 45));

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
        } else if (ControllerBlockEntity.isSpeedUpgrade(stack)) {
            if (!moveItemStackTo(stack, UPG_START, UPG_START + 1, false)
                    && !moveItemStackTo(stack, IN_START, IN_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ControllerBlockEntity.isEnergyUpgrade(stack)) {
            if (!moveItemStackTo(stack, UPG_START + 1, UPG_END, false)
                    && !moveItemStackTo(stack, IN_START, IN_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, IN_START, IN_END, false)) {
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
