package com.chihaya.moremultiblock.blockentity;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.block.PortBlock;
import com.chihaya.moremultiblock.menu.PortMenu;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Relays capabilities to the controller of the multiblock this port belongs to.
 * The link is established by the controller whenever the structure validates.
 * All handlers are stable proxies that resolve the controller lazily, so they
 * behave correctly as the structure forms or breaks after being cached.
 */
public class PortBlockEntity extends BlockEntity implements MenuProvider {

    /** Auto-transfer runs every this many ticks. */
    private static final int AUTO_INTERVAL = 10;
    private static final int ITEM_RATE = 32;
    private static final long GAS_RATE = 2_000L;
    private static final int FLUID_RATE = 2_000;
    private static final int FE_RATE = 40_000;

    @Nullable
    private BlockPos controllerPos;
    /** When enabled, the port automatically pulls (input ports) or pushes (output ports) each interval. */
    private boolean auto;
    private int tickCounter;

    // Client-side mirrors of the synced ContainerData values.
    private int clientAuto;
    private int clientTank10;
    private int clientCap10;

    /**
     * Single slot for tank items on gas/fluid ports: a chemical tank / bucket placed
     * here is drained into (input ports) or filled from (output ports) the machine.
     */
    private final ItemStackHandler containerSlot = new ItemStackHandler(1) {
        @Override
        public void setSize(int size) {
            super.setSize(1);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (portType()) {
                case GAS_INPUT, GAS_OUTPUT ->
                        stack.getCapability(mekanism.common.capabilities.Capabilities.GAS_HANDLER).isPresent();
                case FLUID_INPUT, FLUID_OUTPUT ->
                        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (level != null && level.isClientSide) {
                return switch (index) {
                    case 0 -> clientAuto;
                    case 1 -> clientTank10;
                    case 2 -> clientCap10;
                    default -> 0;
                };
            }
            return switch (index) {
                case 0 -> auto ? 1 : 0;
                case 1 -> (int) (tankAmount() / 10L);
                case 2 -> (int) (tankCapacity() / 10L);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> {
                    clientAuto = value;
                    auto = value == 1;
                }
                case 1 -> clientTank10 = value;
                case 2 -> clientCap10 = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public PortBlockEntity(BlockPos pos, BlockState state) {
        super(MMMRegistry.PORT_BE.get(), pos, state);
    }

    public boolean isAuto() {
        return auto;
    }

    public void toggleAuto() {
        auto = !auto;
        setChanged();
    }

    public ItemStackHandler getContainerSlot() {
        return containerSlot;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public PortBlock.PortType portType() {
        return getBlockState().getBlock() instanceof PortBlock port ? port.type : PortBlock.PortType.ENERGY;
    }

    public void setController(BlockPos pos) {
        if (!pos.equals(controllerPos)) {
            controllerPos = pos.immutable();
            setChanged();
        }
    }

    @Nullable
    public PortHost resolveHost() {
        return host();
    }

    @Nullable
    private PortHost host() {
        if (level == null || controllerPos == null) {
            return null;
        }
        return level.getBlockEntity(controllerPos) instanceof PortHost host && host.isFormed() ? host : null;
    }

    /** Amount in the machine tank this port faces (input tank for input ports, output tank otherwise). */
    private long tankAmount() {
        PortHost host = host();
        if (host == null) {
            return 0;
        }
        return switch (portType()) {
            // machine gas tanks: 0 = input, 1 = input 2, 2 = output
            case GAS_INPUT -> host.getGasHandler() == null ? 0
                    : host.getGasHandler().getChemicalInTank(0).getAmount()
                    + host.getGasHandler().getChemicalInTank(1).getAmount();
            case GAS_OUTPUT -> host.getGasHandler() == null ? 0 : host.getGasHandler().getChemicalInTank(2).getAmount();
            case FLUID_INPUT -> host.getFluidHandler() == null ? 0 : host.getFluidHandler().getFluidInTank(0).getAmount();
            case FLUID_OUTPUT -> host.getFluidHandler() == null ? 0 : host.getFluidHandler().getFluidInTank(1).getAmount();
            default -> 0;
        };
    }

    private long tankCapacity() {
        PortHost host = host();
        if (host == null) {
            return 0;
        }
        return switch (portType()) {
            case GAS_INPUT, GAS_OUTPUT -> host.getGasHandler() == null ? 0 : host.getGasHandler().getTankCapacity(0);
            case FLUID_INPUT, FLUID_OUTPUT -> host.getFluidHandler() == null ? 0 : host.getFluidHandler().getTankCapacity(0);
            default -> 0;
        };
    }

    private final IItemHandler itemProxy = new IItemHandler() {
        @Override
        public int getSlots() {
            PortHost host = host();
            if (host == null) {
                return 0;
            }
            return (portType() == PortBlock.PortType.ITEM_INPUT ? host.getInputs() : host.getOutputs()).getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            PortHost controller = host();
            if (controller == null) {
                return ItemStack.EMPTY;
            }
            return portType() == PortBlock.PortType.ITEM_INPUT
                    ? controller.getInputs().getStackInSlot(slot)
                    : controller.getOutputs().getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            PortHost controller = host();
            if (controller == null || portType() != PortBlock.PortType.ITEM_INPUT) {
                return stack;
            }
            return controller.getInputs().insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            PortHost controller = host();
            if (controller == null || portType() != PortBlock.PortType.ITEM_OUTPUT) {
                return ItemStack.EMPTY;
            }
            return controller.getOutputs().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return portType() == PortBlock.PortType.ITEM_INPUT;
        }
    };

    private final IEnergyStorage feProxy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            PortHost controller = host();
            return controller == null ? 0 : controller.getFeHandler().receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            PortHost controller = host();
            return controller == null ? 0 : controller.getFeHandler().getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            PortHost controller = host();
            return controller == null ? 0 : controller.getFeHandler().getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private final IStrictEnergyHandler strictProxy = new IStrictEnergyHandler() {
        @Override
        public int getEnergyContainerCount() {
            return 1;
        }

        @Override
        public FloatingLong getEnergy(int container) {
            PortHost controller = host();
            return controller == null ? FloatingLong.ZERO : controller.getStrictEnergyHandler().getEnergy(container);
        }

        @Override
        public void setEnergy(int container, FloatingLong energy) {
            PortHost controller = host();
            if (controller != null) {
                controller.getStrictEnergyHandler().setEnergy(container, energy);
            }
        }

        @Override
        public FloatingLong getMaxEnergy(int container) {
            PortHost controller = host();
            return controller == null ? FloatingLong.ZERO : controller.getStrictEnergyHandler().getMaxEnergy(container);
        }

        @Override
        public FloatingLong getNeededEnergy(int container) {
            PortHost controller = host();
            return controller == null ? FloatingLong.ZERO : controller.getStrictEnergyHandler().getNeededEnergy(container);
        }

        @Override
        public FloatingLong insertEnergy(int container, FloatingLong amount, @NotNull Action action) {
            PortHost controller = host();
            return controller == null ? amount : controller.getStrictEnergyHandler().insertEnergy(container, amount, action);
        }

        @Override
        public FloatingLong extractEnergy(int container, FloatingLong amount, @NotNull Action action) {
            return FloatingLong.ZERO;
        }
    };

    /** Relays Mekanism gas: insert goes to the machine's input tank, extract pulls from its output tank. */
    private final IGasHandler gasProxy = new IGasHandler() {
        @Nullable
        private IGasHandler target() {
            PortHost host = host();
            return host == null ? null : host.getGasHandler();
        }

        @Override
        public int getTanks() {
            IGasHandler target = target();
            return target == null ? 0 : target.getTanks();
        }

        @Override
        public GasStack getChemicalInTank(int tank) {
            IGasHandler target = target();
            return target == null ? GasStack.EMPTY : target.getChemicalInTank(tank);
        }

        @Override
        public void setChemicalInTank(int tank, GasStack stack) {
            IGasHandler target = target();
            if (target != null) {
                target.setChemicalInTank(tank, stack);
            }
        }

        @Override
        public long getTankCapacity(int tank) {
            IGasHandler target = target();
            return target == null ? 0 : target.getTankCapacity(tank);
        }

        @Override
        public boolean isValid(int tank, GasStack stack) {
            IGasHandler target = target();
            return target != null && target.isValid(tank, stack);
        }

        @Override
        public GasStack insertChemical(int tank, GasStack stack, Action action) {
            if (portType() != PortBlock.PortType.GAS_INPUT) {
                return stack;
            }
            IGasHandler target = target();
            return target == null ? stack : target.insertChemical(tank, stack, action);
        }

        @Override
        public GasStack extractChemical(int tank, long amount, Action action) {
            if (portType() != PortBlock.PortType.GAS_OUTPUT) {
                return GasStack.EMPTY;
            }
            IGasHandler target = target();
            return target == null ? GasStack.EMPTY : target.extractChemical(tank, amount, action);
        }
    };

    /** Relays fluids: fill goes to the machine's input tank, drain pulls from its output tank. */
    private final IFluidHandler fluidProxy = new IFluidHandler() {
        @Nullable
        private IFluidHandler target() {
            PortHost host = host();
            return host == null ? null : host.getFluidHandler();
        }

        @Override
        public int getTanks() {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTanks();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            IFluidHandler target = target();
            return target != null && target.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (portType() != PortBlock.PortType.FLUID_INPUT) {
                return 0;
            }
            IFluidHandler target = target();
            return target == null ? 0 : target.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (portType() != PortBlock.PortType.FLUID_OUTPUT) {
                return FluidStack.EMPTY;
            }
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (portType() != PortBlock.PortType.FLUID_OUTPUT) {
                return FluidStack.EMPTY;
            }
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.drain(maxDrain, action);
        }
    };

    private final LazyOptional<IItemHandler> itemCapLO = LazyOptional.of(() -> itemProxy);
    private final LazyOptional<IEnergyStorage> energyCapLO = LazyOptional.of(() -> feProxy);
    private final LazyOptional<IStrictEnergyHandler> strictCapLO = LazyOptional.of(() -> strictProxy);
    private final LazyOptional<IGasHandler> gasCapLO = LazyOptional.of(() -> gasProxy);
    private final LazyOptional<IFluidHandler> fluidCapLO = LazyOptional.of(() -> fluidProxy);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove) {
            PortBlock.PortType type = portType();
            if (type == PortBlock.PortType.ENERGY) {
                if (cap == ForgeCapabilities.ENERGY) {
                    return energyCapLO.cast();
                }
                if (cap == mekanism.common.capabilities.Capabilities.STRICT_ENERGY) {
                    return strictCapLO.cast();
                }
            } else if (type == PortBlock.PortType.GAS_INPUT || type == PortBlock.PortType.GAS_OUTPUT) {
                if (cap == mekanism.common.capabilities.Capabilities.GAS_HANDLER) {
                    return gasCapLO.cast();
                }
            } else if (type == PortBlock.PortType.FLUID_INPUT || type == PortBlock.PortType.FLUID_OUTPUT) {
                if (cap == ForgeCapabilities.FLUID_HANDLER) {
                    return fluidCapLO.cast();
                }
            } else if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return itemCapLO.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapLO.invalidate();
        energyCapLO.invalidate();
        strictCapLO.invalidate();
        gasCapLO.invalidate();
        fluidCapLO.invalidate();
    }

    // --- auto transfer ---

    private int styleCounter;

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        PortHost containerHost = host();
        if (containerHost != null && !containerSlot.getStackInSlot(0).isEmpty()) {
            processContainer(containerHost);
        }
        // Adopt the skin of the machine this port is built into (default when unlinked).
        if (++styleCounter >= AUTO_INTERVAL) {
            styleCounter = 0;
            PortBlock.PortStyle desired = containerHost == null ? PortBlock.PortStyle.DEFAULT : containerHost.portStyle();
            if (getBlockState().getValue(PortBlock.STYLE) != desired) {
                level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PortBlock.STYLE, desired));
            }
        }
        if (!auto) {
            return;
        }
        if (++tickCounter < AUTO_INTERVAL) {
            return;
        }
        tickCounter = 0;
        PortHost host = containerHost;
        if (host == null) {
            return;
        }
        switch (portType()) {
            case ENERGY -> pullEnergy(host);
            case ITEM_INPUT -> pullItems(host);
            case ITEM_OUTPUT -> pushItems(host);
            case GAS_INPUT -> pullGas(host);
            case GAS_OUTPUT -> pushGas(host);
            case FLUID_INPUT -> pullFluid(host);
            case FLUID_OUTPUT -> pushFluid(host);
        }
    }

    /**
     * Moves gas/fluid between the tank item in the container slot and the machine:
     * input ports drain the item into the machine's input tank, output ports fill
     * the item from the machine's output tank.
     */
    private void processContainer(PortHost host) {
        ItemStack stack = containerSlot.getStackInSlot(0);
        switch (portType()) {
            case GAS_INPUT -> stack.getCapability(mekanism.common.capabilities.Capabilities.GAS_HANDLER).ifPresent(item -> {
                IGasHandler tank = host.getGasHandler();
                if (tank == null) {
                    return;
                }
                GasStack available = item.extractChemical(64_000L, Action.SIMULATE);
                if (available.isEmpty()) {
                    return;
                }
                GasStack leftover = tank.insertChemical(available, Action.SIMULATE);
                long movable = available.getAmount() - leftover.getAmount();
                if (movable > 0) {
                    tank.insertChemical(item.extractChemical(movable, Action.EXECUTE), Action.EXECUTE);
                    setChanged();
                }
            });
            case GAS_OUTPUT -> stack.getCapability(mekanism.common.capabilities.Capabilities.GAS_HANDLER).ifPresent(item -> {
                IGasHandler tank = host.getGasHandler();
                if (tank == null) {
                    return;
                }
                GasStack available = tank.extractChemical(64_000L, Action.SIMULATE);
                if (available.isEmpty()) {
                    return;
                }
                GasStack leftover = item.insertChemical(available, Action.SIMULATE);
                long movable = available.getAmount() - leftover.getAmount();
                if (movable > 0) {
                    item.insertChemical(tank.extractChemical(movable, Action.EXECUTE), Action.EXECUTE);
                    setChanged();
                }
            });
            case FLUID_INPUT -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(item -> {
                IFluidHandler tank = host.getFluidHandler();
                if (tank == null) {
                    return;
                }
                FluidStack available = item.drain(64_000, IFluidHandler.FluidAction.SIMULATE);
                if (available.isEmpty()) {
                    return;
                }
                int movable = tank.fill(available, IFluidHandler.FluidAction.SIMULATE);
                if (movable > 0) {
                    FluidStack drained = item.drain(new FluidStack(available, movable), IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        containerSlot.setStackInSlot(0, item.getContainer());
                        setChanged();
                    }
                }
            });
            case FLUID_OUTPUT -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(item -> {
                IFluidHandler tank = host.getFluidHandler();
                if (tank == null) {
                    return;
                }
                FluidStack available = tank.drain(64_000, IFluidHandler.FluidAction.SIMULATE);
                if (available.isEmpty()) {
                    return;
                }
                int movable = item.fill(available, IFluidHandler.FluidAction.SIMULATE);
                if (movable > 0) {
                    item.fill(tank.drain(new FluidStack(available, movable), IFluidHandler.FluidAction.EXECUTE),
                            IFluidHandler.FluidAction.EXECUTE);
                    containerSlot.setStackInSlot(0, item.getContainer());
                    setChanged();
                }
            });
            default -> {
            }
        }
    }

    /**
     * Visits every adjacent block entity exposing the capability on the touching
     * face, skipping other ports and controllers (which would loop transfers
     * straight back into the machine).
     */
    private <T> void forEachNeighbor(Capability<T> cap, java.util.function.Consumer<T> consumer) {
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null || neighbor instanceof PortBlockEntity || neighbor instanceof PortHost) {
                continue;
            }
            neighbor.getCapability(cap, dir.getOpposite()).ifPresent(consumer::accept);
        }
    }

    private void pullEnergy(PortHost host) {
        IEnergyStorage sink = host.getFeHandler();
        forEachNeighbor(ForgeCapabilities.ENERGY, source -> {
            int available = source.extractEnergy(FE_RATE, true);
            if (available <= 0) {
                return;
            }
            int accepted = sink.receiveEnergy(available, true);
            if (accepted > 0) {
                sink.receiveEnergy(source.extractEnergy(accepted, false), false);
            }
        });
    }

    private void pullItems(PortHost host) {
        forEachNeighbor(ForgeCapabilities.ITEM_HANDLER, source -> {
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack available = source.extractItem(slot, ITEM_RATE, true);
                if (available.isEmpty()) {
                    continue;
                }
                ItemStack leftover = insertIntoInputs(host, available, true);
                int moved = available.getCount() - leftover.getCount();
                if (moved > 0) {
                    insertIntoInputs(host, source.extractItem(slot, moved, false), false);
                }
            }
        });
    }

    private ItemStack insertIntoInputs(PortHost host, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int i = 0; i < host.getInputs().getSlots() && !remaining.isEmpty(); i++) {
            remaining = host.getInputs().insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    private void pushItems(PortHost host) {
        forEachNeighbor(ForgeCapabilities.ITEM_HANDLER, sink -> {
            for (int slot = 0; slot < host.getOutputs().getSlots(); slot++) {
                ItemStack available = host.getOutputs().extractItem(slot, ITEM_RATE, true);
                if (available.isEmpty()) {
                    continue;
                }
                ItemStack leftover = net.minecraftforge.items.ItemHandlerHelper.insertItem(sink, available, true);
                int moved = available.getCount() - leftover.getCount();
                if (moved > 0) {
                    net.minecraftforge.items.ItemHandlerHelper.insertItem(
                            sink, host.getOutputs().extractItem(slot, moved, false), false);
                }
            }
        });
    }

    private void pullGas(PortHost host) {
        IGasHandler sink = host.getGasHandler();
        if (sink == null) {
            return;
        }
        forEachNeighbor(mekanism.common.capabilities.Capabilities.GAS_HANDLER, source -> {
            GasStack available = source.extractChemical(GAS_RATE, Action.SIMULATE);
            if (available.isEmpty()) {
                return;
            }
            GasStack leftover = sink.insertChemical(available, Action.SIMULATE);
            long movable = available.getAmount() - leftover.getAmount();
            if (movable > 0) {
                sink.insertChemical(source.extractChemical(movable, Action.EXECUTE), Action.EXECUTE);
            }
        });
    }

    private void pushGas(PortHost host) {
        IGasHandler source = host.getGasHandler();
        if (source == null) {
            return;
        }
        forEachNeighbor(mekanism.common.capabilities.Capabilities.GAS_HANDLER, sink -> {
            GasStack available = source.extractChemical(GAS_RATE, Action.SIMULATE);
            if (available.isEmpty()) {
                return;
            }
            GasStack leftover = sink.insertChemical(available, Action.SIMULATE);
            long movable = available.getAmount() - leftover.getAmount();
            if (movable > 0) {
                sink.insertChemical(source.extractChemical(movable, Action.EXECUTE), Action.EXECUTE);
            }
        });
    }

    private void pullFluid(PortHost host) {
        IFluidHandler sink = host.getFluidHandler();
        if (sink == null) {
            return;
        }
        forEachNeighbor(ForgeCapabilities.FLUID_HANDLER, source -> {
            FluidStack available = source.drain(FLUID_RATE, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty()) {
                return;
            }
            int movable = sink.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (movable > 0) {
                FluidStack drained = source.drain(new FluidStack(available, movable), IFluidHandler.FluidAction.EXECUTE);
                sink.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            }
        });
    }

    private void pushFluid(PortHost host) {
        IFluidHandler source = host.getFluidHandler();
        if (source == null) {
            return;
        }
        forEachNeighbor(ForgeCapabilities.FLUID_HANDLER, sink -> {
            FluidStack available = source.drain(FLUID_RATE, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty()) {
                return;
            }
            int movable = sink.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (movable > 0) {
                FluidStack drained = source.drain(new FluidStack(available, movable), IFluidHandler.FluidAction.EXECUTE);
                sink.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            }
        });
    }

    // --- menu ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PortMenu(id, playerInventory, this);
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0D;
    }

    // --- nbt ---

    public void dropContents() {
        if (level == null) {
            return;
        }
        ItemStack stack = containerSlot.getStackInSlot(0);
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (controllerPos != null) {
            tag.put("Controller", NbtUtils.writeBlockPos(controllerPos));
        }
        tag.putBoolean("Auto", auto);
        tag.put("ContainerSlot", containerSlot.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        controllerPos = tag.contains("Controller") ? NbtUtils.readBlockPos(tag.getCompound("Controller")) : null;
        auto = tag.getBoolean("Auto");
        containerSlot.deserializeNBT(tag.getCompound("ContainerSlot"));
    }
}
