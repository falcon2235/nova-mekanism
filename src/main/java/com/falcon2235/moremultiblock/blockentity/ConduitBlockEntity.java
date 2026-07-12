package com.falcon2235.moremultiblock.blockentity;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.block.ConduitBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The quantum conduit's network brain. Conduits of one type form a network (BFS,
 * capped at {@value #MAX_NETWORK}); the member with the lowest position acts as the
 * master. Distribution is instant and unbuffered:
 * <ul>
 * <li><b>Push-routing</b> — anything inserted into any segment's capability is routed
 *     straight to the network's consumers (our ports auto-push, generators push,
 *     Mekanism machines/cables push into it).</li>
 * <li><b>Extraction</b> — faces toggled to extract mode actively pull from the
 *     adjacent handler every tick (up to the per-type rate) and route it likewise.
 *     Extract faces are never used as insertion targets, so nothing ping-pongs.</li>
 * </ul>
 */
public class ConduitBlockEntity extends BlockEntity {

    private static final int MAX_NETWORK = 512;
    private static final int CACHE_TICKS = 10;

    private final boolean[] extract = new boolean[6];
    /** Cached network membership (master only holds the full list). */
    @Nullable
    private List<BlockPos> network;
    @Nullable
    private BlockPos masterPos;
    private int cacheCooldown;

    public ConduitBlockEntity(BlockPos pos, BlockState state) {
        super(MMMRegistry.CONDUIT_BE.get(), pos, state);
    }

    public ConduitBlock.Type conduitType() {
        return getBlockState().getBlock() instanceof ConduitBlock conduit ? conduit.type : ConduitBlock.Type.ENERGY;
    }

    /** Toggles extract mode on a face; returns the new state. */
    public boolean toggleExtract(Direction face) {
        extract[face.ordinal()] = !extract[face.ordinal()];
        setChanged();
        return extract[face.ordinal()];
    }

    // --- network upkeep ---

    public void serverTick() {
        if (level == null) {
            return;
        }
        if (--cacheCooldown <= 0 || masterPos == null || !isConduit(masterPos)) {
            rebuildNetwork();
        }
        if (worldPosition.equals(masterPos)) {
            runExtractions();
        }
    }

    private boolean isConduit(BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof ConduitBlockEntity be
                && be.conduitType() == conduitType();
    }

    /** BFS over same-type conduits; the smallest position becomes the master. */
    private void rebuildNetwork() {
        cacheCooldown = CACHE_TICKS;
        List<BlockPos> members = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        BlockPos best = worldPosition;
        while (!queue.isEmpty() && members.size() < MAX_NETWORK) {
            BlockPos current = queue.poll();
            members.add(current);
            if (current.asLong() < best.asLong()) {
                best = current;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (seen.add(next) && isConduit(next)) {
                    queue.add(next);
                }
            }
        }
        masterPos = best;
        network = worldPosition.equals(best) ? members : null;
    }

    /** The master conduit instance for this network (self if alone), or null mid-load. */
    @Nullable
    private ConduitBlockEntity master() {
        if (worldPosition.equals(masterPos)) {
            return this;
        }
        if (masterPos != null && level != null
                && level.getBlockEntity(masterPos) instanceof ConduitBlockEntity be
                && be.conduitType() == conduitType()) {
            return be;
        }
        rebuildNetwork();
        return worldPosition.equals(masterPos) ? this : null;
    }

    private List<BlockPos> members() {
        if (network == null) {
            rebuildNetwork();
        }
        return network != null ? network : List.of(worldPosition);
    }

    // --- extraction (master only) ---

    private void runExtractions() {
        for (BlockPos pos : members()) {
            if (!(level.getBlockEntity(pos) instanceof ConduitBlockEntity conduit)
                    || conduit.conduitType() != conduitType()) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (!conduit.extract[dir.ordinal()]) {
                    continue;
                }
                BlockEntity source = level.getBlockEntity(pos.relative(dir));
                if (source == null || source instanceof ConduitBlockEntity) {
                    continue;
                }
                switch (conduitType()) {
                    case ENERGY -> pullEnergy(source, pos, dir);
                    case FLUID -> pullFluid(source, pos, dir);
                    case GAS -> pullGas(source, pos, dir);
                    case ITEM -> pullItems(source, pos, dir);
                }
            }
        }
    }

    private void pullEnergy(BlockEntity source, BlockPos conduitPos, Direction dir) {
        IEnergyStorage handler = source.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                .resolve().orElse(null);
        if (handler == null || !handler.canExtract()) {
            return;
        }
        int available = handler.extractEnergy(com.falcon2235.moremultiblock.MMMConfig.conduitEnergyRfPerTick(), true);
        if (available <= 0) {
            return;
        }
        int accepted = distributeEnergy(available, conduitPos, dir, false);
        if (accepted > 0) {
            handler.extractEnergy(accepted, false);
        }
    }

    private void pullFluid(BlockEntity source, BlockPos conduitPos, Direction dir) {
        IFluidHandler handler = source.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite())
                .resolve().orElse(null);
        if (handler == null) {
            return;
        }
        FluidStack available = handler.drain(
                com.falcon2235.moremultiblock.MMMConfig.conduitFluidMbPerTick(), IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return;
        }
        int accepted = distributeFluid(available, conduitPos, dir, false);
        if (accepted > 0) {
            handler.drain(new FluidStack(available, accepted), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void pullGas(BlockEntity source, BlockPos conduitPos, Direction dir) {
        IGasHandler handler = source.getCapability(mekanism.common.capabilities.Capabilities.GAS_HANDLER,
                dir.getOpposite()).resolve().orElse(null);
        if (handler == null) {
            return;
        }
        GasStack available = handler.extractChemical(
                com.falcon2235.moremultiblock.MMMConfig.conduitGasMbPerTick(), Action.SIMULATE);
        if (available.isEmpty()) {
            return;
        }
        long accepted = distributeGas(available, conduitPos, dir, false);
        if (accepted > 0) {
            handler.extractChemical(new GasStack(available, accepted), Action.EXECUTE);
        }
    }

    private void pullItems(BlockEntity source, BlockPos conduitPos, Direction dir) {
        IItemHandler handler = source.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite())
                .resolve().orElse(null);
        if (handler == null) {
            return;
        }
        int pullLimit = com.falcon2235.moremultiblock.MMMConfig.conduitItemsPerPull();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack available = handler.extractItem(slot, pullLimit, true);
            if (available.isEmpty()) {
                continue;
            }
            ItemStack leftover = distributeItems(available, conduitPos, dir, true);
            int movable = available.getCount() - leftover.getCount();
            if (movable <= 0) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, movable, false);
            ItemStack undelivered = distributeItems(taken, conduitPos, dir, false);
            if (!undelivered.isEmpty()) {
                // Race between simulate and execute — push the remainder back.
                ItemHandlerHelper.insertItem(handler, undelivered, false);
            }
            return; // one stack per side per tick
        }
    }

    // --- distribution (runs on the master; excludes the inserting endpoint) ---

    private interface SinkVisitor {
        /** Visits one external neighbour handler; returns true when distribution is finished. */
        boolean visit(BlockEntity sink, Direction sinkSide);
    }

    private void visitSinks(@Nullable BlockPos exclPos, @Nullable Direction exclDir, SinkVisitor visitor) {
        for (BlockPos pos : members()) {
            if (!(level.getBlockEntity(pos) instanceof ConduitBlockEntity conduit)
                    || conduit.conduitType() != conduitType()) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (conduit.extract[dir.ordinal()]) {
                    continue; // never push back into an extraction source
                }
                if (pos.equals(exclPos) && dir == exclDir) {
                    continue; // never bounce back to the inserter
                }
                BlockEntity sink = level.getBlockEntity(pos.relative(dir));
                if (sink == null || sink instanceof ConduitBlockEntity) {
                    continue;
                }
                if (visitor.visit(sink, dir.getOpposite())) {
                    return;
                }
            }
        }
    }

    int distributeEnergy(int amount, @Nullable BlockPos exclPos, @Nullable Direction exclDir, boolean simulate) {
        int[] remaining = {amount};
        visitSinks(exclPos, exclDir, (sink, side) -> {
            IEnergyStorage handler = sink.getCapability(ForgeCapabilities.ENERGY, side).resolve().orElse(null);
            if (handler != null && handler.canReceive()) {
                remaining[0] -= handler.receiveEnergy(remaining[0], simulate);
            }
            return remaining[0] <= 0;
        });
        return amount - remaining[0];
    }

    int distributeFluid(FluidStack stack, @Nullable BlockPos exclPos, @Nullable Direction exclDir, boolean simulate) {
        int[] remaining = {stack.getAmount()};
        IFluidHandler.FluidAction action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        visitSinks(exclPos, exclDir, (sink, side) -> {
            IFluidHandler handler = sink.getCapability(ForgeCapabilities.FLUID_HANDLER, side).resolve().orElse(null);
            if (handler != null) {
                remaining[0] -= handler.fill(new FluidStack(stack, remaining[0]), action);
            }
            return remaining[0] <= 0;
        });
        return stack.getAmount() - remaining[0];
    }

    long distributeGas(GasStack stack, @Nullable BlockPos exclPos, @Nullable Direction exclDir, boolean simulate) {
        long[] remaining = {stack.getAmount()};
        Action action = simulate ? Action.SIMULATE : Action.EXECUTE;
        visitSinks(exclPos, exclDir, (sink, side) -> {
            IGasHandler handler = sink.getCapability(mekanism.common.capabilities.Capabilities.GAS_HANDLER, side)
                    .resolve().orElse(null);
            if (handler != null) {
                GasStack rejected = handler.insertChemical(new GasStack(stack, remaining[0]), action);
                remaining[0] = rejected.getAmount();
            }
            return remaining[0] <= 0;
        });
        return stack.getAmount() - remaining[0];
    }

    ItemStack distributeItems(ItemStack stack, @Nullable BlockPos exclPos, @Nullable Direction exclDir, boolean simulate) {
        ItemStack[] remaining = {stack.copy()};
        visitSinks(exclPos, exclDir, (sink, side) -> {
            IItemHandler handler = sink.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
            if (handler != null) {
                remaining[0] = ItemHandlerHelper.insertItem(handler, remaining[0], simulate);
            }
            return remaining[0].isEmpty();
        });
        return remaining[0];
    }

    // --- capability proxies (one per side, routing through the master) ---

    private ConduitBlockEntity routingMaster() {
        ConduitBlockEntity m = master();
        return m != null ? m : this;
    }

    private final class EnergyProxy implements IEnergyStorage {
        private final Direction side;

        private EnergyProxy(Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return routingMaster().distributeEnergy(maxReceive, worldPosition, side, simulate);
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
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    private final class FluidProxy implements IFluidHandler {
        private final Direction side;

        private FluidProxy(Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return routingMaster().distributeFluid(resource, worldPosition, side, action.simulate());
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class GasProxy implements IGasHandler {
        private final Direction side;

        private GasProxy(Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public GasStack getChemicalInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public void setChemicalInTank(int tank, GasStack stack) {
        }

        @Override
        public long getTankCapacity(int tank) {
            return Long.MAX_VALUE;
        }

        @Override
        public boolean isValid(int tank, GasStack stack) {
            return true;
        }

        @Override
        public GasStack insertChemical(int tank, GasStack stack, Action action) {
            long accepted = routingMaster().distributeGas(stack, worldPosition, side, action.simulate());
            long rejected = stack.getAmount() - accepted;
            return rejected <= 0 ? GasStack.EMPTY : new GasStack(stack, rejected);
        }

        @Override
        public GasStack extractChemical(int tank, long amount, Action action) {
            return GasStack.EMPTY;
        }
    }

    private final class ItemProxy implements IItemHandler {
        private final Direction side;

        private ItemProxy(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return routingMaster().distributeItems(stack, worldPosition, side, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private final LazyOptional<?>[] sideCaps = new LazyOptional<?>[6];

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove && side != null && matchesType(cap)) {
            int i = side.ordinal();
            if (sideCaps[i] == null || !sideCaps[i].isPresent()) {
                sideCaps[i] = LazyOptional.of(() -> switch (conduitType()) {
                    case ENERGY -> new EnergyProxy(side);
                    case FLUID -> new FluidProxy(side);
                    case GAS -> new GasProxy(side);
                    case ITEM -> new ItemProxy(side);
                });
            }
            return sideCaps[i].cast();
        }
        return super.getCapability(cap, side);
    }

    private boolean matchesType(Capability<?> cap) {
        return switch (conduitType()) {
            case ENERGY -> cap == ForgeCapabilities.ENERGY;
            case FLUID -> cap == ForgeCapabilities.FLUID_HANDLER;
            case GAS -> cap == mekanism.common.capabilities.Capabilities.GAS_HANDLER;
            case ITEM -> cap == ForgeCapabilities.ITEM_HANDLER;
        };
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<?> lo : sideCaps) {
            if (lo != null) {
                lo.invalidate();
            }
        }
    }

    // --- nbt ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        byte bits = 0;
        for (int i = 0; i < 6; i++) {
            if (extract[i]) {
                bits |= (byte) (1 << i);
            }
        }
        tag.putByte("Extract", bits);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        byte bits = tag.getByte("Extract");
        for (int i = 0; i < 6; i++) {
            extract[i] = (bits & (1 << i)) != 0;
        }
    }
}
