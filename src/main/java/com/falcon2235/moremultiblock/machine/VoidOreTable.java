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
 * The void ore miner's loot table: every mining tick picks ONE ore type at random,
 * weighted by rarity (commons like coal/iron dominate; naquadah is the jackpot), and
 * produces {@link #ORES_PER_TICK} of it. Built lazily after item registration and
 * cached; Mekanism ores are resolved by id at build time.
 */
public final class VoidOreTable {

    /** How many ore blocks one mining tick produces. */
    public static final int ORES_PER_TICK = 10;
    /** Energy per mining tick: 1,000,000 RF/t = 2,500,000 J/t. */
    public static final long ENERGY_PER_TICK = 2_500_000L;

    /** One rollable ore: the ore block item and its rarity weight. */
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
            // --- vanilla ---
            add(list, Items.COAL_ORE, 140);
            add(list, Items.IRON_ORE, 130);
            add(list, Items.COPPER_ORE, 130);
            add(list, Items.REDSTONE_ORE, 90);
            add(list, Items.GOLD_ORE, 60);
            add(list, Items.LAPIS_ORE, 55);
            add(list, Items.DIAMOND_ORE, 20);
            add(list, Items.EMERALD_ORE, 10);
            // --- mekanism ---
            addById(list, "mekanism", "tin_ore", 90);
            addById(list, "mekanism", "lead_ore", 85);
            addById(list, "mekanism", "osmium_ore", 80);
            addById(list, "mekanism", "uranium_ore", 35);
            addById(list, "mekanism", "fluorite_ore", 35);
            // --- ours ---
            add(list, MMMRegistry.NICKEL_ORE.get().asItem(), 70);
            add(list, MMMRegistry.MAGNESIUM_ORE.get().asItem(), 65);
            add(list, MMMRegistry.BAUXITE_ORE.get().asItem(), 60);
            add(list, MMMRegistry.TITANIUM_ORE.get().asItem(), 50);
            add(list, MMMRegistry.CHROMIUM_ORE.get().asItem(), 45);
            add(list, MMMRegistry.SALTPETER_ORE.get().asItem(), 45);
            add(list, MMMRegistry.ANTIMONY_ORE.get().asItem(), 40);
            add(list, MMMRegistry.COOPERITE_ORE.get().asItem(), 12);
            add(list, MMMRegistry.NAQUADAH_ORE.get().asItem(), 8);
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

    /** Rolls one weighted ore type and returns a fresh stack of {@link #ORES_PER_TICK}. */
    public static ItemStack roll(RandomSource random) {
        List<Entry> table = entries();
        if (table.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int pick = random.nextInt(totalWeight);
        for (Entry entry : table) {
            pick -= entry.weight();
            if (pick < 0) {
                return entry.stack().copyWithCount(ORES_PER_TICK);
            }
        }
        return table.get(table.size() - 1).stack().copyWithCount(ORES_PER_TICK);
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
