package com.falcon2235.moremultiblock.blockentity;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.block.ChemMachineBlock;
import com.falcon2235.moremultiblock.machine.ChemMachineType;
import com.falcon2235.moremultiblock.machine.ChemRecipe;
import com.falcon2235.moremultiblock.machine.ChemRecipes;
import com.falcon2235.moremultiblock.menu.ChemMachineMenu;
import com.falcon2235.moremultiblock.multiblock.MultiblockValidator;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared block entity for the three chemical multiblocks. Exposes item, energy,
 * Mekanism gas and Forge fluid capabilities on the controller so machinery can be
 * piped directly to it, and processes the hardcoded titanium-chain recipes.
 */
public class ChemMachineBlockEntity extends BlockEntity implements MenuProvider, PortHost {

    public static final int INPUT_SLOTS = 5;
    public static final int OUTPUT_SLOTS = 4;
    private static final long GAS_CAP = 64_000L;
    private static final int FLUID_CAP = 64_000;
    // Huge buffer: the black hole stabilizer draws 2.5e9 J/t (1,000,000,000 RF/t).
    private static final long CAPACITY = 100_000_000_000L;
    private static final String LANG = "multiblock." + MekanismMoreMultiblock.MODID + ".";
    private static final int REVALIDATE_FORMED = 40;
    private static final int REVALIDATE_UNFORMED = 20;

    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        public void setSize(int size) {
            // Keep the fixed slot count even when older saves carry a smaller "Size"
            // tag — otherwise loading pre-update machines shrinks the handler and
            // recipe checks crash with an out-of-range slot access.
            super.setSize(INPUT_SLOTS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler outputs = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        public void setSize(int size) {
            super.setSize(OUTPUT_SLOTS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public static final int UPGRADE_SLOTS = 3;

    /** Slot 0: speed upgrades (max 8), slot 1: energy upgrades (max 8), slot 2: a special module (max 1). */
    private final ItemStackHandler upgrades = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        public void setSize(int size) {
            super.setSize(UPGRADE_SLOTS);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 2 ? 1 : 8;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> ControllerBlockEntity.isSpeedUpgrade(stack);
                case 1 -> ControllerBlockEntity.isEnergyUpgrade(stack);
                default -> stack.is(MMMRegistry.POLONIUM_SYNTHESIS_UPGRADE.get());
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            upgradesDirty = true;
            setChanged();
        }
    };

    private GasStack gasIn = GasStack.EMPTY;
    /** Second gas input tank for two-gas reactions (e.g. NaOH + CO2). */
    private GasStack gasIn2 = GasStack.EMPTY;
    private GasStack gasOut = GasStack.EMPTY;
    private final FluidTank fluidIn = new FluidTank(FLUID_CAP);
    private final FluidTank fluidOut = new FluidTank(FLUID_CAP);

    private long energy;
    private int progress;
    private int ticksRequired = 1;
    private boolean formed;
    /** Heating-coil tier of the formed structure (blast furnace only, 0 otherwise). */
    private int coilTier;
    private boolean upgradesDirty = true;
    /** Mekanism upgrade multipliers, recomputed only when the upgrade slots change. */
    private double upgradeTimeFactor = 1.0;
    private double upgradeEnergyFactor = 1.0;
    /** Whether the EBF coil rings are currently glowing. */
    private boolean coilsLit;
    /**
     * Glow keep-alive: reset while processing, counts down when stalled. Keeps the
     * coils lit across the 1-tick gap between operations (and brief input gaps)
     * instead of flickering off and on every cycle.
     */
    private int coilGlowGrace;
    private static final int COIL_GLOW_GRACE_TICKS = 15;
    /** Whether the machine is actively running — synced to clients to drive the star / black-hole render. */
    private boolean active;
    private int revalidateIn;
    private Component statusMessage = Component.translatable(LANG + "not_formed");
    /** Structure ports found by the last successful validation (transient, rebuilt on revalidate). */
    private java.util.List<BlockPos> cachedPorts = java.util.List.of();

    private int clientEnergy10k;
    // Tank amounts synced in units of 10 mB so they fit ContainerData's short range.
    private int clientGasIn10;
    private int clientGasIn2x10;
    private int clientGasOut10;
    private int clientFluidIn10;
    private int clientFluidOut10;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (level != null && level.isClientSide) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> Math.max(1, ticksRequired);
                    case 2 -> clientEnergy10k;
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY / 10_000L);
                    case 4 -> formed ? 1 : 0;
                    case 5 -> clientGasIn10;
                    case 6 -> clientGasOut10;
                    case 7 -> clientFluidIn10;
                    case 8 -> clientFluidOut10;
                    case 9 -> clientGasIn2x10;
                    default -> 0;
                };
            }
            return switch (index) {
                case 0 -> progress;
                case 1 -> Math.max(1, ticksRequired);
                case 2 -> clientEnergy10k;
                case 3 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY / 10_000L);
                case 4 -> formed ? 1 : 0;
                case 5 -> (int) (gasIn.getAmount() / 10L);
                case 6 -> (int) (gasOut.getAmount() / 10L);
                case 7 -> fluidIn.getFluidAmount() / 10;
                case 8 -> fluidOut.getFluidAmount() / 10;
                case 9 -> (int) (gasIn2.getAmount() / 10L);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> ticksRequired = value;
                case 2 -> clientEnergy10k = value;
                case 4 -> formed = value == 1;
                case 5 -> clientGasIn10 = value;
                case 6 -> clientGasOut10 = value;
                case 7 -> clientFluidIn10 = value;
                case 8 -> clientFluidOut10 = value;
                case 9 -> clientGasIn2x10 = value;
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public ChemMachineBlockEntity(BlockPos pos, BlockState state) {
        super(MMMRegistry.CHEM_MACHINE_BE.get(), pos, state);
    }

    public ChemMachineType machineType() {
        return getBlockState().getBlock() instanceof ChemMachineBlock block ? block.machineType : ChemMachineType.BLAST_FURNACE;
    }

    public boolean isFormed() {
        return formed;
    }

    public Component getStatusMessage() {
        return statusMessage;
    }

    public ItemStackHandler getInputs() {
        return inputs;
    }

    public ItemStackHandler getOutputs() {
        return outputs;
    }

    public ItemStackHandler getUpgrades() {
        return upgrades;
    }

    public IEnergyStorage getFeHandler() {
        return feHandler;
    }

    public IStrictEnergyHandler getStrictEnergyHandler() {
        return strictEnergy;
    }

    @Override
    public IGasHandler getGasHandler() {
        return gasHandler;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    @Override
    public com.falcon2235.moremultiblock.block.PortBlock.PortStyle portStyle() {
        return switch (machineType()) {
            case BLAST_FURNACE -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.HEAT_PROOF;
            case REACTOR -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.PTFE;
            case DISTILLATION, MIXER -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.STAINLESS;
            case ALLOY_BLAST_FURNACE -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.ALLOY;
            case VACUUM_FREEZER -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.FROST;
            case CIRCUIT_ASSEMBLER -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.ASSEMBLY;
            case ELECTROLYZER -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.ELECTROLYZER;
            case CENTRIFUGE -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.CENTRIFUGE;
            case FUSION_REACTOR, STAR_GENERATOR, STABILIZER, HADRON_COLLIDER, ANNIHILATION_GENERATOR -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.FUSION;
            case VOID_MINER -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.ALLOY;
            case OIL_RIG -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.STAINLESS;
            case COMBUSTION_GENERATOR -> com.falcon2235.moremultiblock.block.PortBlock.PortStyle.HEAT_PROOF;
        };
    }

    // --- structure ---

    public void revalidate() {
        if (level == null || level.isClientSide) {
            return;
        }
        ChemMachineType type = machineType();
        Direction facing = getBlockState().getValue(ChemMachineBlock.FACING);
        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        int[] coilTierOut = {0};
        Component error;
        if (type == ChemMachineType.ALLOY_BLAST_FURNACE) {
            error = MultiblockValidator.validateAbs(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.HEAT_VENT.get(), ports, coilTierOut);
        } else if (type == ChemMachineType.CIRCUIT_ASSEMBLER) {
            error = MultiblockValidator.validateAssemblyLine(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.ASSEMBLY_GLASS.get(),
                    type.width, type.height, type.depth, ports);
        } else if (type == ChemMachineType.FUSION_REACTOR) {
            error = MultiblockValidator.validateFusion(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.FUSION_COIL.get(),
                    MMMRegistry.FUSION_GLASS.get(), ports);
        } else if (type == ChemMachineType.STAR_GENERATOR || type == ChemMachineType.ANNIHILATION_GENERATOR) {
            error = MultiblockValidator.validateStar(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.FUSION_GLASS.get(), type.width, ports);
        } else if (type == ChemMachineType.STABILIZER) {
            error = MultiblockValidator.validateStabilizer(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.STABILIZER_GLASS.get(), ports);
        } else if (type == ChemMachineType.HADRON_COLLIDER) {
            error = MultiblockValidator.validateCollider(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.COLLIDER_MAGNET.get(),
                    MMMRegistry.FUSION_GLASS.get(), ports);
        } else if (type == ChemMachineType.VOID_MINER) {
            error = MultiblockValidator.validateVoidMiner(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.VOID_DRILL.get(), ports);
        } else if (type == ChemMachineType.OIL_RIG) {
            error = MultiblockValidator.validateOilRig(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), MMMRegistry.DRILL_PIPE.get(), ports);
        } else if (type.coilTower) {
            error = MultiblockValidator.validateEbf(level, worldPosition, facing,
                    MMMRegistry.chemCasing(type), type.height, ports, coilTierOut);
        } else {
            error = MultiblockValidator.validateBox(level, worldPosition, facing,
                    type.width, type.height, type.depth,
                    MMMRegistry.chemCasing(type), MMMRegistry.chemCoil(type), ports);
        }
        formed = error == null;
        coilTier = formed ? coilTierOut[0] : 0;
        cachedPorts = formed ? java.util.List.copyOf(ports) : java.util.List.of();
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
        if (!formed) {
            progress = 0;
            applyCoilGlow(false, true);
            updateDisplay();
            return;
        }

        if (upgradesDirty) {
            recomputeUpgrades();
            upgradesDirty = false;
        }
        if (machineType() == ChemMachineType.VOID_MINER) {
            voidMinerTick();
            return;
        }
        if (machineType() == ChemMachineType.COMBUSTION_GENERATOR) {
            combustionTick();
            return;
        }
        if (machineType() == ChemMachineType.ANNIHILATION_GENERATOR) {
            annihilationTick();
            return;
        }
        ChemRecipe recipe = findRecipe();
        if (recipe == null) {
            progress = 0;
            applyCoilGlow(false, false);
            updateDisplay();
            return;
        }
        int effective = effectiveTicks(recipe);
        long cost = effectiveEnergyPerTick(recipe);
        ticksRequired = effective;
        boolean processed = false;
        if (energy >= cost) {
            energy -= cost;
            progress++;
            processed = true;
            if (progress >= effective) {
                complete(recipe);
                progress = 0;
            }
            setChanged();
        }
        applyCoilGlow(processed, false);
        updateDisplay();
    }

    /**
     * The void ore miner's mining loop: every tick it burns a flat energy cost and rolls
     * ONE rarity-weighted ore type, producing 10 of it (see {@link com.falcon2235.moremultiblock.machine.VoidOreTable}).
     * Stalls (without burning energy) when the output buffer cannot take the rolled stack.
     * Energy upgrades reduce the per-tick cost like any other machine.
     */
    private void voidMinerTick() {
        ticksRequired = 1;
        long cost = Math.max(1L, (long) Math.ceil(
                com.falcon2235.moremultiblock.machine.VoidOreTable.ENERGY_PER_TICK * upgradeEnergyFactor));
        boolean processed = false;
        if (energy >= cost && level != null) {
            ItemStack mined = com.falcon2235.moremultiblock.machine.VoidOreTable.roll(level.random);
            if (!mined.isEmpty() && fitsItemStack(mined)) {
                energy -= cost;
                insertItemOutput(mined);
                processed = true;
                setChanged();
            }
        }
        progress = processed ? 1 : 0;
        applyCoilGlow(processed, false);
        updateDisplay();
    }

    /** Combustion generator output: 1,250,000 J/t = 500,000 RF/t while burning. */
    public static final long COMBUSTION_J_PER_TICK = 1_250_000L;
    /** Diesel burned per tick (mB). */
    public static final int COMBUSTION_DIESEL_MB_PER_TICK = 20;

    /**
     * The large combustion generator's loop: burns diesel from the fluid input tank
     * into the energy buffer (GT Large Combustion Engine style), then pushes the
     * buffered energy out through the structure's energy ports every tick.
     */
    private void combustionTick() {
        ticksRequired = 1;
        boolean processed = false;
        FluidStack diesel = ChemRecipes.diesel(COMBUSTION_DIESEL_MB_PER_TICK);
        if (!diesel.isEmpty()
                && fluidIn.getFluidAmount() >= COMBUSTION_DIESEL_MB_PER_TICK
                && fluidIn.getFluid().isFluidEqual(diesel)
                && energy + COMBUSTION_J_PER_TICK <= CAPACITY) {
            fluidIn.drain(COMBUSTION_DIESEL_MB_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            energy += COMBUSTION_J_PER_TICK;
            processed = true;
            setChanged();
        }
        pushGeneratedEnergy();
        progress = processed ? 1 : 0;
        applyCoilGlow(processed, false);
        updateDisplay();
    }

    /**
     * Actively distributes buffered energy to FE receivers adjacent to the controller
     * and to each structure port (so cables plugged into any energy port are fed even
     * if they never pull). Our energy is Joules; neighbours receive FE (= J * 2/5).
     */
    private void pushGeneratedEnergy() {
        if (energy <= 0 || level == null) {
            return;
        }
        java.util.List<BlockPos> origins = new java.util.ArrayList<>(cachedPorts.size() + 1);
        origins.add(worldPosition);
        origins.addAll(cachedPorts);
        for (BlockPos origin : origins) {
            for (Direction dir : Direction.values()) {
                int offerFe = (int) Math.min(Integer.MAX_VALUE, energy * 2 / 5);
                if (offerFe <= 0) {
                    return;
                }
                BlockEntity neighbor = level.getBlockEntity(origin.relative(dir));
                if (neighbor == null || neighbor instanceof PortBlockEntity || neighbor instanceof ChemMachineBlockEntity) {
                    continue; // skip our own structure
                }
                IEnergyStorage handler = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                        .resolve().orElse(null);
                if (handler == null || !handler.canReceive()) {
                    continue;
                }
                int accepted = handler.receiveEnergy(offerFe, false);
                if (accepted > 0) {
                    energy -= accepted * 5L / 2;
                    setChanged();
                }
            }
        }
    }

    /** Annihilation generator output: 2,000,000,000 J/t = 800,000,000 RF/t while running. */
    public static final long ANNIHILATION_J_PER_TICK = 2_000_000_000L;
    /** Hydrogen annihilated per tick (mB). */
    public static final int ANNIHILATION_HYDROGEN_MB_PER_TICK = 50;
    /** Antimatter annihilated per tick (mB). */
    public static final int ANNIHILATION_ANTIMATTER_MB_PER_TICK = 1;
    /** Liquid-helium coolant consumed per tick (mB). */
    public static final int ANNIHILATION_HELIUM_MB_PER_TICK = 10;

    /**
     * The annihilation generator's loop: collides hydrogen against antimatter under a
     * liquid-helium coolant bath, converting the annihilated mass straight into the
     * energy buffer, then pushes the buffered energy out through the energy ports.
     */
    private void annihilationTick() {
        ticksRequired = 1;
        boolean processed = false;
        GasStack hydrogen = new GasStack(mekanism.common.registries.MekanismGases.HYDROGEN.get(),
                ANNIHILATION_HYDROGEN_MB_PER_TICK);
        GasStack antimatter = new GasStack(mekanism.common.registries.MekanismGases.ANTIMATTER.get(),
                ANNIHILATION_ANTIMATTER_MB_PER_TICK);
        FluidStack helium = ChemRecipes.liquidHelium(ANNIHILATION_HELIUM_MB_PER_TICK);
        if (hasGasInput(hydrogen) && hasGasInput(antimatter)
                && !helium.isEmpty()
                && fluidIn.getFluidAmount() >= ANNIHILATION_HELIUM_MB_PER_TICK
                && fluidIn.getFluid().isFluidEqual(helium)
                && energy + ANNIHILATION_J_PER_TICK <= CAPACITY) {
            consumeGasInput(hydrogen);
            consumeGasInput(antimatter);
            fluidIn.drain(ANNIHILATION_HELIUM_MB_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            energy += ANNIHILATION_J_PER_TICK;
            processed = true;
            setChanged();
        }
        pushGeneratedEnergy();
        progress = processed ? 1 : 0;
        applyCoilGlow(processed, false);
        updateDisplay();
    }

    /** Whether this machine produces energy (extract allowed, insert refused). */
    private boolean isGenerator() {
        ChemMachineType type = machineType();
        return type == ChemMachineType.COMBUSTION_GENERATOR || type == ChemMachineType.ANNIHILATION_GENERATOR;
    }

    /** Whether the single stack fits into the output buffer (simulated slot by slot). */
    private boolean fitsItemStack(ItemStack out) {
        int remaining = out.getCount();
        for (int i = 0; i < OUTPUT_SLOTS && remaining > 0; i++) {
            remaining = outputs.insertItem(i, out.copyWithCount(remaining), true).getCount();
        }
        return remaining == 0;
    }

    /**
     * Drives the glow keep-alive: processing refreshes the timer, stalling lets it
     * decay so short gaps (operation boundaries, hopper delays) never blink the coils.
     */
    private void applyCoilGlow(boolean processedNow, boolean forceOff) {
        if (forceOff) {
            coilGlowGrace = 0;
        } else if (processedNow) {
            coilGlowGrace = COIL_GLOW_GRACE_TICKS;
        } else if (coilGlowGrace > 0) {
            coilGlowGrace--;
        }
        updateCoilGlow(coilGlowGrace > 0);
        setActive(coilGlowGrace > 0);
    }

    /** Tracks the running state and syncs it to clients (for the star / black-hole effect machines). */
    private void setActive(boolean now) {
        if (active == now || level == null || level.isClientSide) {
            return;
        }
        ChemMachineType type = machineType();
        if (type != ChemMachineType.STAR_GENERATOR && type != ChemMachineType.STABILIZER) {
            active = now;
            return; // other machines have no world render; skip the sync packet
        }
        active = now;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    /** Client-side: whether the machine is running (drives the star / black-hole render). */
    public boolean isEffectActive() {
        return active;
    }

    /** Lights or dims the EBF coil rings when the running state changes. */
    private void updateCoilGlow(boolean lit) {
        if (coilsLit == lit || !machineType().coilTower || level == null) {
            coilsLit = lit && machineType().coilTower;
            return;
        }
        coilsLit = lit;
        ChemMachineType type = machineType();
        Direction facing = getBlockState().getValue(ChemMachineBlock.FACING);
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // Sweep every ring layer of the structure's footprint and toggle whatever
        // coils are there — works for both the EBF tower and the ABS barrel since
        // non-coil blocks (casing, vents, air) are simply skipped.
        int halfW = (type.width - 1) / 2;
        for (int dy = 1; dy <= type.height - 2; dy++) {
            for (int dz = 0; dz < type.depth; dz++) {
                for (int dr = -halfW; dr <= halfW; dr++) {
                    cursor.set(worldPosition).move(back, dz).move(Direction.UP, dy).move(right, dr);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof com.falcon2235.moremultiblock.block.CoilBlock
                            && state.getValue(com.falcon2235.moremultiblock.block.CoilBlock.LIT) != lit) {
                        level.setBlock(cursor, state.setValue(com.falcon2235.moremultiblock.block.CoilBlock.LIT, lit), 3);
                    }
                }
            }
        }
    }

    /** Called when the controller block is removed so coils do not stay lit. */
    public void shutdownCoils() {
        updateCoilGlow(false);
    }

    /** Mekanism upgrade formulas: 8 speed upgrades = 10x faster, energy upgrades bring the cost back down. */
    private void recomputeUpgrades() {
        int speed = upgrades.getStackInSlot(0).getCount();
        int energyUpgrades = upgrades.getStackInSlot(1).getCount();
        upgradeTimeFactor = Math.pow(10, -speed / 8.0);
        upgradeEnergyFactor = Math.pow(10, (2.0 * speed - energyUpgrades) / 8.0);
    }

    /**
     * Each coil tier above the recipe's requirement halves the processing time,
     * then speed upgrades shorten it further.
     */
    private int effectiveTicks(ChemRecipe recipe) {
        int bonus = coilTier - recipe.coilTier;
        int ticks = bonus <= 0 ? recipe.ticks : Math.max(1, recipe.ticks >> bonus);
        return Math.max(1, (int) Math.round(ticks * upgradeTimeFactor));
    }

    private long effectiveEnergyPerTick(ChemRecipe recipe) {
        return Math.max(1L, (long) Math.ceil(recipe.energyPerTick * upgradeEnergyFactor));
    }

    private void updateDisplay() {
        clientEnergy10k = (int) Math.min(Integer.MAX_VALUE, energy / 10_000L);
    }

    @Nullable
    private ChemRecipe findRecipe() {
        for (ChemRecipe recipe : ChemRecipes.get(machineType())) {
            if (recipe.coilTier <= coilTier && canRun(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean canRun(ChemRecipe r) {
        return hasRequiredUpgrade(r)
                && hasItemInput(r.itemInput) && hasItemInput(r.itemInput2) && hasItemInput(r.itemInput3)
                && hasItemInput(r.itemInput4) && hasItemInput(r.itemInput5)
                && hasGasInput(r.gasInput) && hasGasInput(r.gasInput2) && hasFluidInput(r.fluidInput)
                && fitsItemOutputs(r) && fitsGasOutput(r.gasOutput) && fitsFluidOutput(r.fluidOutput);
    }

    /** Whether the recipe's required upgrade module (if any) is installed in the special slot. */
    private boolean hasRequiredUpgrade(ChemRecipe r) {
        if (r.requiredUpgrade.isEmpty()) {
            return true;
        }
        ItemStack module = upgrades.getStackInSlot(2);
        return !module.isEmpty() && ItemStack.isSameItemSameTags(module, r.requiredUpgrade);
    }

    private int countItem(ItemStack template) {
        int c = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack s = inputs.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, template)) {
                c += s.getCount();
            }
        }
        return c;
    }

    private boolean hasItemInput(ItemStack template) {
        return template.isEmpty() || countItem(template) >= template.getCount();
    }

    /** Checks both gas input tanks; same-type contents are summed. */
    private boolean hasGasInput(GasStack need) {
        if (need.isEmpty()) {
            return true;
        }
        long available = 0;
        if (!gasIn.isEmpty() && gasIn.isTypeEqual(need)) {
            available += gasIn.getAmount();
        }
        if (!gasIn2.isEmpty() && gasIn2.isTypeEqual(need)) {
            available += gasIn2.getAmount();
        }
        return available >= need.getAmount();
    }

    /** Removes the given gas amount from the input tanks (both if the type is split). */
    private void consumeGasInput(GasStack need) {
        long remaining = need.getAmount();
        if (!gasIn.isEmpty() && gasIn.isTypeEqual(need)) {
            long take = Math.min(remaining, gasIn.getAmount());
            gasIn = shrinkGas(gasIn, take);
            remaining -= take;
        }
        if (remaining > 0 && !gasIn2.isEmpty() && gasIn2.isTypeEqual(need)) {
            long take = Math.min(remaining, gasIn2.getAmount());
            gasIn2 = shrinkGas(gasIn2, take);
        }
    }

    private boolean hasFluidInput(FluidStack need) {
        return need.isEmpty() || (fluidIn.getFluidAmount() >= need.getAmount() && fluidIn.getFluid().isFluidEqual(need));
    }

    /**
     * Simulates inserting ALL of the recipe's item outputs at once (a centrifuge
     * separation can produce four different dusts per operation), merging into
     * matching stacks or claiming empty slots.
     */
    private boolean fitsItemOutputs(ChemRecipe r) {
        java.util.List<ItemStack> outs = r.itemOutputs();
        if (outs.isEmpty()) {
            return true;
        }
        ItemStack[] sim = new ItemStack[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            sim[i] = outputs.getStackInSlot(i).copy();
        }
        for (ItemStack out : outs) {
            int remaining = out.getCount();
            for (int i = 0; i < OUTPUT_SLOTS && remaining > 0; i++) {
                if (sim[i].isEmpty()) {
                    sim[i] = out.copyWithCount(remaining);
                    remaining = 0;
                } else if (ItemStack.isSameItemSameTags(sim[i], out)) {
                    int space = sim[i].getMaxStackSize() - sim[i].getCount();
                    int put = Math.min(space, remaining);
                    sim[i].grow(put);
                    remaining -= put;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean fitsGasOutput(GasStack out) {
        if (out.isEmpty()) {
            return true;
        }
        if (gasOut.isEmpty()) {
            return out.getAmount() <= GAS_CAP;
        }
        return gasOut.isTypeEqual(out) && gasOut.getAmount() + out.getAmount() <= GAS_CAP;
    }

    private boolean fitsFluidOutput(FluidStack out) {
        return out.isEmpty() || fluidOut.fill(out, IFluidHandler.FluidAction.SIMULATE) >= out.getAmount();
    }

    private void complete(ChemRecipe r) {
        if (!r.itemInput.isEmpty()) {
            consumeItem(r.itemInput);
        }
        if (!r.itemInput2.isEmpty()) {
            consumeItem(r.itemInput2);
        }
        if (!r.itemInput3.isEmpty()) {
            consumeItem(r.itemInput3);
        }
        if (!r.itemInput4.isEmpty()) {
            consumeItem(r.itemInput4);
        }
        if (!r.itemInput5.isEmpty()) {
            consumeItem(r.itemInput5);
        }
        if (!r.gasInput.isEmpty()) {
            consumeGasInput(r.gasInput);
        }
        if (!r.gasInput2.isEmpty()) {
            consumeGasInput(r.gasInput2);
        }
        if (!r.fluidInput.isEmpty()) {
            fluidIn.drain(r.fluidInput.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        for (ItemStack out : r.itemOutputs()) {
            insertItemOutput(out.copy());
        }
        if (!r.gasOutput.isEmpty()) {
            gasOut = growGas(gasOut, r.gasOutput);
        }
        if (!r.fluidOutput.isEmpty()) {
            fluidOut.fill(r.fluidOutput.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    private void consumeItem(ItemStack template) {
        int remaining = template.getCount();
        for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
            ItemStack s = inputs.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, template)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
                inputs.setStackInSlot(i, s.isEmpty() ? ItemStack.EMPTY : s);
            }
        }
    }

    private void insertItemOutput(ItemStack out) {
        for (int i = 0; i < OUTPUT_SLOTS && !out.isEmpty(); i++) {
            out = outputs.insertItem(i, out, false);
        }
    }

    private static GasStack shrinkGas(GasStack tank, long amount) {
        long rem = tank.getAmount() - amount;
        return rem <= 0 ? GasStack.EMPTY : new GasStack(tank, rem);
    }

    private static GasStack growGas(GasStack tank, GasStack add) {
        if (tank.isEmpty()) {
            return add.copy();
        }
        return new GasStack(tank, tank.getAmount() + add.getAmount());
    }

    // --- gas tank helpers (tanks: 0 = input, 1 = input 2, 2 = output) ---

    private GasStack getGasTank(int tank) {
        return switch (tank) {
            case 0 -> gasIn;
            case 1 -> gasIn2;
            case 2 -> gasOut;
            default -> GasStack.EMPTY;
        };
    }

    private void setGasTank(int tank, GasStack stack) {
        switch (tank) {
            case 0 -> gasIn = stack;
            case 1 -> gasIn2 = stack;
            case 2 -> gasOut = stack;
        }
        setChanged();
    }

    private GasStack fillGasTank(int tank, GasStack stack, Action action) {
        GasStack cur = getGasTank(tank);
        if (stack.isEmpty()) {
            return GasStack.EMPTY;
        }
        // Keep one gas type per tank: if the OTHER input tank already holds this
        // type, reject here so the handler-level loop merges into it instead.
        if (tank <= 1 && cur.isEmpty()) {
            GasStack other = getGasTank(1 - tank);
            if (!other.isEmpty() && other.isTypeEqual(stack)) {
                return stack;
            }
        }
        long space = cur.isEmpty() ? GAS_CAP : (cur.isTypeEqual(stack) ? GAS_CAP - cur.getAmount() : 0);
        if (space <= 0) {
            return stack;
        }
        long put = Math.min(space, stack.getAmount());
        if (action.execute()) {
            GasStack merged = cur.isEmpty() ? new GasStack(stack, put) : new GasStack(cur, cur.getAmount() + put);
            setGasTank(tank, merged);
        }
        long leftover = stack.getAmount() - put;
        return leftover <= 0 ? GasStack.EMPTY : new GasStack(stack, leftover);
    }

    private GasStack drainGasTank(int tank, long amount, Action action) {
        GasStack cur = getGasTank(tank);
        if (cur.isEmpty() || amount <= 0) {
            return GasStack.EMPTY;
        }
        long take = Math.min(amount, cur.getAmount());
        GasStack result = new GasStack(cur, take);
        if (action.execute()) {
            setGasTank(tank, shrinkGas(cur, take));
        }
        return result;
    }

    // --- capabilities ---

    private final IItemHandler externalItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return INPUT_SLOTS + OUTPUT_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return slot < INPUT_SLOTS ? inputs.getStackInSlot(slot) : outputs.getStackInSlot(slot - INPUT_SLOTS);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot < INPUT_SLOTS ? inputs.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < INPUT_SLOTS ? ItemStack.EMPTY : outputs.extractItem(slot - INPUT_SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < INPUT_SLOTS;
        }
    };

    private final IGasHandler gasHandler = new IGasHandler() {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public GasStack getChemicalInTank(int tank) {
            return getGasTank(tank);
        }

        @Override
        public void setChemicalInTank(int tank, GasStack stack) {
            setGasTank(tank, stack);
        }

        @Override
        public long getTankCapacity(int tank) {
            return GAS_CAP;
        }

        @Override
        public boolean isValid(int tank, GasStack stack) {
            return tank <= 1;
        }

        @Override
        public GasStack insertChemical(int tank, GasStack stack, Action action) {
            return tank <= 1 ? fillGasTank(tank, stack, action) : stack;
        }

        @Override
        public GasStack extractChemical(int tank, long amount, Action action) {
            return tank == 2 ? drainGasTank(2, amount, action) : GasStack.EMPTY;
        }
    };

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fluidIn.getFluid() : fluidOut.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return FLUID_CAP;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fluidIn.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return fluidOut.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return fluidOut.drain(maxDrain, action);
        }
    };

    private final IEnergyStorage feHandler = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (isGenerator()) {
                return 0; // generators only ever emit
            }
            long needed = CAPACITY - energy;
            if (needed <= 0 || maxReceive <= 0) {
                return 0;
            }
            int accepted = (int) Math.min(maxReceive, needed * 2 / 5);
            if (accepted <= 0) {
                return 0;
            }
            if (!simulate) {
                energy += accepted * 5L / 2;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!isGenerator() || maxExtract <= 0) {
                return 0;
            }
            int available = (int) Math.min(Integer.MAX_VALUE, energy * 2 / 5);
            int extracted = Math.min(maxExtract, available);
            if (extracted <= 0) {
                return 0;
            }
            if (!simulate) {
                energy -= extracted * 5L / 2;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, energy * 2 / 5);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, CAPACITY * 2 / 5);
        }

        @Override
        public boolean canExtract() {
            return isGenerator();
        }

        @Override
        public boolean canReceive() {
            return !isGenerator();
        }
    };

    private final IStrictEnergyHandler strictEnergy = new IStrictEnergyHandler() {
        @Override
        public int getEnergyContainerCount() {
            return 1;
        }

        @Override
        public FloatingLong getEnergy(int container) {
            return FloatingLong.create(energy);
        }

        @Override
        public void setEnergy(int container, FloatingLong value) {
            energy = value.longValue();
            setChanged();
        }

        @Override
        public FloatingLong getMaxEnergy(int container) {
            return FloatingLong.create(CAPACITY);
        }

        @Override
        public FloatingLong getNeededEnergy(int container) {
            return FloatingLong.create(Math.max(0, CAPACITY - energy));
        }

        @Override
        public FloatingLong insertEnergy(int container, FloatingLong amount, @NotNull Action action) {
            if (isGenerator()) {
                return amount; // generators only ever emit
            }
            long room = CAPACITY - energy;
            long toInsert = Math.min(amount.longValue(), room);
            if (toInsert <= 0) {
                return amount;
            }
            if (action.execute()) {
                energy += toInsert;
                setChanged();
            }
            return amount.subtract(toInsert);
        }

        @Override
        public FloatingLong extractEnergy(int container, FloatingLong amount, @NotNull Action action) {
            if (!isGenerator()) {
                return FloatingLong.ZERO;
            }
            long extracted = Math.min(amount.longValue(), energy);
            if (extracted <= 0) {
                return FloatingLong.ZERO;
            }
            if (action.execute()) {
                energy -= extracted;
                setChanged();
            }
            return FloatingLong.create(extracted);
        }
    };

    private final LazyOptional<IItemHandler> itemCapLO = LazyOptional.of(() -> externalItems);
    private final LazyOptional<IEnergyStorage> energyCapLO = LazyOptional.of(() -> feHandler);
    private final LazyOptional<IStrictEnergyHandler> strictCapLO = LazyOptional.of(() -> strictEnergy);
    private final LazyOptional<IGasHandler> gasCapLO = LazyOptional.of(() -> gasHandler);
    private final LazyOptional<IFluidHandler> fluidCapLO = LazyOptional.of(() -> fluidHandler);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return itemCapLO.cast();
            }
            if (cap == ForgeCapabilities.ENERGY) {
                return energyCapLO.cast();
            }
            if (cap == ForgeCapabilities.FLUID_HANDLER) {
                return fluidCapLO.cast();
            }
            if (cap == mekanism.common.capabilities.Capabilities.STRICT_ENERGY) {
                return strictCapLO.cast();
            }
            if (cap == mekanism.common.capabilities.Capabilities.GAS_HANDLER) {
                return gasCapLO.cast();
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

    // --- menu ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ChemMachineMenu(id, playerInventory, this);
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0D;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (ItemStackHandler handler : new ItemStackHandler[]{inputs, outputs, upgrades}) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    // --- display getters (client) ---

    public int displayProgress() {
        return progress;
    }

    public int displayTicksRequired() {
        return Math.max(1, ticksRequired);
    }

    public int displayEnergy10k() {
        return clientEnergy10k;
    }

    public int displayCapacity10k() {
        return (int) Math.min(Integer.MAX_VALUE, CAPACITY / 10_000L);
    }

    public boolean displayFormed() {
        return formed;
    }

    // Tank amounts are synced in units of 10 mB; scale back up for display.
    public int displayGasIn() {
        return dataAccess.get(5) * 10;
    }

    public int displayGasIn2() {
        return dataAccess.get(9) * 10;
    }

    public int displayGasOut() {
        return dataAccess.get(6) * 10;
    }

    public int displayFluidIn() {
        return dataAccess.get(7) * 10;
    }

    public int displayFluidOut() {
        return dataAccess.get(8) * 10;
    }

    public int gasCapacity() {
        return (int) GAS_CAP;
    }

    public int fluidCapacity() {
        return FLUID_CAP;
    }

    // --- nbt ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inputs", inputs.serializeNBT());
        tag.put("Outputs", outputs.serializeNBT());
        tag.put("Upgrades", upgrades.serializeNBT());
        tag.put("GasIn", gasIn.write(new CompoundTag()));
        tag.put("GasIn2", gasIn2.write(new CompoundTag()));
        tag.put("GasOut", gasOut.write(new CompoundTag()));
        tag.put("FluidIn", fluidIn.writeToNBT(new CompoundTag()));
        tag.put("FluidOut", fluidOut.writeToNBT(new CompoundTag()));
        tag.putLong("Energy", energy);
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inputs.deserializeNBT(tag.getCompound("Inputs"));
        outputs.deserializeNBT(tag.getCompound("Outputs"));
        upgrades.deserializeNBT(tag.getCompound("Upgrades"));
        upgradesDirty = true;
        gasIn = GasStack.readFromNBT(tag.getCompound("GasIn"));
        gasIn2 = GasStack.readFromNBT(tag.getCompound("GasIn2"));
        gasOut = GasStack.readFromNBT(tag.getCompound("GasOut"));
        fluidIn.readFromNBT(tag.getCompound("FluidIn"));
        fluidOut.readFromNBT(tag.getCompound("FluidOut"));
        energy = tag.getLong("Energy");
        progress = tag.getInt("Progress");
    }

    // --- client sync (running state, for the star / black-hole world render) ---

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", active);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        active = tag.getBoolean("Active");
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

    /** Enlarge the render box for the effect machines so the central star / black hole is not culled. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        ChemMachineType type = machineType();
        if (type == ChemMachineType.STAR_GENERATOR || type == ChemMachineType.STABILIZER) {
            return new net.minecraft.world.phys.AABB(worldPosition).inflate(20.0D);
        }
        return new net.minecraft.world.phys.AABB(worldPosition);
    }
}
