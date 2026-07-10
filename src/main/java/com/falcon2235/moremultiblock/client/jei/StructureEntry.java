package com.falcon2235.moremultiblock.client.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** One machine's multiblock structure, shown in the JEI "Structure" category. */
public class StructureEntry {

    /** How the structure preview is laid out. */
    public enum Mode {
        /** Hollow casing box (reactor, distillation, mixer, freezer, parallel machines). */
        BOX,
        /** GT vertical blast-furnace tower (solid base/top, coil rings, hollow centre column). */
        TOWER,
        /** GT alloy-blast-smelter barrel (5x5 plates, coil/vent rings, hollow 3x3x3). */
        BARREL,
        /** GTNH-style circuit assembly line (casing floor/walls, glass roof, hollow channel). */
        ASSEMBLY,
        /** GregTech fusion reactor (flat octagonal casing ring, coil ring, hollow core). */
        RING,
        /** Artificial star generator (giant rounded cube with face windows, hollow core). */
        SPHERE,
        /** Black hole stabilizer (chunk-sized wireframe cube: edges + face windows). */
        FRAME,
        /** Large hadron collider (giant flat octagonal accelerator loop). */
        LOOP,
        /** Void ore miner (drill rig: base plate, corner legs, glowing mast, crown platform). */
        DRILL,
        /** Oil drilling rig (GT fluid drill: 5x5 base, corner legs, drill-pipe string, crown). */
        RIG
    }

    public final ResourceLocation id;
    public final ItemStack controllerStack;
    public final BlockState controllerState;
    public final Component name;
    public final int width;
    public final int height;
    public final int depth;
    public final BlockState casingState;
    public final ItemStack casingStack;
    @Nullable
    public final BlockState coilState;
    @Nullable
    public final ItemStack coilStack;
    public final Mode mode;
    /** Heat-vent ring block (BARREL mode only). */
    @Nullable
    public final BlockState ventState;
    @Nullable
    public final ItemStack ventStack;
    /**
     * The full bill of materials — one stack per unique block with its required count
     * (counted from the construction blueprint; the controller itself is excluded).
     */
    public java.util.List<ItemStack> materials = java.util.List.of();

    public StructureEntry(ResourceLocation id, ItemStack controllerStack, BlockState controllerState,
                          Component name, int width, int height, int depth,
                          BlockState casingState, ItemStack casingStack,
                          @Nullable BlockState coilState, @Nullable ItemStack coilStack) {
        this(id, controllerStack, controllerState, name, width, height, depth,
                casingState, casingStack, coilState, coilStack, Mode.BOX, null, null);
    }

    public StructureEntry(ResourceLocation id, ItemStack controllerStack, BlockState controllerState,
                          Component name, int width, int height, int depth,
                          BlockState casingState, ItemStack casingStack,
                          @Nullable BlockState coilState, @Nullable ItemStack coilStack, boolean ebf) {
        this(id, controllerStack, controllerState, name, width, height, depth,
                casingState, casingStack, coilState, coilStack, ebf ? Mode.TOWER : Mode.BOX, null, null);
    }

    public StructureEntry(ResourceLocation id, ItemStack controllerStack, BlockState controllerState,
                          Component name, int width, int height, int depth,
                          BlockState casingState, ItemStack casingStack,
                          @Nullable BlockState coilState, @Nullable ItemStack coilStack, Mode mode,
                          @Nullable BlockState ventState, @Nullable ItemStack ventStack) {
        this.id = id;
        this.controllerStack = controllerStack;
        this.controllerState = controllerState;
        this.name = name;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.casingState = casingState;
        this.casingStack = casingStack;
        this.coilState = coilState;
        this.coilStack = coilStack;
        this.mode = mode;
        this.ventState = ventState;
        this.ventStack = ventStack;
    }

    /** Fluent: attach the counted bill of materials. */
    public StructureEntry withMaterials(java.util.List<ItemStack> materials) {
        this.materials = materials;
        return this;
    }
}
