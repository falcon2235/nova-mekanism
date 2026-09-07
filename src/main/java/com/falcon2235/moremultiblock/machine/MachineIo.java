package com.falcon2235.moremultiblock.machine;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.item.ItemStack;

/**
 * Which item slots and tanks a machine actually uses, derived from its own recipe
 * list rather than hand-written per machine — a hardcoded table would go stale the
 * first time a recipe gained a second gas input.
 *
 * <p>The GUI uses this to hide the slots and bars a machine can never fill: an
 * electrolyzer has no business showing two gas inputs and a fluid input it will
 * never accept, and an empty tank bar reads as "broken" rather than "unused".
 */
public record MachineIo(int itemInputs, int itemOutputs,
                        boolean gasIn, boolean gasIn2, boolean fluidIn,
                        boolean gasOut, boolean fluidOut, boolean module) {

    private static final Map<ChemMachineType, MachineIo> CACHE = new EnumMap<>(ChemMachineType.class);

    public static synchronized MachineIo of(ChemMachineType type) {
        return CACHE.computeIfAbsent(type, MachineIo::scan);
    }

    /** Dropped alongside the recipe cache so a config reload re-derives the layout. */
    public static synchronized void invalidateCache() {
        CACHE.clear();
    }

    private static MachineIo scan(ChemMachineType type) {
        int inputs = 0;
        int outputs = 0;
        boolean gasIn = false;
        boolean gasIn2 = false;
        boolean fluidIn = false;
        boolean gasOut = false;
        boolean fluidOut = false;
        boolean module = false;

        for (ChemRecipe recipe : ChemRecipes.get(type)) {
            inputs = Math.max(inputs, countUsed(recipe.itemInput, recipe.itemInput2, recipe.itemInput3,
                    recipe.itemInput4, recipe.itemInput5));
            outputs = Math.max(outputs, countUsed(recipe.itemOutput, recipe.itemOutput2, recipe.itemOutput3,
                    recipe.itemOutput4));
            if (!recipe.chanceOutput.isEmpty()) {
                // The roll needs a slot of its own or it would have nowhere to land.
                outputs = Math.max(outputs, countUsed(recipe.itemOutput, recipe.itemOutput2, recipe.itemOutput3,
                        recipe.itemOutput4) + 1);
            }
            gasIn |= !recipe.gasInput.isEmpty();
            gasIn2 |= !recipe.gasInput2.isEmpty();
            fluidIn |= !recipe.fluidInput.isEmpty();
            gasOut |= !recipe.gasOutput.isEmpty();
            fluidOut |= !recipe.fluidOutput.isEmpty();
            module |= !recipe.requiredUpgrade.isEmpty();
        }

        // A machine with NO recipes at all — an optional integration is absent — would
        // otherwise render a completely blank window that reads as a bug, so it falls
        // back to one slot each. A machine that genuinely takes no items (the oil rig
        // pumps, the replicator eats only fluid) correctly shows none.
        if (ChemRecipes.get(type).isEmpty()) {
            return new MachineIo(1, 1, false, false, false, false, false, false);
        }
        return new MachineIo(inputs, Math.min(outputs, 4),
                gasIn, gasIn2, fluidIn, gasOut, fluidOut, module);
    }

    private static int countUsed(ItemStack... stacks) {
        int last = 0;
        for (int i = 0; i < stacks.length; i++) {
            if (!stacks[i].isEmpty()) {
                last = i + 1;
            }
        }
        return last;
    }

    /** Whether the given input slot index is ever used by this machine. */
    public boolean usesInputSlot(int index) {
        return index < itemInputs;
    }

    /** Whether the given output slot index is ever used by this machine. */
    public boolean usesOutputSlot(int index) {
        return index < itemOutputs;
    }
}
