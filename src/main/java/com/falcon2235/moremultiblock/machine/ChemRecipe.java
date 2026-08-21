package com.falcon2235.moremultiblock.machine;

import mekanism.api.chemical.gas.GasStack;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * A hardcoded processing recipe for a chemical machine. Any of the item/gas/fluid
 * inputs and outputs may be empty; a machine only consumes/produces the non-empty ones.
 */
public class ChemRecipe {

    public final ItemStack itemInput;
    /** Optional second item input (mixer alloying etc.); EMPTY when unused. */
    public final ItemStack itemInput2;
    /** Optional third item input (three-way alloys); EMPTY when unused. */
    public final ItemStack itemInput3;
    /** Optional fourth item input (direct alloy-blast recipes); EMPTY when unused. */
    public final ItemStack itemInput4;
    /** Optional fifth item input (circuit assembler); EMPTY when unused. */
    public final ItemStack itemInput5;
    public final GasStack gasInput;
    /** Optional second gas input (two-gas reactions); EMPTY when unused. */
    public final GasStack gasInput2;
    public final FluidStack fluidInput;
    public final ItemStack itemOutput;
    /** Optional extra item outputs (centrifuge separation etc.); EMPTY when unused. */
    public final ItemStack itemOutput2;
    public final ItemStack itemOutput3;
    public final ItemStack itemOutput4;
    public final GasStack gasOutput;
    public final FluidStack fluidOutput;
    /** Mutable so the global config multipliers can adjust them after construction. */
    public int ticks;
    public long energyPerTick;
    /** Special upgrade module the machine must have installed to run this recipe; EMPTY = none. */
    public ItemStack requiredUpgrade = ItemStack.EMPTY;
    /** Optional note drawn under the recipe in JEI (e.g. the void miner's roll chance); null = none. */
    public net.minecraft.network.chat.Component note;
    /** Botania mana this recipe drains from the structure's mana hatches; 0 = none. */
    public int manaCost;
    /** Extra output rolled per operation (GT-style chance output); EMPTY = none. */
    public ItemStack chanceOutput = ItemStack.EMPTY;
    /** Percent chance (1-100) that {@link #chanceOutput} is produced. */
    public int chancePercent;
    /**
     * Minimum heating-coil tier required (blast furnace only; 0 elsewhere).
     * Each coil tier above this halves the processing time.
     */
    public final int coilTier;

    public ChemRecipe(ItemStack itemInput, GasStack gasInput, FluidStack fluidInput,
                      ItemStack itemOutput, GasStack gasOutput, FluidStack fluidOutput,
                      int ticks, long energyPerTick) {
        this(itemInput, ItemStack.EMPTY, ItemStack.EMPTY, gasInput, GasStack.EMPTY, fluidInput,
                itemOutput, gasOutput, fluidOutput, ticks, energyPerTick, 0);
    }

    public ChemRecipe(ItemStack itemInput, GasStack gasInput, FluidStack fluidInput,
                      ItemStack itemOutput, GasStack gasOutput, FluidStack fluidOutput,
                      int ticks, long energyPerTick, int coilTier) {
        this(itemInput, ItemStack.EMPTY, ItemStack.EMPTY, gasInput, GasStack.EMPTY, fluidInput,
                itemOutput, gasOutput, fluidOutput, ticks, energyPerTick, coilTier);
    }

    public ChemRecipe(ItemStack itemInput, ItemStack itemInput2, GasStack gasInput, FluidStack fluidInput,
                      ItemStack itemOutput, GasStack gasOutput, FluidStack fluidOutput,
                      int ticks, long energyPerTick, int coilTier) {
        this(itemInput, itemInput2, ItemStack.EMPTY, gasInput, GasStack.EMPTY, fluidInput,
                itemOutput, gasOutput, fluidOutput, ticks, energyPerTick, coilTier);
    }

    public ChemRecipe(ItemStack itemInput, ItemStack itemInput2, ItemStack itemInput3,
                      GasStack gasInput, GasStack gasInput2, FluidStack fluidInput,
                      ItemStack itemOutput, GasStack gasOutput, FluidStack fluidOutput,
                      int ticks, long energyPerTick, int coilTier) {
        this(itemInput, itemInput2, itemInput3, ItemStack.EMPTY, gasInput, gasInput2, fluidInput,
                itemOutput, gasOutput, fluidOutput, ticks, energyPerTick, coilTier);
    }

    public ChemRecipe(ItemStack itemInput, ItemStack itemInput2, ItemStack itemInput3, ItemStack itemInput4,
                      GasStack gasInput, GasStack gasInput2, FluidStack fluidInput,
                      ItemStack itemOutput, GasStack gasOutput, FluidStack fluidOutput,
                      int ticks, long energyPerTick, int coilTier) {
        this(itemInput, itemInput2, itemInput3, itemInput4, ItemStack.EMPTY, gasInput, gasInput2, fluidInput,
                itemOutput, gasOutput, fluidOutput, ticks, energyPerTick, coilTier);
    }

    public ChemRecipe(ItemStack itemInput, ItemStack itemInput2, ItemStack itemInput3, ItemStack itemInput4,
                      ItemStack itemInput5, GasStack gasInput, GasStack gasInput2, FluidStack fluidInput,
                      ItemStack itemOutput, GasStack gasOutput, FluidStack fluidOutput,
                      int ticks, long energyPerTick, int coilTier) {
        this(itemInput, itemInput2, itemInput3, itemInput4, itemInput5, gasInput, gasInput2, fluidInput,
                itemOutput, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, gasOutput, fluidOutput,
                ticks, energyPerTick, coilTier);
    }

    public ChemRecipe(ItemStack itemInput, ItemStack itemInput2, ItemStack itemInput3, ItemStack itemInput4,
                      ItemStack itemInput5, GasStack gasInput, GasStack gasInput2, FluidStack fluidInput,
                      ItemStack itemOutput, ItemStack itemOutput2, ItemStack itemOutput3, ItemStack itemOutput4,
                      GasStack gasOutput, FluidStack fluidOutput,
                      int ticks, long energyPerTick, int coilTier) {
        this.itemInput = itemInput;
        this.itemInput2 = itemInput2;
        this.itemInput3 = itemInput3;
        this.itemInput4 = itemInput4;
        this.itemInput5 = itemInput5;
        this.gasInput = gasInput;
        this.gasInput2 = gasInput2;
        this.fluidInput = fluidInput;
        this.itemOutput = itemOutput;
        this.itemOutput2 = itemOutput2;
        this.itemOutput3 = itemOutput3;
        this.itemOutput4 = itemOutput4;
        this.gasOutput = gasOutput;
        this.fluidOutput = fluidOutput;
        this.ticks = ticks;
        this.energyPerTick = energyPerTick;
        this.coilTier = coilTier;
    }

    /** Fluent: require an installed upgrade module (e.g. antimatter-forged) to run this recipe. */
    public ChemRecipe requireUpgrade(ItemStack upgrade) {
        this.requiredUpgrade = upgrade;
        return this;
    }

    /** Fluent: attach a JEI display note (drawn under the recipe panel). */
    public ChemRecipe withNote(net.minecraft.network.chat.Component note) {
        this.note = note;
        return this;
    }

    /** Fluent: require Botania mana (drained from the structure's mana hatches). */
    public ChemRecipe withMana(int mana) {
        this.manaCost = mana;
        return this;
    }

    /**
     * Fluent: add a GT-style chance output — rolled once per completed operation.
     * The machine only runs when there is room for it, so a roll is never voided.
     */
    public ChemRecipe withChance(ItemStack output, int percent) {
        this.chanceOutput = output;
        this.chancePercent = Math.max(1, Math.min(100, percent));
        return this;
    }

    /** The non-empty item outputs, in slot order. */
    public java.util.List<ItemStack> itemOutputs() {
        java.util.List<ItemStack> list = new java.util.ArrayList<>(4);
        for (ItemStack out : new ItemStack[]{itemOutput, itemOutput2, itemOutput3, itemOutput4}) {
            if (!out.isEmpty()) {
                list.add(out);
            }
        }
        return list;
    }

    /** The non-empty item INPUTS, in slot order (used by the shadowing audit). */
    public java.util.List<ItemStack> itemOutputsInputs() {
        java.util.List<ItemStack> list = new java.util.ArrayList<>(5);
        for (ItemStack in : new ItemStack[]{itemInput, itemInput2, itemInput3, itemInput4, itemInput5}) {
            if (!in.isEmpty()) {
                list.add(in);
            }
        }
        return list;
    }

    /** Guaranteed outputs plus the chance output, for output-space checks. */
    public java.util.List<ItemStack> allPossibleOutputs() {
        java.util.List<ItemStack> list = itemOutputs();
        if (!chanceOutput.isEmpty()) {
            list.add(chanceOutput);
        }
        return list;
    }
}
