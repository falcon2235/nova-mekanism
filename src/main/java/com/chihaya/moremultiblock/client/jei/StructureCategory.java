package com.chihaya.moremultiblock.client.jei;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.MekanismMoreMultiblock;

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
 * JEI category that shows a GregTech-style isometric 3D preview of any machine's
 * multiblock structure, for both the parallel machines (enriching/crushing/smelting)
 * and the chemical machines. Each controller is a catalyst for this category.
 */
public class StructureCategory implements IRecipeCategory<StructureEntry> {

    public static final ResourceLocation UID = new ResourceLocation(MekanismMoreMultiblock.MODID, "structure");
    public static final RecipeType<StructureEntry> TYPE = RecipeType.create(UID.getNamespace(), UID.getPath(), StructureEntry.class);
    private static final int WIDTH = 160;
    private static final int HEIGHT = 118;

    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public StructureCategory(IGuiHelper guiHelper) {
        this.title = Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".structure_title");
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(MMMRegistry.CASING.get()));
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<StructureEntry> getRecipeType() {
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
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StructureEntry entry, IFocusGroup focuses) {
        // The controller item lets JEI focus-filter to this entry, and marks which machine it is.
        builder.addSlot(RecipeIngredientRole.CATALYST, 6, 6).addItemStack(entry.controllerStack);
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 28).addItemStack(entry.casingStack);
        if (entry.coilStack != null) {
            boolean heatingCoil = MMMRegistry.COIL_TIERS.stream()
                    .anyMatch(c -> entry.coilStack.getItem() == c.get().asItem());
            if (heatingCoil) {
                // Any heating-coil tier works (all must match); cycle through them in the slot.
                java.util.List<ItemStack> coils = MMMRegistry.COIL_TIERS.stream()
                        .map(coil -> new ItemStack(coil.get()))
                        .toList();
                builder.addSlot(RecipeIngredientRole.INPUT, 26, 28).addItemStacks(coils);
            } else {
                // Dedicated coil (fusion reactor's superconducting coil).
                builder.addSlot(RecipeIngredientRole.INPUT, 26, 28).addItemStack(entry.coilStack);
            }
        }
        if (entry.ventStack != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 46, 28).addItemStack(entry.ventStack);
        }
    }

    @Override
    public void draw(StructureEntry entry, IRecipeSlotsView slotsView, GuiGraphics g, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, entry.name, 30, 10, 0xFF404040, false);
        Component dims = Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".structure",
                entry.width, entry.height, entry.depth);
        g.drawString(font, dims, 30, 24, 0xFF606060, false);
        Component hint = Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".structure_hint");
        g.drawString(font, hint, 6, 52, 0xFF808080, false);

        // the bigger barrel / longer assembly line / wide fusion ring need a smaller scale
        float scale = entry.mode == StructureEntry.Mode.BARREL ? 6.5F
                : entry.mode == StructureEntry.Mode.ASSEMBLY ? 7.0F
                : entry.mode == StructureEntry.Mode.RING ? 2.6F
                : entry.mode == StructureEntry.Mode.SPHERE ? 3.6F
                : entry.mode == StructureEntry.Mode.FRAME ? 2.0F : 8.5F;
        StructurePreview.render(g, WIDTH / 2, 96, entry.width, entry.height, entry.depth,
                entry.controllerState, entry.casingState, entry.coilState, entry.mode, entry.ventState, scale);
    }
}
