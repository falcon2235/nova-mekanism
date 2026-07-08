package com.chihaya.moremultiblock.client.jei;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.blockentity.PbfBlockEntity;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI category for the primitive blast furnace's fixed recipe:
 * 1 iron ingot + 2 coal → 1 steel ingot, 30 s, no power.
 */
public class PbfRecipeCategory implements IRecipeCategory<PbfRecipeCategory.PbfDisplayRecipe> {

    /** Marker recipe object (the PBF has a single hardcoded recipe). */
    public record PbfDisplayRecipe() {
    }

    public static final RecipeType<PbfDisplayRecipe> TYPE =
            RecipeType.create(MekanismMoreMultiblock.MODID, "primitive_blast_furnace", PbfDisplayRecipe.class);

    public static final int WIDTH = 168;
    public static final int HEIGHT = 48;

    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public PbfRecipeCategory(IGuiHelper guiHelper) {
        this.title = Component.translatable("block." + MekanismMoreMultiblock.MODID + ".primitive_blast_furnace_controller");
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(MMMRegistry.PBF_CONTROLLER.get()));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<PbfDisplayRecipe> getRecipeType() {
        return TYPE;
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
    public void setRecipe(IRecipeLayoutBuilder builder, PbfDisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 6).addItemStack(new ItemStack(Items.IRON_INGOT));
        builder.addSlot(RecipeIngredientRole.INPUT, 26, 6).addItemStacks(java.util.List.of(
                new ItemStack(Items.COAL, PbfBlockEntity.FUEL_PER_OPERATION),
                new ItemStack(Items.CHARCOAL, PbfBlockEntity.FUEL_PER_OPERATION)));
        Item steel = PbfBlockEntity.steelIngot();
        if (steel != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 146, 6).addItemStack(new ItemStack(steel));
        }
    }

    @Override
    public void draw(PbfDisplayRecipe recipe, IRecipeSlotsView slotsView, net.minecraft.client.gui.GuiGraphics g,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int ax = 74;
        int ay = 10;
        g.fill(ax, ay, ax + 22, ay + 6, 0xFF3B3B3B);
        g.fill(ax, ay + 1, ax + 20, ay + 5, 0xFFFF8F00);
        g.drawString(font, (PbfBlockEntity.TICKS_REQUIRED / 20) + "s", ax, ay + 12, 0xFF606060, false);
        g.drawString(font, Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".no_power"),
                6, HEIGHT - 12, 0xFF606060, false);
    }
}
