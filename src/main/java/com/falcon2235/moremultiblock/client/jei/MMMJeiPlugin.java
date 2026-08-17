package com.falcon2235.moremultiblock.client.jei;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.MachineType;
import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.block.ChemMachineBlock;
import com.falcon2235.moremultiblock.block.ControllerBlock;
import com.falcon2235.moremultiblock.machine.ChemMachineType;
import com.falcon2235.moremultiblock.machine.ChemRecipes;
import com.falcon2235.moremultiblock.multiblock.MultiblockValidator;

import java.util.ArrayList;
import java.util.List;

import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.client.jei.MekanismJEIRecipeType;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers each multiblock controller as a JEI recipe catalyst for the machine
 * it represents, so viewing a controller's uses (right-click / U) lists the recipes
 * that machine can run and left-click / R shows how to craft the controller itself.
 *
 * <p>The Energized Smelter uses vanilla {@code minecraft:smelting} recipes, so the
 * smelting controller is bound to JEI's built-in furnace category. Enriching and
 * crushing reuse Mekanism's own JEI categories, resolved from Mekanism's
 * {@link MekanismJEIRecipeType} uid + recipe class (JEI matches categories by uid).
 */
@JeiPlugin
public class MMMJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("MekanismMoreMultiblockJEI");
    private static final ResourceLocation UID = new ResourceLocation(MekanismMoreMultiblock.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (ChemMachineType type : ChemMachineType.values()) {
            registration.addRecipeCategories(new ChemRecipeCategory(type, guiHelper));
        }
        registration.addRecipeCategories(new PbfRecipeCategory(guiHelper));
        registration.addRecipeCategories(new StructureCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (ChemMachineType type : ChemMachineType.values()) {
            registration.addRecipes(
                    RecipeType.create(MekanismMoreMultiblock.MODID, type.id, com.falcon2235.moremultiblock.machine.ChemRecipe.class),
                    ChemRecipes.get(type));
        }
        registration.addRecipes(PbfRecipeCategory.TYPE, List.of(new PbfRecipeCategory.PbfDisplayRecipe()));
        registration.addRecipes(StructureCategory.TYPE, buildStructures());
    }

    /**
     * Counts the structure's bill of materials from its construction blueprint
     * (the same offsets the terminal builds and the validator checks), collapsed
     * to one stack per unique block, largest count first.
     */
    private static List<ItemStack> countMaterials(net.minecraft.world.level.block.state.BlockState controllerState) {
        List<com.falcon2235.moremultiblock.multiblock.StructureBlueprint.Cell> cells =
                com.falcon2235.moremultiblock.multiblock.StructureBlueprint.forController(
                        net.minecraft.core.BlockPos.ZERO, controllerState);
        if (cells == null) {
            return List.of();
        }
        java.util.Map<Block, Integer> counts = new java.util.LinkedHashMap<>();
        for (var cell : cells) {
            counts.merge(cell.block(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    ItemStack stack = new ItemStack(e.getKey());
                    stack.setCount(e.getValue());
                    return stack;
                })
                .toList();
    }

    private static List<StructureEntry> buildStructures() {
        List<StructureEntry> list = new ArrayList<>();
        Block parallelCasing = MMMRegistry.CASING.get();
        for (MachineType type : MachineType.values()) {
            Block block = MMMRegistry.CONTROLLERS.get(type).get();
            var state = block.defaultBlockState().setValue(ControllerBlock.FACING, Direction.NORTH);
            list.add(new StructureEntry(
                    new ResourceLocation(MekanismMoreMultiblock.MODID, "structure_" + type.id()),
                    new ItemStack(block),
                    state,
                    block.getName(),
                    MultiblockValidator.WIDTH, MultiblockValidator.HEIGHT, MultiblockValidator.DEPTH,
                    parallelCasing.defaultBlockState(), new ItemStack(parallelCasing), null, null)
                    .withMaterials(countMaterials(state)));
        }
        for (ChemMachineType type : ChemMachineType.values()) {
            Block block = MMMRegistry.CHEM_CONTROLLERS.get(type).get();
            Block casing = MMMRegistry.chemCasing(type);
            Block coil = MMMRegistry.chemCoil(type);
            boolean barrel = type == ChemMachineType.ALLOY_BLAST_FURNACE;
            boolean assembly = type == ChemMachineType.CIRCUIT_ASSEMBLER;
            boolean fusion = type == ChemMachineType.FUSION_REACTOR;
            boolean star = type == ChemMachineType.STAR_GENERATOR || type == ChemMachineType.ANNIHILATION_GENERATOR;
            boolean stabilizer = type == ChemMachineType.STABILIZER;
            boolean collider = type == ChemMachineType.HADRON_COLLIDER;
            boolean voidMiner = type == ChemMachineType.VOID_MINER;
            boolean oilRig = type == ChemMachineType.OIL_RIG;
            boolean assline = type == ChemMachineType.ASSEMBLY_LINE;
            if (fusion) {
                coil = MMMRegistry.FUSION_COIL.get();
            } else if (collider) {
                coil = MMMRegistry.COLLIDER_MAGNET.get();
            } else if (voidMiner) {
                coil = MMMRegistry.VOID_DRILL.get();
            } else if (oilRig) {
                coil = MMMRegistry.DRILL_PIPE.get();
            } else if (assline || assembly) {
                coil = MMMRegistry.ASSLINE_CONVEYOR.get();
            }
            // "accent" block: heat vent (barrel), glass roof (assembly), fusion/face glass,
            // stabilizer glass panels
            Block vent = barrel ? MMMRegistry.HEAT_VENT.get()
                    : (assembly || assline) ? MMMRegistry.ASSEMBLY_GLASS.get()
                    : stabilizer ? MMMRegistry.STABILIZER_GLASS.get()
                    : (fusion || star || collider) ? MMMRegistry.FUSION_GLASS.get() : null;
            StructureEntry.Mode mode = barrel ? StructureEntry.Mode.BARREL
                    : assembly ? StructureEntry.Mode.ASSEMBLY
                    : fusion ? StructureEntry.Mode.RING
                    : star ? StructureEntry.Mode.SPHERE
                    : stabilizer ? StructureEntry.Mode.FRAME
                    : collider ? StructureEntry.Mode.LOOP
                    : voidMiner ? StructureEntry.Mode.DRILL
                    : oilRig ? StructureEntry.Mode.RIG
                    : assline ? StructureEntry.Mode.LINE
                    : type.coilTower ? StructureEntry.Mode.TOWER : StructureEntry.Mode.BOX;
            var chemState = block.defaultBlockState().setValue(ChemMachineBlock.FACING, Direction.NORTH);
            list.add(new StructureEntry(
                    new ResourceLocation(MekanismMoreMultiblock.MODID, "structure_" + type.id),
                    new ItemStack(block),
                    chemState,
                    block.getName(),
                    type.width, type.height, type.depth,
                    casing.defaultBlockState(), new ItemStack(casing),
                    coil == null ? null : coil.defaultBlockState(),
                    coil == null ? null : new ItemStack(coil),
                    mode,
                    vent == null ? null : vent.defaultBlockState(),
                    vent == null ? null : new ItemStack(vent))
                    .withMaterials(countMaterials(chemState)));
        }
        // primitive blast furnace: same vertical shape as the EBF, all bricks
        Block pbf = MMMRegistry.PBF_CONTROLLER.get();
        var pbfState = pbf.defaultBlockState().setValue(com.falcon2235.moremultiblock.block.PbfBlock.FACING, Direction.NORTH);
        list.add(new StructureEntry(
                new ResourceLocation(MekanismMoreMultiblock.MODID, "structure_primitive_blast_furnace"),
                new ItemStack(pbf),
                pbfState,
                pbf.getName(),
                3, 4, 3,
                net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState(),
                new ItemStack(net.minecraft.world.level.block.Blocks.BRICKS),
                null, null, true)
                .withMaterials(countMaterials(pbfState)));
        return list;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Smelting -> JEI's built-in furnace category (covers every minecraft:smelting recipe).
        registration.addRecipeCatalyst(controllerStack(MachineType.SMELTING), RecipeTypes.SMELTING);

        try {
            addMekanismCatalyst(registration, MachineType.ENRICHING, MekanismJEIRecipeType.ENRICHING);
            addMekanismCatalyst(registration, MachineType.CRUSHING, MekanismJEIRecipeType.CRUSHING);
        } catch (Throwable t) {
            LOGGER.error("Failed to register Mekanism JEI catalysts for enriching/crushing controllers", t);
        }

        // Chemical machine controllers -> their own recipe categories.
        for (ChemMachineType type : ChemMachineType.values()) {
            registration.addRecipeCatalyst(new ItemStack(MMMRegistry.CHEM_CONTROLLERS.get(type).get()),
                    RecipeType.create(MekanismMoreMultiblock.MODID, type.id, com.falcon2235.moremultiblock.machine.ChemRecipe.class));
        }

        // Every controller (parallel + chemical) -> the structure preview category.
        for (MachineType type : MachineType.values()) {
            registration.addRecipeCatalyst(controllerStack(type), StructureCategory.TYPE);
        }
        for (ChemMachineType type : ChemMachineType.values()) {
            registration.addRecipeCatalyst(new ItemStack(MMMRegistry.CHEM_CONTROLLERS.get(type).get()), StructureCategory.TYPE);
        }

        // Primitive blast furnace -> its recipe + structure categories.
        ItemStack pbfStack = new ItemStack(MMMRegistry.PBF_CONTROLLER.get());
        registration.addRecipeCatalyst(pbfStack, PbfRecipeCategory.TYPE);
        registration.addRecipeCatalyst(pbfStack, StructureCategory.TYPE);
    }

    private static void addMekanismCatalyst(IRecipeCatalystRegistration registration, MachineType machine,
                                            MekanismJEIRecipeType<ItemStackToItemStackRecipe> mekType) {
        ResourceLocation uid = mekType.uid();
        RecipeType<ItemStackToItemStackRecipe> jeiType =
                RecipeType.create(uid.getNamespace(), uid.getPath(), ItemStackToItemStackRecipe.class);
        registration.addRecipeCatalyst(controllerStack(machine), jeiType);
    }

    private static ItemStack controllerStack(MachineType machine) {
        Block controller = MMMRegistry.CONTROLLERS.get(machine).get();
        return new ItemStack(controller);
    }
}
