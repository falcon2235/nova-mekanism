package com.chihaya.moremultiblock.client.jei;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.machine.ChemMachineType;
import com.chihaya.moremultiblock.machine.ChemRecipe;

import mekanism.client.jei.MekanismJEI;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for one chemical machine: shows the item/gas/fluid inputs and outputs
 * of each stage, plus an isometric 3D preview of how to build the multiblock.
 */
public class ChemRecipeCategory implements IRecipeCategory<ChemRecipe> {

    public static final int WIDTH = 184;
    public static final int HEIGHT = 64;

    private final ChemMachineType type;
    private final RecipeType<ChemRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public ChemRecipeCategory(ChemMachineType type, IGuiHelper guiHelper) {
        this.type = type;
        this.recipeType = RecipeType.create(MekanismMoreMultiblock.MODID, type.id, ChemRecipe.class);
        this.title = Component.translatable("block." + MekanismMoreMultiblock.MODID + "." + type.id + "_controller");
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(MMMRegistry.CHEM_CONTROLLERS.get(type).get()));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    public static ResourceLocation uid(ChemMachineType type) {
        return new ResourceLocation(MekanismMoreMultiblock.MODID, type.id);
    }

    @Override
    public RecipeType<ChemRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ChemRecipe recipe, IFocusGroup focuses) {
        int inX = 6;
        int outX = 144;
        if (!recipe.itemInput.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX, 6).addItemStack(recipe.itemInput);
        }
        if (!recipe.itemInput2.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX + 20, 6).addItemStack(recipe.itemInput2);
        }
        if (!recipe.itemInput3.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX + 40, 6).addItemStack(recipe.itemInput3);
        }
        if (!recipe.itemInput4.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX + 60, 6).addItemStack(recipe.itemInput4);
        }
        if (!recipe.itemInput5.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX + 80, 6).addItemStack(recipe.itemInput5);
        }
        if (!recipe.gasInput.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX, 26).addIngredient(MekanismJEI.TYPE_GAS, recipe.gasInput);
        }
        if (!recipe.gasInput2.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX + 20, 26).addIngredient(MekanismJEI.TYPE_GAS, recipe.gasInput2);
        }
        if (!recipe.fluidInput.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, inX, 46).addIngredient(ForgeTypes.FLUID_STACK, recipe.fluidInput);
        }
        // item outputs fill a 2x2 grid; gas/fluid outputs keep the right-hand column
        // (no recipe has 3+ item outputs AND a gas output, so the spots never clash)
        int[][] outPos = {{outX, 6}, {outX + 18, 6}, {outX, 26}, {outX + 18, 26}};
        java.util.List<ItemStack> outs = recipe.itemOutputs();
        for (int i = 0; i < outs.size() && i < outPos.length; i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[i][0], outPos[i][1]).addItemStack(outs.get(i));
        }
        if (!recipe.gasOutput.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outX, 26).addIngredient(MekanismJEI.TYPE_GAS, recipe.gasOutput);
        }
        if (!recipe.fluidOutput.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outX, 46).addIngredient(ForgeTypes.FLUID_STACK, recipe.fluidOutput);
        }
    }

    /** Compact FE/t label so huge fusion values (400M / 1B RF/t) still fit the panel. */
    private static String formatFe(long fe) {
        if (fe >= 1_000_000_000L) {
            return (fe / 1_000_000_000L) + "B";
        }
        if (fe >= 1_000_000L) {
            return (fe / 1_000_000L) + "M";
        }
        if (fe >= 10_000L) {
            return (fe / 1_000L) + "k";
        }
        return Long.toString(fe);
    }

    @Override
    public void draw(ChemRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics g, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // arrow between input and output columns (past the 5-slot input row)
        int ax = 112;
        int ay = 10;
        g.fill(ax, ay, ax + 22, ay + 6, 0xFF3B3B3B);
        g.fill(ax, ay + 1, ax + 20, ay + 5, 0xFF00C853);

        // time / energy text (energy shown in RF/t: our machines expose FE = Joules * 2/5)
        g.drawString(font, (recipe.ticks / 20) + "s", ax, ay + 12, 0xFF606060, false);
        g.drawString(font, formatFe(recipe.energyPerTick * 2 / 5) + " RF/t", ax - 6, ay + 24, 0xFF606060, false);

        // gas / fluid amounts next to their slots (inputs left, outputs right-aligned)
        if (!recipe.gasInput.isEmpty()) {
            int textX = recipe.gasInput2.isEmpty() ? 25 : 46;
            g.drawString(font, recipe.gasInput.getAmount() + " mB", textX, 27, 0xFF606060, false);
        }
        if (!recipe.gasInput2.isEmpty()) {
            g.drawString(font, recipe.gasInput2.getAmount() + " mB", 46, 37, 0xFF606060, false);
        }
        if (!recipe.fluidInput.isEmpty()) {
            g.drawString(font, recipe.fluidInput.getAmount() + " mB", 25, 51, 0xFF606060, false);
        }
        if (!recipe.gasOutput.isEmpty()) {
            String amount = recipe.gasOutput.getAmount() + " mB";
            g.drawString(font, amount, 141 - font.width(amount), 31, 0xFF606060, false);
        }
        if (!recipe.fluidOutput.isEmpty()) {
            String amount = recipe.fluidOutput.getAmount() + " mB";
            g.drawString(font, amount, 141 - font.width(amount), 51, 0xFF606060, false);
        }

        // required heating coil (coil-tower machines only); higher tiers halve the time per tier
        if (type.coilTower) {
            Component coil = Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".coil_req",
                    MMMRegistry.COIL_TIERS.get(recipe.coilTier).get().getName());
            g.drawString(font, coil, 6, HEIGHT - 10, 0xFF606060, false);
        }
    }
}
