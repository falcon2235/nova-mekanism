package com.falcon2235.moremultiblock;

import mekanism.api.recipes.ItemStackToItemStackRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * The Mekanism machine processes this addon provides as multiblocks.
 * Each one maps to a Mekanism ItemStack-to-ItemStack recipe type.
 */
public enum MachineType {
    ENRICHING("enriching", 200, 50L),
    CRUSHING("crushing", 200, 50L),
    SMELTING("smelting", 200, 50L);

    private final String id;
    /** Base ticks for one operation, before speed upgrades. */
    public final int baseTicks;
    /** Base joules per tick per parallel operation, before upgrades. */
    public final long baseUsage;

    private RecipeType<ItemStackToItemStackRecipe> cached;

    MachineType(String id, int baseTicks, long baseUsage) {
        this.id = id;
        this.baseTicks = baseTicks;
        this.baseUsage = baseUsage;
    }

    public String id() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public RecipeType<ItemStackToItemStackRecipe> recipeType() {
        if (cached == null) {
            cached = (RecipeType<ItemStackToItemStackRecipe>) BuiltInRegistries.RECIPE_TYPE
                    .get(new ResourceLocation("mekanism", id));
        }
        return cached;
    }
}
