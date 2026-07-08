package com.chihaya.moremultiblock.client;

import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.blockentity.PbfBlockEntity;
import com.chihaya.moremultiblock.menu.PbfMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Programmatic GUI for the primitive blast furnace: iron + coal slots, an output
 * slot and a progress arrow. No energy — just bricks and fire.
 */
public class PbfScreen extends AbstractContainerScreen<PbfMenu> {

    private static final String LANG = "gui." + MekanismMoreMultiblock.MODID + ".";

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_BAR_BG = 0xFF3B3B3B;
    private static final int COLOR_PROGRESS = 0xFFFF8F00;

    public PbfScreen(PbfMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        g.fill(x, y, x + imageWidth, y + imageHeight, COLOR_PANEL);
        g.fill(x, y, x + imageWidth, y + 1, COLOR_PANEL_LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, COLOR_PANEL_LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, COLOR_PANEL_DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, COLOR_PANEL_DARK);

        for (Slot slot : menu.slots) {
            drawSlot(g, x + slot.x - 1, y + slot.y - 1);
        }

        // progress arrow
        int ax = x + 72;
        int ay = y + 36;
        g.fill(ax, ay, ax + 26, ay + 8, COLOR_BAR_BG);
        int pw = 26 * menu.be.displayProgress() / PbfBlockEntity.TICKS_REQUIRED;
        if (pw > 0) {
            g.fill(ax, ay, ax + Math.min(26, pw), ay + 8, COLOR_PROGRESS);
        }
    }

    private static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, COLOR_SLOT_DARK);
        g.fill(x + 1, y + 1, x + 18, y + 18, COLOR_PANEL_LIGHT);
        g.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (!menu.be.displayFormed()) {
            Component notFormed = Component.translatable(LANG + "not_formed");
            g.drawString(font, notFormed, (imageWidth - font.width(notFormed)) / 2, 66, 0xAA0000, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
