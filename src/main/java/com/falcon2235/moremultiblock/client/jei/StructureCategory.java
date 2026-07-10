package com.falcon2235.moremultiblock.client.jei;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.MekanismMoreMultiblock;

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
        if (entry.materials.isEmpty()) {
            // No blueprint (shouldn't happen) — fall back to the bare casing stack.
            builder.addSlot(RecipeIngredientRole.INPUT, 6, 28).addItemStack(entry.casingStack);
            return;
        }
        // Bill of materials: one slot per unique block; the stack count is how many
        // blocks the structure needs (counted from the construction blueprint).
        int x = 6;
        for (ItemStack material : entry.materials) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, x, 28);
            boolean heatingCoil = MMMRegistry.COIL_TIERS.stream()
                    .anyMatch(c -> material.getItem() == c.get().asItem());
            if (heatingCoil) {
                // Any heating-coil tier works (all must match); cycle through them with the count.
                java.util.List<ItemStack> coils = MMMRegistry.COIL_TIERS.stream()
                        .map(coil -> {
                            ItemStack stack = new ItemStack(coil.get());
                            stack.setCount(material.getCount());
                            return stack;
                        })
                        .toList();
                slot.addItemStacks(coils);
            } else {
                slot.addItemStack(material);
            }
            x += 20;
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
                : entry.mode == StructureEntry.Mode.FRAME ? 2.0F
                : entry.mode == StructureEntry.Mode.LOOP ? 2.3F
                : entry.mode == StructureEntry.Mode.DRILL ? 5.0F
                : entry.mode == StructureEntry.Mode.RIG ? 6.0F : 8.5F;
        StructurePreview.render(g, WIDTH / 2, 96, entry.width, entry.height, entry.depth,
                entry.controllerState, entry.casingState, entry.coilState, entry.mode, entry.ventState, scale);
    }
}
