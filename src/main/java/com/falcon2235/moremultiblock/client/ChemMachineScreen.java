package com.falcon2235.moremultiblock.client;

import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.menu.ChemMachineMenu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Programmatic GUI for the chemical machines: item slots, a progress arrow, an
 * energy bar plus in/out gas and fluid level bars. Every bar has a hover tooltip
 * with its exact amount, and an unformed structure shows the validator's message
 * (including the offending block's coordinates) right in the panel.
 */
public class ChemMachineScreen extends AbstractContainerScreen<ChemMachineMenu> {

    private static final String LANG = "gui." + MekanismMoreMultiblock.MODID + ".";

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_BAR_BG = 0xFF3B3B3B;
    private static final int COLOR_BAR_EDGE = 0xFF1E1E1E;
    private static final int COLOR_ENERGY = 0xFF76FF03;
    private static final int COLOR_ENERGY_HI = 0xFFC6FF7A;
    private static final int COLOR_PROGRESS = 0xFF00C853;
    private static final int COLOR_GAS = 0xFFB8A66A;
    private static final int COLOR_FLUID = 0xFF42A5F5;
    private static final int COLOR_ERROR_BG = 0xE0200000;
    private static final int COLOR_ERROR_TEXT = 0xFFFF6E6E;

    /** Bar hit boxes registered each frame, for tooltips. */
    private record BarZone(int x, int y, int w, int h, Component tooltip) {
    }

    private final List<BarZone> zones = new ArrayList<>();

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
        zones.clear();

        g.fill(x, y, x + imageWidth, y + imageHeight, COLOR_PANEL);
        g.fill(x, y, x + imageWidth, y + 1, COLOR_PANEL_LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, COLOR_PANEL_LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, COLOR_PANEL_DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, COLOR_PANEL_DARK);

        for (Slot slot : menu.slots) {
            drawSlot(g, x + slot.x - 1, y + slot.y - 1);
        }

        // Fits the 17px gap between the input grid (ends x+80) and the output
        // column (starts x+97) — a wider arrow would sit under the output slots.
        drawProgressArrow(g, x + 81, y + 33);

        // vertical bars: gas in x2 + fluid in (left), gas out + fluid out (right)
        drawVBar(g, x + 2, y + 16, menu.be.displayGasIn(), menu.be.gasCapacity(), COLOR_GAS, LANG + "gas_in");
        drawVBar(g, x + 9, y + 16, menu.be.displayGasIn2(), menu.be.gasCapacity(), COLOR_GAS, LANG + "gas_in2");
        drawVBar(g, x + 16, y + 16, menu.be.displayFluidIn(), menu.be.fluidCapacity(), COLOR_FLUID, LANG + "fluid_in");
        drawVBar(g, x + 152, y + 16, menu.be.displayGasOut(), menu.be.gasCapacity(), COLOR_GAS, LANG + "gas_out");
        drawVBar(g, x + 160, y + 16, menu.be.displayFluidOut(), menu.be.fluidCapacity(), COLOR_FLUID, LANG + "fluid_out");

        // Energy bar sits above the inventory label (y=72) with a clear gap, and stops
        // short of the upgrade column at x+134.
        drawEnergyBar(g, x + 8, y + 63, 118);
    }

    /** A real arrow: a 10px shaft with a 4px head, filling left to right. */
    private void drawProgressArrow(GuiGraphics g, int ax, int ay) {
        int shaft = 10;
        int head = 4;
        int total = shaft + head;
        int progress = Math.min(total, total * menu.be.displayProgress() / menu.be.displayTicksRequired());
        // background silhouette
        g.fill(ax, ay + 2, ax + shaft, ay + 6, COLOR_BAR_BG);
        for (int i = 0; i < head; i++) {
            g.fill(ax + shaft + i, ay + i, ax + shaft + 1 + i, ay + 8 - i, COLOR_BAR_BG);
        }
        if (progress <= 0) {
            return;
        }
        // filled portion, clipped to the arrow silhouette
        g.fill(ax, ay + 2, ax + Math.min(shaft, progress), ay + 6, COLOR_PROGRESS);
        for (int i = 0; i < head && shaft + i < progress; i++) {
            g.fill(ax + shaft + i, ay + i, ax + shaft + 1 + i, ay + 8 - i, COLOR_PROGRESS);
        }
    }

    private void drawVBar(GuiGraphics g, int x, int y, int amount, int cap, int color, String labelKey) {
        int h = 46;
        int w = 6;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COLOR_BAR_EDGE);
        g.fill(x, y, x + w, y + h, COLOR_BAR_BG);
        int fill = (int) (h * Math.min(1.0D, amount / (double) Math.max(1, cap)));
        if (fill > 0) {
            g.fill(x, y + h - fill, x + w, y + h, color);
            // highlight column for a little depth
            g.fill(x, y + h - fill, x + 1, y + h, 0x40FFFFFF);
        }
        zones.add(new BarZone(x, y, w, h, Component.translatable(labelKey)
                .append(": ")
                .append(Component.translatable(LANG + "mb_amount", amount, cap))));
    }

    private void drawEnergyBar(GuiGraphics g, int bx, int by, int barW) {
        g.fill(bx - 1, by - 1, bx + barW + 1, by + 7, COLOR_BAR_EDGE);
        g.fill(bx, by, bx + barW, by + 6, COLOR_BAR_BG);
        int cap = Math.max(1, menu.be.displayCapacity10k());
        int stored = menu.be.displayEnergy10k();
        int fw = (int) ((barW - 2) * Math.min(1.0D, stored / (double) cap));
        if (fw > 0) {
            g.fill(bx + 1, by + 1, bx + 1 + fw, by + 5, COLOR_ENERGY);
            g.fill(bx + 1, by + 1, bx + 1 + fw, by + 2, COLOR_ENERGY_HI);
        }
        // stored/capacity are in units of 10k J; show RF (FE = J * 2/5)
        long storedFe = stored * 10_000L * 2 / 5;
        long capFe = cap * 10_000L * 2 / 5;
        zones.add(new BarZone(bx, by, barW, 6, Component.translatable(LANG + "energy_rf",
                formatFe(storedFe), formatFe(capFe))));
    }

    /** Compact RF label so multi-billion buffers stay readable. */
    private static String formatFe(long fe) {
        if (fe >= 1_000_000_000L) {
            return String.format("%.1fB", fe / 1_000_000_000.0D);
        }
        if (fe >= 1_000_000L) {
            return String.format("%.1fM", fe / 1_000_000.0D);
        }
        if (fe >= 1_000L) {
            return String.format("%.1fk", fe / 1_000.0D);
        }
        return Long.toString(fe);
    }

    private static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, COLOR_SLOT_DARK);
        g.fill(x + 1, y + 1, x + 18, y + 18, COLOR_PANEL_LIGHT);
        g.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT);
    }

    /** Banner geometry: fits strictly inside the machine area (never reaches the inventory label at y=72). */
    private static final int BANNER_TOP = 16;
    private static final int BANNER_MAX_BOTTOM = 70;
    private static final int BANNER_PAD = 4;

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Draw the labels ourselves so a long machine name can be trimmed to the panel.
        int titleMax = imageWidth - 12;
        Component shownTitle = font.width(title) <= titleMax
                ? title
                : Component.literal(font.plainSubstrByWidth(title.getString(), titleMax - font.width("...")) + "...");
        g.drawString(font, shownTitle, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        if (menu.be.displayFormed()) {
            return;
        }
        // Unformed: show the validator's own message (it names the offending block and
        // its coordinates). Clipped to the machine area; overflow goes to the tooltip.
        Component status = menu.be.getStatusMessage();
        Component message = status != null ? status : Component.translatable(LANG + "not_formed");
        Component header = Component.translatable(LANG + "not_formed");

        int lineStep = font.lineHeight + 1;
        int usable = BANNER_MAX_BOTTOM - BANNER_TOP - BANNER_PAD * 2 - lineStep; // minus the header line
        int maxLines = Math.max(1, usable / lineStep);
        List<FormattedCharSequence> lines = font.split(message, imageWidth - 16);
        boolean truncated = lines.size() > maxLines;
        if (truncated) {
            lines = lines.subList(0, maxLines);
        }

        int bannerH = BANNER_PAD * 2 + lineStep + lines.size() * lineStep;
        g.fill(BANNER_PAD, BANNER_TOP, imageWidth - BANNER_PAD, BANNER_TOP + bannerH, COLOR_ERROR_BG);
        g.drawString(font, header, (imageWidth - font.width(header)) / 2, BANNER_TOP + BANNER_PAD, 0xFFFF4040, false);

        int ly = BANNER_TOP + BANNER_PAD + lineStep;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            g.drawString(font, line, (imageWidth - font.width(line)) / 2, ly, COLOR_ERROR_TEXT, false);
            ly += lineStep;
        }
        if (truncated) {
            String more = "...";
            g.drawString(font, more, (imageWidth - font.width(more)) / 2, ly - lineStep, 0xFFFFAA00, false);
        }
        // full text on hover, so nothing is ever unreadable
        bannerZone = new BarZone(leftPos + BANNER_PAD, topPos + BANNER_TOP,
                imageWidth - BANNER_PAD * 2, bannerH, message);
    }

    /** Hover zone for the unformed banner (set during renderLabels, consumed in render). */
    private BarZone bannerZone;

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        bannerZone = null;
        super.render(g, mouseX, mouseY, partialTick);
        // The unformed banner covers the bars, so its tooltip (the full, untruncated
        // structure error) wins over theirs.
        if (bannerZone != null && inZone(bannerZone, mouseX, mouseY)) {
            g.renderComponentTooltip(font, font.getSplitter()
                    .splitLines(bannerZone.tooltip(), 200, net.minecraft.network.chat.Style.EMPTY)
                    .stream().map(t -> (Component) Component.literal(t.getString())).toList(), mouseX, mouseY);
            return;
        }
        // bar tooltips take precedence over the (empty) slot tooltip beneath them
        for (BarZone zone : zones) {
            if (inZone(zone, mouseX, mouseY)) {
                g.renderTooltip(font, zone.tooltip(), mouseX, mouseY);
                return;
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }

    private static boolean inZone(BarZone zone, int mouseX, int mouseY) {
        return mouseX >= zone.x() && mouseX < zone.x() + zone.w()
                && mouseY >= zone.y() && mouseY < zone.y() + zone.h();
    }
}
