package com.falcon2235.moremultiblock.menu;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.blockentity.ChemMachineBlockEntity;
import com.falcon2235.moremultiblock.blockentity.ControllerBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ChemMachineMenu extends AbstractContainerMenu {

    private static final int IN_START = 0;
    private static final int IN_END = ChemMachineBlockEntity.INPUT_SLOTS;
    private static final int OUT_START = IN_END;
    private static final int OUT_END = OUT_START + ChemMachineBlockEntity.OUTPUT_SLOTS;
    private static final int UPG_START = OUT_END;
    private static final int UPG_END = UPG_START + ChemMachineBlockEntity.UPGRADE_SLOTS;
    private static final int PLAYER_START = UPG_END;
    private static final int PLAYER_END = PLAYER_START + 36;

    public final ChemMachineBlockEntity be;

    public ChemMachineMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, (ChemMachineBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ChemMachineMenu(int id, Inventory playerInventory, ChemMachineBlockEntity be) {
        super(MMMRegistry.CHEM_MACHINE_MENU.get(), id);
        this.be = be;

        // Every slot is always registered so the indices quickMoveStack relies on stay
        // fixed; the ones this machine never uses are simply inactive, which stops
        // vanilla both drawing them and letting anything be put in them.
        var io = com.falcon2235.moremultiblock.machine.MachineIo.of(be.machineType());

        // 5 input slots: three across the top row, two on the bottom row
        int[][] inPos = {{26, 20}, {44, 20}, {62, 20}, {26, 42}, {44, 42}};
        for (int i = 0; i < ChemMachineBlockEntity.INPUT_SLOTS; i++) {
            final int index = i;
            addSlot(new SlotItemHandler(be.getInputs(), i, inPos[i][0], inPos[i][1]) {
                @Override
                public boolean isActive() {
                    return io.usesInputSlot(index);
                }
            });
        }
        // output 2x2 grid (centrifuge separations fill up to four different stacks)
        int[][] outPos = {{98, 25}, {116, 25}, {98, 47}, {116, 47}};
        for (int i = 0; i < ChemMachineBlockEntity.OUTPUT_SLOTS; i++) {
            final int index = i;
            addSlot(new SlotItemHandler(be.getOutputs(), i, outPos[i][0], outPos[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return io.usesOutputSlot(index);
                }
            });
        }
        // upgrade slots: speed, energy, and a special module slot below
        // Upgrade column: kept inside the machine area so the bottom slot never
        // crosses into the inventory label / player inventory below. A machine that
        // takes no speed (or energy) upgrades hides that slot entirely.
        addSlot(new SlotItemHandler(be.getUpgrades(), 0, 134, 17) {
            @Override
            public boolean isActive() {
                return be.machineType().maxSpeedUpgrades() > 0;
            }
        });
        addSlot(new SlotItemHandler(be.getUpgrades(), 1, 134, 37) {
            @Override
            public boolean isActive() {
                return be.machineType().maxEnergyUpgrades() > 0;
            }
        });
        addSlot(new SlotItemHandler(be.getUpgrades(), 2, 134, 57) {
            @Override
            public boolean isActive() {
                return io.module();
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
        } else if (ControllerBlockEntity.isSpeedUpgrade(stack)) {
            if (!moveItemStackTo(stack, UPG_START, UPG_START + 1, false)
                    && !moveItemStackTo(stack, IN_START, IN_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ControllerBlockEntity.isEnergyUpgrade(stack)) {
            if (!moveItemStackTo(stack, UPG_START + 1, UPG_START + 2, false)
                    && !moveItemStackTo(stack, IN_START, IN_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(MMMRegistry.POLONIUM_SYNTHESIS_UPGRADE.get())) {
            if (!moveItemStackTo(stack, UPG_START + 2, UPG_END, false)) {
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
