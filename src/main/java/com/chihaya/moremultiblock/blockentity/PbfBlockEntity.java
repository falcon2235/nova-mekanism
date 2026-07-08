package com.chihaya.moremultiblock.blockentity;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.block.PbfBlock;
import com.chihaya.moremultiblock.menu.PbfMenu;
import com.chihaya.moremultiblock.multiblock.MultiblockValidator;

import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GT-style primitive blast furnace: an all-brick vertical multiblock that turns iron
 * into steel using coal as fuel — no energy involved. One operation takes 30 seconds
 * and consumes 1 iron ingot + 2 coal for 1 steel ingot.
 */
public class PbfBlockEntity extends BlockEntity implements MenuProvider, PortHost {

    public static final int TICKS_REQUIRED = 600; // 30 seconds
    public static final int FUEL_PER_OPERATION = 2;
    private static final String LANG = "multiblock." + MekanismMoreMultiblock.MODID + ".";
    private static final int REVALIDATE_FORMED = 40;
    private static final int REVALIDATE_UNFORMED = 20;

    /** Slot 0: smeltable input (iron), slot 1: fuel (coal/charcoal). */
    private final ItemStackHandler inputs = new ItemStackHandler(2) {
        @Override
        public void setSize(int size) {
            super.setSize(2);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 1 ? stack.is(ItemTags.COALS) : !stack.is(ItemTags.COALS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler outputs = new ItemStackHandler(1) {
        @Override
        public void setSize(int size) {
            super.setSize(1);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int progress;
    private boolean formed;
    private int revalidateIn;
    private Component statusMessage = Component.translatable(LANG + "not_formed");

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> TICKS_REQUIRED;
                case 2 -> formed ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 2 -> formed = value == 1;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public PbfBlockEntity(BlockPos pos, BlockState state) {
        super(MMMRegistry.PBF_BE.get(), pos, state);
    }

    @Nullable
    public static Item steelIngot() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekanism", "ingot_steel"));
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    public Component getStatusMessage() {
        return statusMessage;
    }

    @Override
    public ItemStackHandler getInputs() {
        return inputs;
    }

    @Override
    public ItemStackHandler getOutputs() {
        return outputs;
    }

    public void revalidate() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(PbfBlock.FACING);
        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        Component error = MultiblockValidator.validatePbf(level, worldPosition, facing, Blocks.BRICKS, ports);
        formed = error == null;
        if (formed) {
            for (BlockPos portPos : ports) {
                if (level.getBlockEntity(portPos) instanceof PortBlockEntity port) {
                    port.setController(worldPosition);
                }
            }
        }
        statusMessage = formed ? Component.translatable(LANG + "formed_simple") : error;
        revalidateIn = formed ? REVALIDATE_FORMED : REVALIDATE_UNFORMED;
    }

    public void serverTick() {
        if (--revalidateIn <= 0) {
            revalidate();
        }
        if (!formed || !canOperate()) {
            progress = 0;
            return;
        }
        progress++;
        if (progress >= TICKS_REQUIRED) {
            completeOperation();
            progress = 0;
        }
        setChanged();
    }

    private boolean canOperate() {
        Item steel = steelIngot();
        if (steel == null) {
            return false;
        }
        if (!inputs.getStackInSlot(0).is(Items.IRON_INGOT)) {
            return false;
        }
        ItemStack fuel = inputs.getStackInSlot(1);
        if (!fuel.is(ItemTags.COALS) || fuel.getCount() < FUEL_PER_OPERATION) {
            return false;
        }
        ItemStack out = outputs.getStackInSlot(0);
        return out.isEmpty() || (out.is(steel) && out.getCount() < out.getMaxStackSize());
    }

    private void completeOperation() {
        Item steel = steelIngot();
        if (steel == null) {
            return;
        }
        inputs.getStackInSlot(0).shrink(1);
        inputs.getStackInSlot(1).shrink(FUEL_PER_OPERATION);
        ItemStack out = outputs.getStackInSlot(0);
        if (out.isEmpty()) {
            outputs.setStackInSlot(0, new ItemStack(steel, 1));
        } else {
            out.grow(1);
            outputs.setStackInSlot(0, out);
        }
    }

    // --- capabilities (items only; the PBF is unpowered) ---

    private final IItemHandler externalItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return slot < 2 ? inputs.getStackInSlot(slot) : outputs.getStackInSlot(slot - 2);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot < 2 ? inputs.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < 2 ? ItemStack.EMPTY : outputs.extractItem(slot - 2, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < 2 && inputs.isItemValid(slot, stack);
        }
    };

    /** The PBF is unpowered; ports still need a handler, so expose a zero-capacity one. */
    private final IEnergyStorage noEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private final IStrictEnergyHandler noStrictEnergy = new IStrictEnergyHandler() {
        @Override
        public int getEnergyContainerCount() {
            return 0;
        }

        @Override
        public FloatingLong getEnergy(int container) {
            return FloatingLong.ZERO;
        }

        @Override
        public void setEnergy(int container, FloatingLong energy) {
        }

        @Override
        public FloatingLong getMaxEnergy(int container) {
            return FloatingLong.ZERO;
        }

        @Override
        public FloatingLong getNeededEnergy(int container) {
            return FloatingLong.ZERO;
        }

        @Override
        public FloatingLong insertEnergy(int container, FloatingLong amount, @NotNull Action action) {
            return amount;
        }

        @Override
        public FloatingLong extractEnergy(int container, FloatingLong amount, @NotNull Action action) {
            return FloatingLong.ZERO;
        }
    };

    @Override
    public com.chihaya.moremultiblock.block.PortBlock.PortStyle portStyle() {
        return com.chihaya.moremultiblock.block.PortBlock.PortStyle.BRICK;
    }

    @Override
    public IEnergyStorage getFeHandler() {
        return noEnergy;
    }

    @Override
    public IStrictEnergyHandler getStrictEnergyHandler() {
        return noStrictEnergy;
    }

    private final LazyOptional<IItemHandler> itemCapLO = LazyOptional.of(() -> externalItems);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove && cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapLO.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapLO.invalidate();
    }

    // --- menu ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PbfMenu(id, playerInventory, this);
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0D;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (ItemStackHandler handler : new ItemStackHandler[]{inputs, outputs}) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    // --- display getters (client, synced via dataAccess) ---

    public int displayProgress() {
        return progress;
    }

    public boolean displayFormed() {
        return formed;
    }

    // --- nbt ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inputs", inputs.serializeNBT());
        tag.put("Outputs", outputs.serializeNBT());
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inputs.deserializeNBT(tag.getCompound("Inputs"));
        outputs.deserializeNBT(tag.getCompound("Outputs"));
        progress = tag.getInt("Progress");
    }
}
