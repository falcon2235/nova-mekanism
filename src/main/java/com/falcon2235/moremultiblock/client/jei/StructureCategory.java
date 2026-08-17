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
    /**
     * Deliberately compact. Recipe viewers frame a category by its declared size, and
     * EMI (via TooManyRecipeViewers) gives an oversized category no room to breathe —
     * a tall panel is what spills out of its frame. Everything therefore fits in a
     * modest box, with the preview hard-clipped into the space that is left.
     */
    private static final int HEIGHT = 118;
    /** Top of the material slot grid. */
    private static final int SLOTS_Y = 28;
    /** Material slots per row before wrapping. */
    private static final int SLOTS_PER_ROW = (WIDTH - 12) / 20;
    /**
     * Top of the preview box. Sits just under the hint's second line
     * (slots end at 48, hint runs 52..70), so the model starts on clear space.
     */
    private static final int PREVIEW_TOP = 71;
    /** Vertical centre of the isometric preview. */
    private static final int PREVIEW_CY = 94;
    /** Box the preview must fit inside, so it never reaches the slots/hint above. */
    private static final float PREVIEW_MAX_W = WIDTH - 16;
    private static final float PREVIEW_MAX_H = 44.0F;

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
        // Wraps to a second row so wide bills never run off the page.
        int index = 0;
        for (ItemStack material : entry.materials) {
            int x = 6 + (index % SLOTS_PER_ROW) * 20;
            int y = SLOTS_Y + (index / SLOTS_PER_ROW) * 20;
            index++;
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y);
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
        }
    }

    @Override
    public void draw(StructureEntry entry, IRecipeSlotsView slotsView, GuiGraphics g, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        // Machine names can be long ("Grand Imbuement Chamber Controller") — trim to the panel.
        int nameMax = WIDTH - 34;
        String name = entry.name.getString();
        if (font.width(name) > nameMax) {
            name = font.plainSubstrByWidth(name, nameMax - font.width("...")) + "...";
        }
        g.drawString(font, name, 30, 8, 0xFF404040, false);
        Component dims = Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".structure",
                entry.width, entry.height, entry.depth);
        g.drawString(font, dims, 30, 20, 0xFF606060, false);
        // The hint sits below however many rows the bill of materials needed, and is
        // wrapped instead of running off the page.
        int rows = Math.max(1, (entry.materials.size() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW);
        int hy = SLOTS_Y + rows * 20 + 4;
        Component hint = Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".structure_hint");
        for (var line : font.split(hint, WIDTH - 12)) {
            g.drawString(font, line, 6, hy, 0xFF808080, false);
            hy += font.lineHeight;
        }

        // Scale is derived from the structure's own size instead of a per-mode table,
        // so every machine — including a 33x33 collider — fits the preview box.
        float isoW = (entry.width + entry.depth) * 0.707F;              // projected width
        float isoH = (entry.width + entry.depth) * 0.354F + entry.height; // projected height
        float scale = Math.min(PREVIEW_MAX_W / Math.max(1.0F, isoW),
                PREVIEW_MAX_H / Math.max(1.0F, isoH));
        scale = Math.max(1.2F, Math.min(8.5F, scale));

        // Hard-clip the preview to its box. The size estimate above is geometry-based,
        // but recipe viewers differ in how they size and place a category (EMI via TMRV
        // lays this out differently from JEI), so the scissor guarantees the model can
        // never bleed into the slots/hint above it or outside the panel in ANY viewer.
        var matrix = g.pose().last().pose();
        int originX = (int) matrix.m30();
        int originY = (int) matrix.m31();
        g.enableScissor(originX, originY + PREVIEW_TOP, originX + WIDTH, originY + HEIGHT);
        StructurePreview.render(g, WIDTH / 2, PREVIEW_CY, entry.width, entry.height, entry.depth,
                entry.controllerState, entry.casingState, entry.coilState, entry.mode, entry.ventState, scale);
        g.disableScissor();
    }
}
