package com.falcon2235.moremultiblock.menu;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.block.PortBlock;
import com.falcon2235.moremultiblock.blockentity.PortBlockEntity;
import com.falcon2235.moremultiblock.blockentity.PortHost;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for port blocks. Besides the auto-transfer toggle (menu button 0), non-energy
 * ports expose direct access:
 * <ul>
 *   <li>item ports show the machine's input/output slots,</li>
 *   <li>gas/fluid ports show a tank-item slot that fills or drains against the machine.</li>
 * </ul>
 * The host controller position travels in the open-screen buffer so the client builds
 * the exact same slot list as the server.
 */
public class PortMenu extends AbstractContainerMenu {

    /** Content row / player inventory layout, shared with the screen. */
    public static final int CONTENT_Y = 38;
    public static final int PLAYER_INV_Y = 70;

    public final PortBlockEntity be;
    private final int machineSlots;

    public PortMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, readPort(playerInventory, buf), readHost(playerInventory, buf));
    }

    private static PortBlockEntity readPort(Inventory inv, FriendlyByteBuf buf) {
        return (PortBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos());
    }

    @Nullable
    private static PortHost readHost(Inventory inv, FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof PortHost host ? host : null;
    }

    public PortMenu(int id, Inventory playerInventory, PortBlockEntity be) {
        this(id, playerInventory, be, be.resolveHost());
    }

    public PortMenu(int id, Inventory playerInventory, PortBlockEntity be, @Nullable PortHost host) {
        super(MMMRegistry.PORT_MENU.get(), id);
        this.be = be;

        PortBlock.PortType type = be.portType();
        int added = 0;
        switch (type) {
            case ITEM_INPUT -> {
                if (host != null) {
                    var handler = host.getInputs();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        addSlot(new SlotItemHandler(handler, i, 8 + i * 18, CONTENT_Y));
                        added++;
                    }
                }
            }
            case ITEM_OUTPUT -> {
                if (host != null) {
                    var handler = host.getOutputs();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        addSlot(new SlotItemHandler(handler, i, 8 + i * 18, CONTENT_Y) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return false;
                            }
                        });
                        added++;
                    }
                }
            }
            case GAS_INPUT, GAS_OUTPUT, FLUID_INPUT, FLUID_OUTPUT -> {
                addSlot(new SlotItemHandler(be.getContainerSlot(), 0, 116, CONTENT_Y));
                added++;
            }
            default -> {
            }
        }
        this.machineSlots = added;

        if (type != PortBlock.PortType.ENERGY) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
            }
        }

        addDataSlots(be.dataAccess);
    }

    public int getMachineSlots() {
        return machineSlots;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == 0) {
            be.toggleAuto();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        int playerStart = machineSlots;
        int playerEnd = slots.size();
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (machineSlots == 0 || !moveItemStackTo(stack, 0, machineSlots, false)) {
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
