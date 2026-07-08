package com.chihaya.moremultiblock.client;

import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.menu.ControllerMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Simple programmatically drawn GUI (no texture file needed).
 */
public class ControllerScreen extends AbstractContainerScreen<ControllerMenu> {

    private static final String LANG = "gui." + MekanismMoreMultiblock.MODID + ".";

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_BAR_BG = 0xFF3B3B3B;
    private static final int COLOR_ENERGY = 0xFF76FF03;
    private static final int COLOR_PROGRESS = 0xFF00C853;

    public ControllerScreen(ControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, COLOR_PANEL);
        graphics.fill(x, y, x + imageWidth, y + 1, COLOR_PANEL_LIGHT);
        graphics.fill(x, y, x + 1, y + imageHeight, COLOR_PANEL_LIGHT);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, COLOR_PANEL_DARK);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, COLOR_PANEL_DARK);

        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }

        int ax = x + 66;
        int ay = y + 34;
        graphics.fill(ax, ay, ax + 26, ay + 8, COLOR_BAR_BG);
        int ticks = menu.be.displayTicksRequired();
        int progressWidth = 26 * menu.be.displayProgress() / Math.max(1, ticks);
        if (progressWidth > 0) {
            graphics.fill(ax, ay, ax + Math.min(26, progressWidth), ay + 8, COLOR_PROGRESS);
        }

        int bx = x + 8;
        int by = y + 66;
        graphics.fill(bx, by, bx + 160, by + 8, COLOR_BAR_BG);
        int cap = Math.max(1, menu.be.displayCapacity10k());
        int fillWidth = (int) (158 * Math.min(1.0D, menu.be.displayEnergy10k() / (double) cap));
        if (fillWidth > 0) {
            graphics.fill(bx + 1, by + 1, bx + 1 + fillWidth, by + 7, COLOR_ENERGY);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, COLOR_SLOT_DARK);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, COLOR_PANEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (!menu.be.displayFormed()) {
            Component notFormed = Component.translatable(LANG + "not_formed");
            int width = font.width(notFormed);
            graphics.drawString(font, notFormed, (imageWidth - width) / 2, 56, 0xAA0000, false);
        } else {
            String batchText = menu.be.displayBatch() + "x";
            int width = font.width(batchText);
            graphics.drawString(font, batchText, 79 - width / 2, 25, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(8, 66, 160, 8, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(LANG + "energy",
                    format10k(menu.be.displayEnergy10k()), format10k(menu.be.displayCapacity10k())), mouseX, mouseY);
        }
        if (isHovering(66, 34, 26, 8, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(LANG + "parallel_tooltip",
                    menu.be.displayParallel(), menu.be.displayBatch()), mouseX, mouseY);
        }
    }

    /** Converts a value in units of 10 kJ into a MJ string. */
    private static String format10k(int value10k) {
        return String.format("%.2f", value10k / 100.0D);
    }
}
