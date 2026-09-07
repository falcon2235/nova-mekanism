package com.falcon2235.moremultiblock.blockentity;

import com.falcon2235.moremultiblock.MMMRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one research-data module for the multiblock it is built into. A machine
 * looks here as well as in its own module slot, so a line can be re-tooled by
 * piping a different research data into the hatch instead of opening the
 * controller — and the module slot stays free for presses and patterns.
 */
public class ResearchHatchBlockEntity extends BlockEntity {

    /** How many different research data modules one hatch can serve at once. */
    public static final int SLOTS = 6;

    private final ItemStackHandler item = new ItemStackHandler(SLOTS) {
        @Override
        public void setSize(int size) {
            super.setSize(SLOTS);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // one of each kind; the point is variety, not stacking
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return ChemMachineBlockEntity.isResearchData(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                // Push the change to clients so the hatch's lit state matches.
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state,
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
    };

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> item);

    public ResearchHatchBlockEntity(BlockPos pos, BlockState state) {
        super(MMMRegistry.RESEARCH_HATCH_BE.get(), pos, state);
    }

    public ItemStackHandler getItem() {
        return item;
    }

    /** Whether this hatch holds the given research data in any of its slots. */
    public boolean holds(ItemStack required) {
        for (int i = 0; i < SLOTS; i++) {
            if (ItemStack.isSameItemSameTags(item.getStackInSlot(i), required)) {
                return true;
            }
        }
        return false;
    }

    /** Whether any slot is occupied (drives the block's lit state). */
    public boolean hasAny() {
        for (int i = 0; i < SLOTS; i++) {
            if (!item.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Everything currently installed, for tooltips and messages. */
    public java.util.List<ItemStack> installedAll() {
        java.util.List<ItemStack> list = new java.util.ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = item.getStackInSlot(i);
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    /** Whether any of the given structure positions holds this exact research data. */
    public static boolean holdsAmong(net.minecraft.world.level.Level level,
                                     java.util.List<BlockPos> positions, ItemStack required) {
        if (level == null || required.isEmpty()) {
            return false;
        }
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof ResearchHatchBlockEntity hatch && hatch.holds(required)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                      @Nullable net.minecraft.core.Direction side) {
        if (!remove && cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Item", item.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        item.deserializeNBT(tag.getCompound("Item"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", item.serializeNBT());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains("Item")) {
            item.deserializeNBT(tag.getCompound("Item"));
        }
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }
}
