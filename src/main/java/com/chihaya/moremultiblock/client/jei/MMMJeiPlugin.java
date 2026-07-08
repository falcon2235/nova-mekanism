package com.chihaya.moremultiblock.client.jei;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.MachineType;
import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.block.ChemMachineBlock;
import com.chihaya.moremultiblock.block.ControllerBlock;
import com.chihaya.moremultiblock.machine.ChemMachineType;
import com.chihaya.moremultiblock.machine.ChemRecipes;
import com.chihaya.moremultiblock.multiblock.MultiblockValidator;

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
                    RecipeType.create(MekanismMoreMultiblock.MODID, type.id, com.chihaya.moremultiblock.machine.ChemRecipe.class),
                    ChemRecipes.get(type));
        }
        registration.addRecipes(PbfRecipeCategory.TYPE, List.of(new PbfRecipeCategory.PbfDisplayRecipe()));
        registration.addRecipes(StructureCategory.TYPE, buildStructures());
    }

    private static List<StructureEntry> buildStructures() {
        List<StructureEntry> list = new ArrayList<>();
        Block parallelCasing = MMMRegistry.CASING.get();
        for (MachineType type : MachineType.values()) {
            Block block = MMMRegistry.CONTROLLERS.get(type).get();
            list.add(new StructureEntry(
                    new ResourceLocation(MekanismMoreMultiblock.MODID, "structure_" + type.id()),
                    new ItemStack(block),
                    block.defaultBlockState().setValue(ControllerBlock.FACING, Direction.NORTH),
                    block.getName(),
                    MultiblockValidator.WIDTH, MultiblockValidator.HEIGHT, MultiblockValidator.DEPTH,
                    parallelCasing.defaultBlockState(), new ItemStack(parallelCasing), null, null));
        }
        for (ChemMachineType type : ChemMachineType.values()) {
            Block block = MMMRegistry.CHEM_CONTROLLERS.get(type).get();
            Block casing = MMMRegistry.chemCasing(type);
            Block coil = MMMRegistry.chemCoil(type);
            boolean barrel = type == ChemMachineType.ALLOY_BLAST_FURNACE;
            boolean assembly = type == ChemMachineType.CIRCUIT_ASSEMBLER;
            boolean fusion = type == ChemMachineType.FUSION_REACTOR;
            boolean star = type == ChemMachineType.STAR_GENERATOR;
            boolean stabilizer = type == ChemMachineType.STABILIZER;
            if (fusion) {
                coil = MMMRegistry.FUSION_COIL.get();
            }
            // "accent" block: heat vent (barrel), glass roof (assembly), fusion/face glass,
            // stabilizer glass panels
            Block vent = barrel ? MMMRegistry.HEAT_VENT.get()
                    : assembly ? MMMRegistry.ASSEMBLY_GLASS.get()
                    : stabilizer ? MMMRegistry.STABILIZER_GLASS.get()
                    : (fusion || star) ? MMMRegistry.FUSION_GLASS.get() : null;
            StructureEntry.Mode mode = barrel ? StructureEntry.Mode.BARREL
                    : assembly ? StructureEntry.Mode.ASSEMBLY
                    : fusion ? StructureEntry.Mode.RING
                    : star ? StructureEntry.Mode.SPHERE
                    : stabilizer ? StructureEntry.Mode.FRAME
                    : type.coilTower ? StructureEntry.Mode.TOWER : StructureEntry.Mode.BOX;
            list.add(new StructureEntry(
                    new ResourceLocation(MekanismMoreMultiblock.MODID, "structure_" + type.id),
                    new ItemStack(block),
                    block.defaultBlockState().setValue(ChemMachineBlock.FACING, Direction.NORTH),
                    block.getName(),
                    type.width, type.height, type.depth,
                    casing.defaultBlockState(), new ItemStack(casing),
                    coil == null ? null : coil.defaultBlockState(),
                    coil == null ? null : new ItemStack(coil),
                    mode,
                    vent == null ? null : vent.defaultBlockState(),
                    vent == null ? null : new ItemStack(vent)));
        }
        // primitive blast furnace: same vertical shape as the EBF, all bricks
        Block pbf = MMMRegistry.PBF_CONTROLLER.get();
        list.add(new StructureEntry(
                new ResourceLocation(MekanismMoreMultiblock.MODID, "structure_primitive_blast_furnace"),
                new ItemStack(pbf),
                pbf.defaultBlockState().setValue(com.chihaya.moremultiblock.block.PbfBlock.FACING, Direction.NORTH),
                pbf.getName(),
                3, 4, 3,
                net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState(),
                new ItemStack(net.minecraft.world.level.block.Blocks.BRICKS),
                null, null, true));
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
                    RecipeType.create(MekanismMoreMultiblock.MODID, type.id, com.chihaya.moremultiblock.machine.ChemRecipe.class));
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
