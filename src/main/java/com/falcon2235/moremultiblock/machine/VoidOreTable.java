package com.falcon2235.moremultiblock.machine;

import com.falcon2235.moremultiblock.MMMRegistry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The void ore miner's loot table: every mining roll picks ONE raw-ore type at random,
 * weighted by rarity (commons like coal/iron dominate; naquadah is the jackpot), and
 * produces {@link #oresPerRoll()} of it. Yields RAW materials (raw iron, raw tin, raw
 * titanium, gems for the non-metals) rather than ore blocks. Built lazily after item
 * registration and cached; Mekanism raw items are resolved by id at build time.
 */
public final class VoidOreTable {

    /** How many raw ores one mining roll produces (config-driven). */
    public static int oresPerRoll() {
        return com.falcon2235.moremultiblock.MMMConfig.voidMinerOresPerRoll();
    }

    /** Base ticks between rolls (config-driven; Mekanism speed upgrades shorten it). */
    public static int rollIntervalTicks() {
        return com.falcon2235.moremultiblock.MMMConfig.voidMinerRollIntervalTicks();
    }

    /** Energy per working tick in Joules (config-driven; configured as RF/t). */
    public static long energyPerTick() {
        return com.falcon2235.moremultiblock.MMMConfig.voidMinerJPerTick();
    }

    /** One rollable raw ore: the raw item and its rarity weight. */
    public record Entry(ItemStack stack, int weight) {
    }

    private static List<Entry> entries;
    private static int totalWeight;

    private VoidOreTable() {
    }

    /** The full weighted table (immutable), for JEI display and the roll itself. */
    public static synchronized List<Entry> entries() {
        if (entries == null) {
            List<Entry> list = new ArrayList<>();
            // --- vanilla (raw metals; the non-metals drop their gem/dust directly) ---
            add(list, Items.COAL, 140);
            add(list, Items.RAW_IRON, 130);
            add(list, Items.RAW_COPPER, 130);
            add(list, Items.REDSTONE, 90);
            add(list, Items.RAW_GOLD, 60);
            add(list, Items.LAPIS_LAZULI, 55);
            add(list, Items.DIAMOND, 20);
            add(list, Items.EMERALD, 10);
            // --- mekanism raw ores ---
            addById(list, "mekanism", "raw_tin", 90);
            addById(list, "mekanism", "raw_lead", 85);
            addById(list, "mekanism", "raw_osmium", 80);
            addById(list, "mekanism", "raw_uranium", 35);
            addById(list, "mekanism", "fluorite_gem", 35);
            // --- ours ---
            add(list, MMMRegistry.RAW_NICKEL.get(), 70);
            add(list, MMMRegistry.RAW_MAGNESIUM.get(), 65);
            add(list, MMMRegistry.RAW_BAUXITE.get(), 60);
            add(list, MMMRegistry.RAW_TITANIUM.get(), 50);
            add(list, MMMRegistry.RAW_CHROMIUM.get(), 45);
            add(list, MMMRegistry.SALTPETER.get(), 45);
            add(list, MMMRegistry.RAW_ANTIMONY.get(), 40);
            add(list, MMMRegistry.RAW_COOPERITE.get(), 12);
            add(list, MMMRegistry.RAW_NAQUADAH.get(), 8);
            entries = List.copyOf(list);
            totalWeight = entries.stream().mapToInt(Entry::weight).sum();
        }
        return entries;
    }

    /** Sum of all rarity weights (for percentage display). */
    public static int totalWeight() {
        entries();
        return totalWeight;
    }

    /** Rolls one weighted raw-ore type and returns a fresh stack of {@link #oresPerRoll()}. */
    public static ItemStack roll(RandomSource random) {
        List<Entry> table = entries();
        if (table.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int pick = random.nextInt(totalWeight);
        for (Entry entry : table) {
            pick -= entry.weight();
            if (pick < 0) {
                return entry.stack().copyWithCount(oresPerRoll());
            }
        }
        return table.get(table.size() - 1).stack().copyWithCount(oresPerRoll());
    }

    private static void add(List<Entry> list, Item item, int weight) {
        list.add(new Entry(new ItemStack(item), weight));
    }

    private static void addById(List<Entry> list, String namespace, String path, int weight) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, path));
        if (item != null && item != Items.AIR) {
            list.add(new Entry(new ItemStack(item), weight));
        }
    }
}
