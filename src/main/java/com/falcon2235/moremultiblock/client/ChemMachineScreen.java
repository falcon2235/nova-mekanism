package com.falcon2235.moremultiblock.client;

import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.menu.ChemMachineMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Programmatic GUI for the chemical machines: item slots, a progress arrow, an
 * energy bar plus in/out gas and fluid level bars.
 */
public class ChemMachineScreen extends AbstractContainerScreen<ChemMachineMenu> {

    private static final String LANG = "gui." + MekanismMoreMultiblock.MODID + ".";

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_BAR_BG = 0xFF3B3B3B;
    private static final int COLOR_ENERGY = 0xFF76FF03;
    private static final int COLOR_PROGRESS = 0xFF00C853;
    private static final int COLOR_GAS = 0xFFB8A66A;
    private static final int COLOR_FLUID = 0xFF42A5F5;

    public ChemMachineScreen(ChemMachineMenu menu, Inventory playerInventory, Component title) {
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

        // progress arrow (between the input grid and the output column)
        int ax = x + 88;
        int ay = y + 34;
        g.fill(ax, ay, ax + 24, ay + 8, COLOR_BAR_BG);
        int pw = 24 * menu.be.displayProgress() / menu.be.displayTicksRequired();
        if (pw > 0) {
            g.fill(ax, ay, ax + Math.min(24, pw), ay + 8, COLOR_PROGRESS);
        }

        // vertical bars: gas in x2 + fluid in (left), gas out + fluid out (right)
        drawVBar(g, x + 2, y + 16, menu.be.displayGasIn(), menu.be.gasCapacity(), COLOR_GAS);
        drawVBar(g, x + 9, y + 16, menu.be.displayGasIn2(), menu.be.gasCapacity(), COLOR_GAS);
        drawVBar(g, x + 16, y + 16, menu.be.displayFluidIn(), menu.be.fluidCapacity(), COLOR_FLUID);
        drawVBar(g, x + 152, y + 16, menu.be.displayGasOut(), menu.be.gasCapacity(), COLOR_GAS);
        drawVBar(g, x + 160, y + 16, menu.be.displayFluidOut(), menu.be.fluidCapacity(), COLOR_FLUID);

        // energy bar (bottom of machine area) — stops short of the module slot at x+134
        int bx = x + 8;
        int by = y + 66;
        int barW = 120;
        g.fill(bx, by, bx + barW, by + 6, COLOR_BAR_BG);
        int cap = Math.max(1, menu.be.displayCapacity10k());
        int fw = (int) ((barW - 2) * Math.min(1.0D, menu.be.displayEnergy10k() / (double) cap));
        if (fw > 0) {
            g.fill(bx + 1, by + 1, bx + 1 + fw, by + 5, COLOR_ENERGY);
        }
    }

    private static void drawVBar(GuiGraphics g, int x, int y, int amount, int cap, int color) {
        int h = 46;
        g.fill(x, y, x + 6, y + h, COLOR_BAR_BG);
        int fill = (int) (h * Math.min(1.0D, amount / (double) Math.max(1, cap)));
        if (fill > 0) {
            g.fill(x, y + h - fill, x + 6, y + h, color);
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
            g.drawString(font, notFormed, (imageWidth - font.width(notFormed)) / 2, 72, 0xAA0000, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
