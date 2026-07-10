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
    public final int ticks;
    public final long energyPerTick;
    /** Special upgrade module the machine must have installed to run this recipe; EMPTY = none. */
    public ItemStack requiredUpgrade = ItemStack.EMPTY;
    /** Optional note drawn under the recipe in JEI (e.g. the void miner's roll chance); null = none. */
    public net.minecraft.network.chat.Component note;
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
}
