package com.falcon2235.moremultiblock.blockentity;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.MachineType;
import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.block.ControllerBlock;
import com.falcon2235.moremultiblock.menu.ControllerMenu;
import com.falcon2235.moremultiblock.multiblock.MultiblockValidator;
import com.falcon2235.moremultiblock.multiblock.ParallelClaimRegistry;

import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import mekanism.api.recipes.ItemStackToItemStackRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Heart of the multiblock machine. Validates the structure, processes Mekanism
 * recipes in parallel batches and accepts Mekanism speed/energy upgrades.
 */
public class ControllerBlockEntity extends BlockEntity implements MenuProvider, PortHost {

    public static final int INPUT_SLOTS = 6;
    public static final int OUTPUT_SLOTS = 6;

    private static final ResourceLocation SPEED_UPGRADE_ID = new ResourceLocation("mekanism", "upgrade_speed");
    private static final ResourceLocation ENERGY_UPGRADE_ID = new ResourceLocation("mekanism", "upgrade_energy");
    private static final String LANG = "multiblock." + MekanismMoreMultiblock.MODID + ".";
    /** Joules of internal buffer per parallel operation. */
    private static final long CAPACITY_PER_PARALLEL = 1_000_000L;
    private static final int REVALIDATE_FORMED = 40;
    private static final int REVALIDATE_UNFORMED = 20;
    private static final long CLAIM_TTL = 200L;

    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        public void setSize(int size) {
            // keep the fixed slot count regardless of the "Size" tag in older saves
            super.setSize(INPUT_SLOTS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            cachedRecipe = null;
            recipeSearched = false;
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

    /** Slot 0: speed upgrades, slot 1: energy upgrades. Max 8 each, like Mekanism machines. */
    private final ItemStackHandler upgrades = new ItemStackHandler(2) {
        @Override
        public void setSize(int size) {
            super.setSize(2);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 8;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 ? isSpeedUpgrade(stack) : isEnergyUpgrade(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            upgradesDirty = true;
            setChanged();
        }
    };

    private long energy;
    private int progress;
    private int batch;
    private boolean formed;
    private int parallel = 1;
    private int ticksRequired = 200;
    private long energyPerTick = 50;
    private int revalidateIn;
    private boolean upgradesDirty = true;
    private boolean recipeSearched;
    private Component statusMessage = Component.translatable(LANG + "not_formed");
    @Nullable
    private ProcessRecipe cachedRecipe;

    private int clientEnergy10k;
    private int clientCapacity10k;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> Math.max(1, ticksRequired);
                case 2 -> clientEnergy10k;
                case 3 -> clientCapacity10k;
                case 4 -> parallel;
                case 5 -> batch;
                case 6 -> formed ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> ticksRequired = value;
                case 2 -> clientEnergy10k = value;
                case 3 -> clientCapacity10k = value;
                case 4 -> parallel = value;
                case 5 -> batch = value;
                case 6 -> formed = value == 1;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public ControllerBlockEntity(BlockPos pos, BlockState state) {
        super(MMMRegistry.CONTROLLER_BE.get(), pos, state);
    }

    public MachineType machineType() {
        return getBlockState().getBlock() instanceof ControllerBlock controller ? controller.machineType : MachineType.ENRICHING;
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

    public long capacity() {
        return CAPACITY_PER_PARALLEL * Math.max(1, parallel);
    }

    public static boolean isSpeedUpgrade(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(SPEED_UPGRADE_ID);
    }

    public static boolean isEnergyUpgrade(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ENERGY_UPGRADE_ID);
    }

    public void revalidate() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(ControllerBlock.FACING);
        MultiblockValidator.Result result = MultiblockValidator.validate(level, worldPosition, facing);
        formed = result.valid();
        if (formed) {
            long now = level.getGameTime();
            int best = 1;
            for (MultiblockValidator.ParallelUnit unit : result.parallelUnits()) {
                if (ParallelClaimRegistry.tryClaim(level, unit.pos(), worldPosition, now, CLAIM_TTL)) {
                    best = Math.max(best, unit.tier());
                }
            }
            parallel = best;
            statusMessage = Component.translatable(LANG + "formed", parallel);
            for (BlockPos portPos : result.ports()) {
                if (level.getBlockEntity(portPos) instanceof PortBlockEntity port) {
                    port.setController(worldPosition);
                }
            }
        } else {
            ParallelClaimRegistry.release(level, worldPosition);
            parallel = 1;
            statusMessage = result.error();
        }
        revalidateIn = formed ? REVALIDATE_FORMED : REVALIDATE_UNFORMED;
    }

    public void onControllerRemoved() {
        if (level != null && !level.isClientSide) {
            ParallelClaimRegistry.release(level, worldPosition);
        }
    }

    public void serverTick() {
        if (--revalidateIn <= 0) {
            revalidate();
        }
        if (!formed) {
            if (progress != 0 || batch != 0) {
                progress = 0;
                batch = 0;
            }
            updateDisplayFields();
            return;
        }
        if (upgradesDirty) {
            recomputeUpgrades();
            upgradesDirty = false;
        }

        RecipeMatch match = findRecipe();
        if (match == null) {
            progress = 0;
            batch = 0;
            updateDisplayFields();
            return;
        }

        int possible = computeBatch(match.recipe(), match.stack());
        batch = progress == 0 ? Math.min(parallel, possible) : Math.min(batch, possible);
        if (batch <= 0) {
            progress = 0;
            updateDisplayFields();
            return;
        }

        long cost = energyPerTick * batch;
        if (energy >= cost) {
            energy -= cost;
            progress++;
            if (progress >= ticksRequired) {
                complete(match.recipe(), match.stack());
                progress = 0;
                batch = 0;
            }
            setChanged();
        }
        updateDisplayFields();
    }

    private void updateDisplayFields() {
        clientEnergy10k = (int) Math.min(Integer.MAX_VALUE, energy / 10_000L);
        clientCapacity10k = (int) Math.min(Integer.MAX_VALUE, capacity() / 10_000L);
    }

    private void recomputeUpgrades() {
        int speed = upgrades.getStackInSlot(0).getCount();
        int energyUpgrades = upgrades.getStackInSlot(1).getCount();
        MachineType type = machineType();
        ticksRequired = Math.max(1, (int) Math.round(type.baseTicks * Math.pow(10, -speed / 8.0)));
        energyPerTick = Math.max(1L, (long) Math.ceil(type.baseUsage * Math.pow(10, (2.0 * speed - energyUpgrades) / 8.0)));
    }

    /**
     * A resolved recipe abstracted over its backend: Mekanism's own
     * {@link ItemStackToItemStackRecipe} (enriching / crushing) or a vanilla
     * furnace {@link SmeltingRecipe} (the Energized Smelter uses minecraft:smelting).
     */
    private interface ProcessRecipe {
        boolean matches(ItemStack input);

        ItemStack assemble(ItemStack input);
    }

    private record RecipeMatch(ProcessRecipe recipe, ItemStack stack) {
    }

    private ProcessRecipe wrap(ItemStackToItemStackRecipe recipe) {
        return new ProcessRecipe() {
            @Override
            public boolean matches(ItemStack input) {
                return recipe.test(input);
            }

            @Override
            public ItemStack assemble(ItemStack input) {
                return recipe.getOutput(input);
            }
        };
    }

    private ProcessRecipe wrap(SmeltingRecipe recipe) {
        return new ProcessRecipe() {
            @Override
            public boolean matches(ItemStack input) {
                return recipe.matches(new SimpleContainer(input), level);
            }

            @Override
            public ItemStack assemble(ItemStack input) {
                return recipe.assemble(new SimpleContainer(input), level.registryAccess());
            }
        };
    }

    @Nullable
    private ProcessRecipe lookup(ItemStack stack) {
        if (machineType() == MachineType.SMELTING) {
            return level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level)
                    .map(this::wrap)
                    .orElse(null);
        }
        for (ItemStackToItemStackRecipe recipe : level.getRecipeManager().getAllRecipesFor(machineType().recipeType())) {
            if (recipe.test(stack)) {
                return wrap(recipe);
            }
        }
        return null;
    }

    @Nullable
    private RecipeMatch findRecipe() {
        if (level == null) {
            return null;
        }
        if (recipeSearched) {
            if (cachedRecipe == null) {
                return null;
            }
            for (int i = 0; i < INPUT_SLOTS; i++) {
                ItemStack stack = inputs.getStackInSlot(i);
                if (!stack.isEmpty() && cachedRecipe.matches(stack)) {
                    return new RecipeMatch(cachedRecipe, stack);
                }
            }
            return null;
        }

        recipeSearched = true;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputs.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ProcessRecipe found = lookup(stack);
            if (found != null) {
                cachedRecipe = found;
                return new RecipeMatch(found, stack);
            }
        }
        cachedRecipe = null;
        return null;
    }

    private int computeBatch(ProcessRecipe recipe, ItemStack template) {
        int available = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputs.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, template)) {
                available += stack.getCount();
            }
        }
        ItemStack output = recipe.assemble(template);
        if (output.isEmpty()) {
            return 0;
        }
        int space = 0;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack existing = outputs.getStackInSlot(i);
            if (existing.isEmpty()) {
                space += output.getMaxStackSize();
            } else if (ItemStack.isSameItemSameTags(existing, output)) {
                space += Math.max(0, existing.getMaxStackSize() - existing.getCount());
            }
        }
        return Math.min(available, space / output.getCount());
    }

    private void complete(ProcessRecipe recipe, ItemStack template) {
        ItemStack matcher = template.copy();
        ItemStack output = recipe.assemble(matcher).copy();
        int remaining = batch;
        for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
            ItemStack stack = inputs.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, matcher)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                inputs.setStackInSlot(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        insertOutputs(output, (long) output.getCount() * batch);
    }

    private void insertOutputs(ItemStack prototype, long total) {
        for (int i = 0; i < OUTPUT_SLOTS && total > 0; i++) {
            ItemStack existing = outputs.getStackInSlot(i);
            if (existing.isEmpty()) {
                int put = (int) Math.min(total, prototype.getMaxStackSize());
                outputs.setStackInSlot(i, prototype.copyWithCount(put));
                total -= put;
            } else if (ItemStack.isSameItemSameTags(existing, prototype)) {
                int put = (int) Math.min(total, existing.getMaxStackSize() - existing.getCount());
                if (put > 0) {
                    existing.grow(put);
                    outputs.setStackInSlot(i, existing);
                    total -= put;
                }
            }
        }
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

    private final IEnergyStorage feHandler = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            long needed = capacity() - energy;
            if (needed <= 0 || maxReceive <= 0) {
                return 0;
            }
            // 1 FE = 2.5 J (Mekanism default conversion rate)
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
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, energy * 2 / 5);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, capacity() * 2 / 5);
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
            return FloatingLong.create(capacity());
        }

        @Override
        public FloatingLong getNeededEnergy(int container) {
            return FloatingLong.create(Math.max(0, capacity() - energy));
        }

        @Override
        public FloatingLong insertEnergy(int container, FloatingLong amount, @NotNull Action action) {
            long room = capacity() - energy;
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
            return FloatingLong.ZERO;
        }
    };

    private final LazyOptional<IItemHandler> itemCapLO = LazyOptional.of(() -> externalItems);
    private final LazyOptional<IEnergyStorage> energyCapLO = LazyOptional.of(() -> feHandler);
    private final LazyOptional<IStrictEnergyHandler> strictCapLO = LazyOptional.of(() -> strictEnergy);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return itemCapLO.cast();
            }
            if (cap == ForgeCapabilities.ENERGY) {
                return energyCapLO.cast();
            }
            if (cap == mekanism.common.capabilities.Capabilities.STRICT_ENERGY) {
                return strictCapLO.cast();
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
    }

    public IEnergyStorage getFeHandler() {
        return feHandler;
    }

    public IStrictEnergyHandler getStrictEnergyHandler() {
        return strictEnergy;
    }

    public IItemHandler getExternalItemHandler() {
        return externalItems;
    }

    // --- menu ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ControllerMenu(id, playerInventory, this);
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

    // --- display getters used by the GUI (client side, synced via dataAccess) ---

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
        return clientCapacity10k;
    }

    public int displayParallel() {
        return parallel;
    }

    public int displayBatch() {
        return batch;
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
        tag.put("Upgrades", upgrades.serializeNBT());
        tag.putLong("Energy", energy);
        tag.putInt("Progress", progress);
        tag.putInt("Batch", batch);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inputs.deserializeNBT(tag.getCompound("Inputs"));
        outputs.deserializeNBT(tag.getCompound("Outputs"));
        upgrades.deserializeNBT(tag.getCompound("Upgrades"));
        energy = tag.getLong("Energy");
        progress = tag.getInt("Progress");
        batch = tag.getInt("Batch");
        upgradesDirty = true;
    }
}
