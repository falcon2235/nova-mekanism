package com.falcon2235.moremultiblock.client;

import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.block.PortBlock;
import com.falcon2235.moremultiblock.menu.PortMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * GUI for port blocks: auto-transfer toggle plus direct access — the machine's item
 * slots on item ports, and a tank-item slot with a level bar on gas/fluid ports.
 */
public class PortScreen extends AbstractContainerScreen<PortMenu> {

    private static final String LANG = "gui." + MekanismMoreMultiblock.MODID + ".";

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_BAR_BG = 0xFF3B3B3B;
    private static final int COLOR_GAS = 0xFFB07CE8;
    private static final int COLOR_FLUID = 0xFF42A5F5;
    private static final int COLOR_BTN_ON = 0xFF2E7D32;
    private static final int COLOR_BTN_ON_FILL = 0xFF66BB6A;
    private static final int COLOR_BTN_OFF = 0xFF5D4037;
    private static final int COLOR_BTN_OFF_FILL = 0xFF9E9E9E;

    private static final int BTN_X = 112;
    private static final int BTN_Y = 14;
    private static final int BTN_W = 52;
    private static final int BTN_H = 18;

    private final PortBlock.PortType type;

    public PortScreen(PortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.type = menu.be.portType();
        imageWidth = 176;
        if (type == PortBlock.PortType.ENERGY) {
            imageHeight = 58;
            inventoryLabelY = -1000;
        } else {
            imageHeight = PortMenu.PLAYER_INV_Y + 58 + 16 + 8; // hotbar bottom + margin
            inventoryLabelY = PortMenu.PLAYER_INV_Y - 11;
        }
    }

    private boolean isTankPort() {
        return type == PortBlock.PortType.GAS_INPUT || type == PortBlock.PortType.GAS_OUTPUT
                || type == PortBlock.PortType.FLUID_INPUT || type == PortBlock.PortType.FLUID_OUTPUT;
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

        // auto toggle button
        boolean on = menu.be.dataAccess.get(0) == 1;
        int bx = x + BTN_X;
        int by = y + BTN_Y;
        g.fill(bx, by, bx + BTN_W, by + BTN_H, on ? COLOR_BTN_ON : COLOR_BTN_OFF);
        g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, on ? COLOR_BTN_ON_FILL : COLOR_BTN_OFF_FILL);
        Component label = Component.translatable(LANG + (on ? "on" : "off"));
        g.drawString(font, label, bx + (BTN_W - font.width(label)) / 2, by + 5, 0xFFFFFFFF, false);

        // tank level bar for gas/fluid ports
        if (isTankPort()) {
            int barX = x + 8;
            int barY = y + PortMenu.CONTENT_Y + 4;
            int barW = 100;
            int barH = 10;
            g.fill(barX, barY, barX + barW, barY + barH, COLOR_BAR_BG);
            int amount10 = menu.be.dataAccess.get(1);
            int cap10 = Math.max(1, menu.be.dataAccess.get(2));
            int fill = (int) ((barW - 2) * Math.min(1.0D, amount10 / (double) cap10));
            if (fill > 0) {
                boolean gas = type == PortBlock.PortType.GAS_INPUT || type == PortBlock.PortType.GAS_OUTPUT;
                g.fill(barX + 1, barY + 1, barX + 1 + fill, barY + barH - 1, gas ? COLOR_GAS : COLOR_FLUID);
            }
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
        boolean input = type.isAutoInput();
        Component label = Component.translatable(LANG + (input ? "auto_input" : "auto_output"));
        g.drawString(font, label, 10, BTN_Y + 5, 0xFF404040, false);
        if (isTankPort()) {
            long amount = menu.be.dataAccess.get(1) * 10L;
            long cap = menu.be.dataAccess.get(2) * 10L;
            Component text = Component.translatable(LANG + "mb_amount", amount, cap);
            g.drawString(font, text, 8, PortMenu.CONTENT_Y + 18, 0xFF606060, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
