package com.falcon2235.moremultiblock;

import com.falcon2235.moremultiblock.block.ChemMachineBlock;
import com.falcon2235.moremultiblock.block.CoilBlock;
import com.falcon2235.moremultiblock.block.ControllerBlock;
import com.falcon2235.moremultiblock.block.ParallelProcessorBlock;
import com.falcon2235.moremultiblock.block.PbfBlock;
import com.falcon2235.moremultiblock.block.PortBlock;
import com.falcon2235.moremultiblock.blockentity.ChemMachineBlockEntity;
import com.falcon2235.moremultiblock.blockentity.ControllerBlockEntity;
import com.falcon2235.moremultiblock.blockentity.PbfBlockEntity;
import com.falcon2235.moremultiblock.blockentity.PortBlockEntity;
import com.falcon2235.moremultiblock.machine.ChemMachineType;
import com.falcon2235.moremultiblock.menu.ChemMachineMenu;
import com.falcon2235.moremultiblock.menu.ControllerMenu;
import com.falcon2235.moremultiblock.menu.PbfMenu;
import com.falcon2235.moremultiblock.menu.PortMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MMMRegistry {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MekanismMoreMultiblock.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MekanismMoreMultiblock.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MekanismMoreMultiblock.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MekanismMoreMultiblock.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MekanismMoreMultiblock.MODID);

    public static final int[] PARALLEL_TIERS = {10, 25, 50, 100, 200, 300};

    public static final RegistryObject<Block> CASING = registerBlock("multiblock_casing", () -> new Block(props()));

    public static final List<RegistryObject<ParallelProcessorBlock>> PARALLEL_BLOCKS = makeParallelBlocks();

    public static final Map<MachineType, RegistryObject<ControllerBlock>> CONTROLLERS = makeControllers();

    public static final RegistryObject<PortBlock> ENERGY_PORT =
            registerBlock("energy_port", () -> new PortBlock(props(), PortBlock.PortType.ENERGY));
    public static final RegistryObject<PortBlock> ITEM_INPUT_PORT =
            registerBlock("item_input_port", () -> new PortBlock(props(), PortBlock.PortType.ITEM_INPUT));
    public static final RegistryObject<PortBlock> ITEM_OUTPUT_PORT =
            registerBlock("item_output_port", () -> new PortBlock(props(), PortBlock.PortType.ITEM_OUTPUT));
    public static final RegistryObject<PortBlock> GAS_INPUT_PORT =
            registerBlock("gas_input_port", () -> new PortBlock(props(), PortBlock.PortType.GAS_INPUT));
    public static final RegistryObject<PortBlock> GAS_OUTPUT_PORT =
            registerBlock("gas_output_port", () -> new PortBlock(props(), PortBlock.PortType.GAS_OUTPUT));
    public static final RegistryObject<PortBlock> FLUID_INPUT_PORT =
            registerBlock("fluid_input_port", () -> new PortBlock(props(), PortBlock.PortType.FLUID_INPUT));
    public static final RegistryObject<PortBlock> FLUID_OUTPUT_PORT =
            registerBlock("fluid_output_port", () -> new PortBlock(props(), PortBlock.PortType.FLUID_OUTPUT));

    public static final RegistryObject<BlockEntityType<PortBlockEntity>> PORT_BE =
            BLOCK_ENTITIES.register("port", () -> BlockEntityType.Builder.of(PortBlockEntity::new,
                    ENERGY_PORT.get(), ITEM_INPUT_PORT.get(), ITEM_OUTPUT_PORT.get(),
                    GAS_INPUT_PORT.get(), GAS_OUTPUT_PORT.get(),
                    FLUID_INPUT_PORT.get(), FLUID_OUTPUT_PORT.get()).build(null));

    public static final RegistryObject<BlockEntityType<ControllerBlockEntity>> CONTROLLER_BE =
            BLOCK_ENTITIES.register("controller", () -> BlockEntityType.Builder.of(ControllerBlockEntity::new,
                    CONTROLLERS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

    public static final RegistryObject<MenuType<ControllerMenu>> CONTROLLER_MENU =
            MENUS.register("controller", () -> IForgeMenuType.create(ControllerMenu::new));

    // --- chemical machines (titanium chain) ---
    // GregTech-style machine-specific casings + the EBF heating coil.
    public static final RegistryObject<Block> HEAT_PROOF_CASING =
            registerBlock("heat_proof_casing", () -> new Block(props()));
    public static final RegistryObject<Block> PTFE_CASING =
            registerBlock("ptfe_casing", () -> new Block(props()));
    public static final RegistryObject<Block> STAINLESS_CASING =
            registerBlock("stainless_casing", () -> new Block(props()));
    // special-steel machine casings (alloy blast furnace + vacuum freezer)
    public static final RegistryObject<Block> ALLOY_BLAST_CASING =
            registerBlock("alloy_blast_casing", () -> new Block(props()));
    public static final RegistryObject<Block> FROST_PROOF_CASING =
            registerBlock("frost_proof_casing", () -> new Block(props()));
    /** Heat vent ring block of the alloy blast furnace (GT's HEAT_VENT). */
    public static final RegistryObject<Block> HEAT_VENT =
            registerBlock("heat_vent", () -> new Block(props()));
    /** Circuit assembly line casing + its reinforced-glass side walls. */
    public static final RegistryObject<Block> ASSEMBLY_CASING =
            registerBlock("assembly_casing", () -> new Block(props()));
    public static final RegistryObject<Block> ASSEMBLY_GLASS =
            registerBlock("assembly_glass", () -> new net.minecraft.world.level.block.GlassBlock(
                    props().noOcclusion().sound(SoundType.GLASS)));
    // platinum-group line machine casings (large electrolyzer + large centrifuge)
    public static final RegistryObject<Block> ELECTROLYZER_CASING =
            registerBlock("electrolyzer_casing", () -> new Block(props()));
    public static final RegistryObject<Block> CENTRIFUGE_CASING =
            registerBlock("centrifuge_casing", () -> new Block(props()));
    // fusion reactor: casing ring, a permanently-glowing superconducting coil, glass window ring
    public static final RegistryObject<Block> FUSION_CASING =
            registerBlock("fusion_casing", () -> new Block(props()));
    public static final RegistryObject<Block> FUSION_COIL =
            registerBlock("fusion_coil", () -> new Block(props().lightLevel(state -> 10)));
    public static final RegistryObject<Block> FUSION_GLASS =
            registerBlock("fusion_glass", () -> new net.minecraft.world.level.block.GlassBlock(
                    props().noOcclusion().lightLevel(state -> 6).sound(SoundType.GLASS)));
    // artificial star generator: glowing star containment casing
    public static final RegistryObject<Block> STAR_CASING =
            registerBlock("star_casing", () -> new Block(props().lightLevel(state -> 7)));
    // black hole stabilizer: ultra-dense neutronium cage block + its face glazing
    public static final RegistryObject<Block> NEUTRONIUM_CASING =
            registerBlock("neutronium_casing", () -> new Block(props().strength(50.0F, 1200.0F)));
    public static final RegistryObject<Block> STABILIZER_GLASS =
            registerBlock("stabilizer_glass", () -> new net.minecraft.world.level.block.GlassBlock(
                    props().noOcclusion().lightLevel(state -> 4).sound(SoundType.GLASS)));
    // large hadron collider: accelerator tube casing + glowing collider magnet
    public static final RegistryObject<Block> ACCELERATOR_CASING =
            registerBlock("accelerator_casing", () -> new Block(props()));
    public static final RegistryObject<Block> COLLIDER_MAGNET =
            registerBlock("collider_magnet", () -> new Block(props().lightLevel(state -> 8)));
    // void ore miner: reinforced rig casing + the glowing void-drill mast block
    public static final RegistryObject<Block> VOID_MINER_CASING =
            registerBlock("void_miner_casing", () -> new Block(props()));
    public static final RegistryObject<Block> VOID_DRILL =
            registerBlock("void_drill", () -> new Block(props().lightLevel(state -> 11)));
    // oil drilling rig: steel rig casing + the drill-pipe string
    public static final RegistryObject<Block> OIL_RIG_CASING =
            registerBlock("oil_rig_casing", () -> new Block(props()));
    public static final RegistryObject<Block> DRILL_PIPE =
            registerBlock("drill_pipe", () -> new Block(props()));
    // large combustion generator: engine casing shell + gearbox ring block
    public static final RegistryObject<Block> ENGINE_CASING =
            registerBlock("engine_casing", () -> new Block(props()));
    public static final RegistryObject<Block> ENGINE_GEARBOX =
            registerBlock("engine_gearbox", () -> new Block(props()));
    // annihilation generator: glowing antimatter-containment sphere casing
    public static final RegistryObject<Block> ANNIHILATION_CASING =
            registerBlock("annihilation_casing", () -> new Block(props().lightLevel(state -> 9)));
    // AE2 integration: large inscriber (sky stone) + large charger (fluix) casings
    public static final RegistryObject<Block> INSCRIBER_CASING =
            registerBlock("inscriber_casing", () -> new Block(props()));
    public static final RegistryObject<Block> CHARGER_CASING =
            registerBlock("charger_casing", () -> new Block(props()));
    // Botania integration: mana pool / elven gate / terra plate machine casings
    public static final RegistryObject<Block> LIVINGROCK_CASING =
            registerBlock("livingrock_casing", () -> new Block(props()));
    // assembly line (GTCEu style) + research station casings
    public static final RegistryObject<Block> ASSLINE_CASING =
            registerBlock("assline_casing", () -> new Block(props()));
    public static final RegistryObject<Block> ASSLINE_GRATE =
            registerBlock("assline_grate", () -> new Block(props()));
    public static final RegistryObject<Block> ASSLINE_CONVEYOR =
            registerBlock("assline_conveyor", () -> new Block(props()));
    public static final RegistryObject<Block> RESEARCH_CASING =
            registerBlock("research_casing", () -> new Block(props()));
    // Ars Nouveau integration: grand imbuement chamber casing
    public static final RegistryObject<Block> SOURCESTONE_CASING =
            registerBlock("sourcestone_casing", () -> new Block(props().lightLevel(state -> 5)));
    /** Matter replicator shell: holds a pattern's field while a copy condenses. */
    public static final RegistryObject<Block> REPLICATOR_CASING =
            registerBlock("replicator_casing", () -> new Block(props().lightLevel(state -> 6)));

    /** Mana hatch: buffers mana for the Botania machines; accepts a spark on top. */
    public static final RegistryObject<com.falcon2235.moremultiblock.block.ManaHatchBlock> MANA_HATCH =
            registerBlock("mana_hatch", () -> new com.falcon2235.moremultiblock.block.ManaHatchBlock(props()));

    public static final RegistryObject<BlockEntityType<?>> MANA_HATCH_BE =
            BLOCK_ENTITIES.register("mana_hatch", () -> BlockEntityType.Builder.of(
                    com.falcon2235.moremultiblock.blockentity.ManaHatchSupport::create,
                    MANA_HATCH.get()).build(null));
    public static final RegistryObject<Block> ELVEN_GATE_CASING =
            registerBlock("elven_gate_casing", () -> new Block(props().lightLevel(state -> 7)));
    public static final RegistryObject<Block> TERRA_PLATE_CASING =
            registerBlock("terra_plate_casing", () -> new Block(props()));

    // quantum conduits: the transmitter tier above Mekanism's ultimate cables/pipes
    public static final RegistryObject<com.falcon2235.moremultiblock.block.ConduitBlock> QUANTUM_CABLE =
            registerBlock("quantum_cable", () -> new com.falcon2235.moremultiblock.block.ConduitBlock(
                    conduitProps(), com.falcon2235.moremultiblock.block.ConduitBlock.Type.ENERGY));
    public static final RegistryObject<com.falcon2235.moremultiblock.block.ConduitBlock> QUANTUM_FLUID_PIPE =
            registerBlock("quantum_fluid_pipe", () -> new com.falcon2235.moremultiblock.block.ConduitBlock(
                    conduitProps(), com.falcon2235.moremultiblock.block.ConduitBlock.Type.FLUID));
    public static final RegistryObject<com.falcon2235.moremultiblock.block.ConduitBlock> QUANTUM_GAS_TUBE =
            registerBlock("quantum_gas_tube", () -> new com.falcon2235.moremultiblock.block.ConduitBlock(
                    conduitProps(), com.falcon2235.moremultiblock.block.ConduitBlock.Type.GAS));
    public static final RegistryObject<com.falcon2235.moremultiblock.block.ConduitBlock> QUANTUM_ITEM_PIPE =
            registerBlock("quantum_item_pipe", () -> new com.falcon2235.moremultiblock.block.ConduitBlock(
                    conduitProps(), com.falcon2235.moremultiblock.block.ConduitBlock.Type.ITEM));

    public static final RegistryObject<BlockEntityType<com.falcon2235.moremultiblock.blockentity.ConduitBlockEntity>> CONDUIT_BE =
            BLOCK_ENTITIES.register("conduit", () -> BlockEntityType.Builder.of(
                    com.falcon2235.moremultiblock.blockentity.ConduitBlockEntity::new,
                    QUANTUM_CABLE.get(), QUANTUM_FLUID_PIPE.get(), QUANTUM_GAS_TUBE.get(), QUANTUM_ITEM_PIPE.get()).build(null));
    public static final RegistryObject<Block> COPPER_COIL =
            registerBlock("copper_coil", () -> new CoilBlock(props()));
    public static final RegistryObject<Block> CUPRONICKEL_COIL =
            registerBlock("cupronickel_coil", () -> new CoilBlock(props()));
    public static final RegistryObject<Block> TITANIUM_COIL =
            registerBlock("titanium_coil", () -> new CoilBlock(props()));
    public static final RegistryObject<Block> PLUTONIUM_COIL =
            registerBlock("plutonium_coil", () -> new CoilBlock(props()));
    public static final RegistryObject<Block> ANTIMATTER_COIL =
            registerBlock("antimatter_coil", () -> new CoilBlock(props()));
    /** End-game coil, wound from graviton alloy; the tier above antimatter. */
    public static final RegistryObject<Block> GRAVITON_COIL =
            registerBlock("graviton_coil", () -> new CoilBlock(props().lightLevel(state -> 6)));

    /**
     * Heating coils ordered by tier: copper(0) → cupronickel(1) → titanium(2) →
     * plutonium(3) → antimatter(4) → graviton(5).
     */
    public static final List<RegistryObject<Block>> COIL_TIERS =
            List.of(COPPER_COIL, CUPRONICKEL_COIL, TITANIUM_COIL, PLUTONIUM_COIL, ANTIMATTER_COIL, GRAVITON_COIL);

    /** Tier index of the given coil block state, or -1 when it is not a coil. */
    public static int coilTierOf(net.minecraft.world.level.block.state.BlockState state) {
        for (int i = 0; i < COIL_TIERS.size(); i++) {
            if (state.is(COIL_TIERS.get(i).get())) {
                return i;
            }
        }
        return -1;
    }

    public static final Map<ChemMachineType, RegistryObject<ChemMachineBlock>> CHEM_CONTROLLERS = makeChemControllers();

    public static final RegistryObject<BlockEntityType<ChemMachineBlockEntity>> CHEM_MACHINE_BE =
            BLOCK_ENTITIES.register("chem_machine", () -> BlockEntityType.Builder.of(ChemMachineBlockEntity::new,
                    CHEM_CONTROLLERS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

    public static final RegistryObject<MenuType<ChemMachineMenu>> CHEM_MACHINE_MENU =
            MENUS.register("chem_machine", () -> IForgeMenuType.create(ChemMachineMenu::new));

    public static final RegistryObject<MenuType<PortMenu>> PORT_MENU =
            MENUS.register("port", () -> IForgeMenuType.create(PortMenu::new));

    // --- primitive blast furnace (unpowered, brick-built, coal-fired) ---
    public static final RegistryObject<PbfBlock> PBF_CONTROLLER =
            registerBlock("primitive_blast_furnace_controller", () -> new PbfBlock(props()));

    public static final RegistryObject<BlockEntityType<PbfBlockEntity>> PBF_BE =
            BLOCK_ENTITIES.register("primitive_blast_furnace", () -> BlockEntityType.Builder.of(PbfBlockEntity::new,
                    PBF_CONTROLLER.get()).build(null));

    public static final RegistryObject<MenuType<PbfMenu>> PBF_MENU =
            MENUS.register("primitive_blast_furnace", () -> IForgeMenuType.create(PbfMenu::new));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MekanismMoreMultiblock.MODID))
            .icon(() -> new ItemStack(CONTROLLERS.get(MachineType.ENRICHING).get()))
            .displayItems((params, output) -> ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());

    // --- materials: titanium production chain + magnesium ---
    public static final RegistryObject<Item> RAW_TITANIUM = registerItem("raw_titanium");
    public static final RegistryObject<Item> TITANIUM_OXIDE = registerItem("titanium_oxide");
    public static final RegistryObject<Item> TITANIUM_SPONGE = registerItem("titanium_sponge");
    public static final RegistryObject<Item> TITANIUM_INGOT = registerItem("titanium_ingot");
    public static final RegistryObject<Item> TITANIUM_DUST = registerItem("titanium_dust");
    public static final RegistryObject<Item> RAW_MAGNESIUM = registerItem("raw_magnesium");
    public static final RegistryObject<Item> MAGNESIUM_DUST = registerItem("magnesium_dust");

    // nickel + cupronickel chain
    public static final RegistryObject<Item> RAW_NICKEL = registerItem("raw_nickel");
    public static final RegistryObject<Item> FINE_NICKEL_POWDER = registerItem("fine_nickel_powder");
    public static final RegistryObject<Item> NICKEL_DUST = registerItem("nickel_dust");
    public static final RegistryObject<Item> NICKEL_INGOT = registerItem("nickel_ingot");
    public static final RegistryObject<Item> CUPRONICKEL_DUST = registerItem("cupronickel_dust");
    public static final RegistryObject<Item> CUPRONICKEL_INGOT = registerItem("cupronickel_ingot");

    // chromium / aluminium / special steel chain
    public static final RegistryObject<Item> RAW_CHROMIUM = registerItem("raw_chromium");
    public static final RegistryObject<Item> ENRICHED_CHROMIUM_ORE = registerItem("enriched_chromium_ore");
    public static final RegistryObject<Item> SODIUM_CARBONATE = registerItem("sodium_carbonate");
    public static final RegistryObject<Item> SODIUM_DICHROMATE = registerItem("sodium_dichromate");
    public static final RegistryObject<Item> SODIUM_DICHROMATE_CRYSTAL = registerItem("sodium_dichromate_crystal");
    public static final RegistryObject<Item> RAW_BAUXITE = registerItem("raw_bauxite");
    public static final RegistryObject<Item> ALUMINA = registerItem("alumina");
    public static final RegistryObject<Item> ALUMINUM_INGOT = registerItem("aluminum_ingot");
    public static final RegistryObject<Item> ALUMINUM_DUST = registerItem("aluminum_dust");
    public static final RegistryObject<Item> CHROMIUM_INGOT = registerItem("chromium_ingot");
    public static final RegistryObject<Item> CHROMIUM_DUST = registerItem("chromium_dust");
    public static final RegistryObject<Item> SPECIAL_STEEL_DUST = registerItem("special_steel_dust");
    public static final RegistryObject<Item> SPECIAL_STEEL_INGOT = registerItem("special_steel_ingot");

    // zinc: the missing ingredient of the 7075 aluminium alloy below
    public static final RegistryObject<Item> RAW_ZINC = registerItem("raw_zinc");
    public static final RegistryObject<Item> ZINC_DUST = registerItem("zinc_dust");
    public static final RegistryObject<Item> ZINC_INGOT = registerItem("zinc_ingot");

    /**
     * Extra super duralumin (7075): aluminium alloyed with zinc, magnesium and copper.
     * A light, very strong structural alloy — the frame material for the mid-game
     * machine casings and their moving parts.
     */
    public static final RegistryObject<Item> DURALUMIN_DUST = registerItem("extra_super_duralumin_dust");
    public static final RegistryObject<Item> DURALUMIN_INGOT = registerItem("extra_super_duralumin_ingot");

    // super alloy chain (beyond atomic alloy): molten in the alloy blast furnace, frozen solid in the vacuum freezer
    public static final RegistryObject<Item> SUPER_ALLOY_DUST = registerItem("super_alloy_dust");
    public static final RegistryObject<Item> SUPER_ALLOY_INGOT = registerItem("super_alloy_ingot");

    /** Top-tier circuit built in the circuit assembler, above Mekanism's ultimate control circuit. */
    public static final RegistryObject<Item> SUPREME_CONTROL_CIRCUIT = registerItem("supreme_control_circuit");

    // --- platinum-group line (GTCEu platline) ---
    public static final RegistryObject<Item> SALTPETER = registerItem("saltpeter");
    public static final RegistryObject<Item> RAW_COOPERITE = registerItem("raw_cooperite");
    public static final RegistryObject<Item> PLATINUM_GROUP_SLUDGE = registerItem("platinum_group_sludge");
    public static final RegistryObject<Item> PLATINUM_RAW = registerItem("platinum_raw");
    public static final RegistryObject<Item> PALLADIUM_RAW = registerItem("palladium_raw");
    public static final RegistryObject<Item> INERT_METAL_MIXTURE = registerItem("inert_metal_mixture");
    public static final RegistryObject<Item> RAREST_METAL_MIXTURE = registerItem("rarest_metal_mixture");
    public static final RegistryObject<Item> RUTHENIUM_TETROXIDE = registerItem("ruthenium_tetroxide");
    public static final RegistryObject<Item> OSMIUM_TETROXIDE = registerItem("osmium_tetroxide");
    public static final RegistryObject<Item> IRIDIUM_METAL_RESIDUE = registerItem("iridium_metal_residue");
    public static final RegistryObject<Item> IRIDIUM_CHLORIDE = registerItem("iridium_chloride");
    public static final RegistryObject<Item> AMMONIUM_CHLORIDE = registerItem("ammonium_chloride");
    public static final RegistryObject<Item> PLATINUM_DUST = registerItem("platinum_dust");
    public static final RegistryObject<Item> PLATINUM_INGOT = registerItem("platinum_ingot");
    public static final RegistryObject<Item> PALLADIUM_DUST = registerItem("palladium_dust");
    public static final RegistryObject<Item> PALLADIUM_INGOT = registerItem("palladium_ingot");
    public static final RegistryObject<Item> RHODIUM_DUST = registerItem("rhodium_dust");
    public static final RegistryObject<Item> RHODIUM_INGOT = registerItem("rhodium_ingot");
    public static final RegistryObject<Item> RUTHENIUM_DUST = registerItem("ruthenium_dust");
    public static final RegistryObject<Item> RUTHENIUM_INGOT = registerItem("ruthenium_ingot");
    public static final RegistryObject<Item> IRIDIUM_DUST = registerItem("iridium_dust");
    public static final RegistryObject<Item> IRIDIUM_INGOT = registerItem("iridium_ingot");
    /** Osmiridium alloy dust (mixer: osmium + iridium); a naquadah-alloy component. */
    public static final RegistryObject<Item> OSMIRIDIUM_DUST = registerItem("osmiridium_dust");

    // --- naquadah line (GTCEu naquadah processing) ---
    public static final RegistryObject<Item> RAW_ANTIMONY = registerItem("raw_antimony");
    public static final RegistryObject<Item> ANTIMONY_DUST = registerItem("antimony_dust");
    public static final RegistryObject<Item> ANTIMONY_TRIOXIDE = registerItem("antimony_trioxide");
    public static final RegistryObject<Item> ANTIMONY_TRIFLUORIDE = registerItem("antimony_trifluoride");
    public static final RegistryObject<Item> RAW_NAQUADAH = registerItem("raw_naquadah");
    public static final RegistryObject<Item> NAQUADAH_DUST = registerItem("naquadah_dust");
    public static final RegistryObject<Item> ENRICHED_NAQUADAH_SULFATE = registerItem("enriched_naquadah_sulfate");
    public static final RegistryObject<Item> NAQUADRIA_SULFATE = registerItem("naquadria_sulfate");
    public static final RegistryObject<Item> TRINIUM_SULFIDE = registerItem("trinium_sulfide");
    public static final RegistryObject<Item> NAQUADAH_ENRICHED_INGOT = registerItem("naquadah_enriched_ingot");
    public static final RegistryObject<Item> NAQUADRIA_INGOT = registerItem("naquadria_ingot");
    public static final RegistryObject<Item> TRINIUM_INGOT = registerItem("trinium_ingot");
    public static final RegistryObject<Item> NAQUADAH_ALLOY_INGOT = registerItem("naquadah_alloy_ingot");

    /** Fusion reactor pinnacle output: molten stellar matter frozen into a solid core. */
    public static final RegistryObject<Item> STELLAR_CORE = registerItem("stellar_core");
    /** Superconducting coil (GregTech-style): crafts the fusion coil block and the fusion casing. */
    public static final RegistryObject<Item> SUPERCONDUCTOR = registerItem("superconductor");
    /** Assembler output that must be charged (AE2 charger / large charger) into a superconductor. */
    public static final RegistryObject<Item> UNCHARGED_SUPERCONDUCTOR = registerItem("uncharged_superconductor");
    /** Blank research medium consumed by the research station. */
    public static final RegistryObject<Item> DATA_ORB = registerItem("data_orb");

    // --- end-game intermediates (the deep neutronium / stellar / singularity chains) ---
    /** Centrifuged out of degenerate matter; four of them press into molten neutronium. */
    public static final RegistryObject<Item> NEUTRON_RICH_MASS = registerItem("neutron_rich_mass");
    /** Burnt-out stellar residue. Recycled back into molten stellar matter — the stellar loop. */
    public static final RegistryObject<Item> STELLAR_ASH = registerItem("stellar_ash");
    /** Shed by a collapsing black hole seed; dissolved in primordial matter into trans-dim metal. */
    public static final RegistryObject<Item> SINGULARITY_FRAGMENT = registerItem("singularity_fragment");

    // --- matter replication line ---
    /** Left over by every replication; the line dissolves it back into feedstock. */
    public static final RegistryObject<Item> EXOTIC_RESIDUE = registerItem("exotic_residue");
    /** Blank lattice that a dissolved rare item's signature is imprinted onto. */
    public static final RegistryObject<Item> BLANK_MATTER_PATTERN = registerItem("blank_matter_pattern");
    // Imprinted patterns: installed in the replicator's module slot, never consumed.
    public static final RegistryObject<Item> PATTERN_NETHER_STAR = registerItem("matter_pattern_nether_star");
    public static final RegistryObject<Item> PATTERN_DRAGON_EGG = registerItem("matter_pattern_dragon_egg");
    public static final RegistryObject<Item> PATTERN_CHAOS_SHARD = registerItem("matter_pattern_chaos_shard");
    public static final RegistryObject<Item> PATTERN_GAIA_SPIRIT = registerItem("matter_pattern_gaia_spirit");
    // research-data modules: install in the assembly line's module slot to unlock recipes
    public static final RegistryObject<Item> RESEARCH_DATA_SUPERCONDUCTOR = registerItem("research_data_superconductor");
    public static final RegistryObject<Item> RESEARCH_DATA_FUSION = registerItem("research_data_fusion");
    public static final RegistryObject<Item> RESEARCH_DATA_VOID_MINING = registerItem("research_data_void_mining");
    public static final RegistryObject<Item> RESEARCH_DATA_TRANSDIMENSIONAL = registerItem("research_data_transdimensional");
    /** Cryogenics: the vacuum freezer and the cold end of the metallurgy line. */
    public static final RegistryObject<Item> RESEARCH_DATA_CRYOGENICS = registerItem("research_data_cryogenics");
    /** Metallurgy: the alloy blast furnace. */
    public static final RegistryObject<Item> RESEARCH_DATA_METALLURGY = registerItem("research_data_metallurgy");
    /** Petrochemistry: the oil rig and the combustion generator. */
    public static final RegistryObject<Item> RESEARCH_DATA_PETROCHEMISTRY = registerItem("research_data_petrochemistry");
    /** Particle physics: the collider, the star generator and the black hole stabilizer. */
    public static final RegistryObject<Item> RESEARCH_DATA_PARTICLE = registerItem("research_data_particle");
    /** Antimatter engineering: the annihilation generator. */
    public static final RegistryObject<Item> RESEARCH_DATA_ANTIMATTER = registerItem("research_data_antimatter");
    /** Matter replication: the replicator itself. */
    public static final RegistryObject<Item> RESEARCH_DATA_REPLICATION = registerItem("research_data_replication");
    /** Digital logic: the AE2 parallel machines and this mod's parallel processors. */
    public static final RegistryObject<Item> RESEARCH_DATA_DIGITAL = registerItem("research_data_digital");
    /** Arcane engineering: the Botania / Ars Nouveau parallel machines. */
    public static final RegistryObject<Item> RESEARCH_DATA_ARCANE = registerItem("research_data_arcane");
    /** Black hole seed: the artificial star generator's pinnacle product. */
    public static final RegistryObject<Item> BLACK_HOLE_SEED = registerItem("black_hole_seed");
    /** Reactor module (antimatter-forged) that unlocks the alternative polonium synthesis recipe. */
    public static final RegistryObject<Item> POLONIUM_SYNTHESIS_UPGRADE = registerItem("polonium_synthesis_upgrade");
    /** Neutronium: fused in the fusion reactor (GT: naquadria fusion); the stabilizer's material. */
    public static final RegistryObject<Item> NEUTRONIUM = registerItem("neutronium");
    /**
     * Graviton alloy: neutronium compressed with stellar matter and superconductor
     * windings in the alloy blast furnace. Withstands the heat the antimatter coil
     * cannot, and is what the tier-5 graviton coil is wound from.
     */
    public static final RegistryObject<Item> GRAVITON_ALLOY = registerItem("graviton_alloy_ingot");
    /** Trans-dimensional metal: the ultimate creative-tier material from the black hole stabilizer. */
    public static final RegistryObject<Item> TRANSDIMENSIONAL_METAL = registerItem("transdimensional_metal");
    /** Trans-dimensional alloy: metal alloyed in the alloy blast furnace (frozen from its molten form). */
    public static final RegistryObject<Item> TRANSDIMENSIONAL_ALLOY = registerItem("transdimensional_alloy");
    /** Trans-dimensional circuit: assembled from the alloy; feeds the creative energy cube craft. */
    public static final RegistryObject<Item> TRANSDIMENSIONAL_CIRCUIT = registerItem("transdimensional_circuit");
    /** Construction terminal: auto-builds a multiblock's structure when used on its controller. */
    public static final RegistryObject<Item> CONSTRUCTION_TERMINAL =
            ITEMS.register("construction_terminal",
                    () -> new com.falcon2235.moremultiblock.item.ConstructionTerminalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> TITANIUM_ORE =
            registerBlock("titanium_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_TITANIUM_ORE =
            registerBlock("deepslate_titanium_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> MAGNESIUM_ORE =
            registerBlock("magnesium_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_MAGNESIUM_ORE =
            registerBlock("deepslate_magnesium_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> NICKEL_ORE =
            registerBlock("nickel_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_NICKEL_ORE =
            registerBlock("deepslate_nickel_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> CHROMIUM_ORE =
            registerBlock("chromium_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_CHROMIUM_ORE =
            registerBlock("deepslate_chromium_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> BAUXITE_ORE =
            registerBlock("bauxite_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_BAUXITE_ORE =
            registerBlock("deepslate_bauxite_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> ZINC_ORE =
            registerBlock("zinc_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_ZINC_ORE =
            registerBlock("deepslate_zinc_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> COOPERITE_ORE =
            registerBlock("cooperite_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_COOPERITE_ORE =
            registerBlock("deepslate_cooperite_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> SALTPETER_ORE =
            registerBlock("saltpeter_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_SALTPETER_ORE =
            registerBlock("deepslate_saltpeter_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> ANTIMONY_ORE =
            registerBlock("antimony_ore", () -> new Block(oreProps(3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_ANTIMONY_ORE =
            registerBlock("deepslate_antimony_ore", () -> new Block(oreProps(4.5F)));
    public static final RegistryObject<Block> NAQUADAH_ORE =
            registerBlock("naquadah_ore", () -> new Block(oreProps(5.0F)));
    public static final RegistryObject<Block> DEEPSLATE_NAQUADAH_ORE =
            registerBlock("deepslate_naquadah_ore", () -> new Block(oreProps(6.0F)));

    private MMMRegistry() {
    }

    private static RegistryObject<Item> registerItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static BlockBehaviour.Properties oreProps(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(hardness, 3.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F, 16.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties conduitProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 8.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static <B extends Block> RegistryObject<B> registerBlock(String name, Supplier<B> supplier) {
        RegistryObject<B> block = BLOCKS.register(name, supplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static List<RegistryObject<ParallelProcessorBlock>> makeParallelBlocks() {
        List<RegistryObject<ParallelProcessorBlock>> list = new ArrayList<>();
        for (int tier : PARALLEL_TIERS) {
            list.add(registerBlock("parallel_processor_" + tier, () -> new ParallelProcessorBlock(props(), tier)));
        }
        return List.copyOf(list);
    }

    private static Map<MachineType, RegistryObject<ControllerBlock>> makeControllers() {
        Map<MachineType, RegistryObject<ControllerBlock>> map = new EnumMap<>(MachineType.class);
        for (MachineType type : MachineType.values()) {
            map.put(type, registerBlock(type.id() + "_controller", () -> new ControllerBlock(props(), type)));
        }
        return Collections.unmodifiableMap(map);
    }

    /** The wall casing each chemical machine's structure is built from. */
    public static Block chemCasing(ChemMachineType type) {
        return switch (type) {
            case BLAST_FURNACE -> HEAT_PROOF_CASING.get();
            case REACTOR -> PTFE_CASING.get();
            case DISTILLATION, MIXER -> STAINLESS_CASING.get();
            case ALLOY_BLAST_FURNACE -> ALLOY_BLAST_CASING.get();
            case VACUUM_FREEZER -> FROST_PROOF_CASING.get();
            case CIRCUIT_ASSEMBLER -> ASSEMBLY_CASING.get();
            case ELECTROLYZER -> ELECTROLYZER_CASING.get();
            case CENTRIFUGE -> CENTRIFUGE_CASING.get();
            case FUSION_REACTOR -> FUSION_CASING.get();
            case STAR_GENERATOR -> STAR_CASING.get();
            case STABILIZER -> NEUTRONIUM_CASING.get();
            case HADRON_COLLIDER -> ACCELERATOR_CASING.get();
            case VOID_MINER -> VOID_MINER_CASING.get();
            case OIL_RIG -> OIL_RIG_CASING.get();
            case COMBUSTION_GENERATOR -> ENGINE_CASING.get();
            case ANNIHILATION_GENERATOR -> ANNIHILATION_CASING.get();
            case LARGE_INSCRIBER -> INSCRIBER_CASING.get();
            case LARGE_CHARGER -> CHARGER_CASING.get();
            case GRAND_MANA_POOL -> LIVINGROCK_CASING.get();
            case GRAND_ELVEN_GATE -> ELVEN_GATE_CASING.get();
            case GRAND_TERRA_PLATE -> TERRA_PLATE_CASING.get();
            case RESEARCH_STATION -> RESEARCH_CASING.get();
            case ASSEMBLY_LINE -> ASSLINE_CASING.get();
            case GRAND_IMBUEMENT -> SOURCESTONE_CASING.get();
            case MATTER_REPLICATOR -> REPLICATOR_CASING.get();
        };
    }

    /** A representative coil block for the machine's rings/preview, or null when it has none. */
    public static Block chemCoil(ChemMachineType type) {
        return switch (type) {
            case BLAST_FURNACE -> CUPRONICKEL_COIL.get();
            case ALLOY_BLAST_FURNACE -> TITANIUM_COIL.get();
            // the generator's box validator enforces full gearbox rings in the middle slices
            case COMBUSTION_GENERATOR -> ENGINE_GEARBOX.get();
            default -> null;
        };
    }

    private static Map<ChemMachineType, RegistryObject<ChemMachineBlock>> makeChemControllers() {
        Map<ChemMachineType, RegistryObject<ChemMachineBlock>> map = new EnumMap<>(ChemMachineType.class);
        for (ChemMachineType type : ChemMachineType.values()) {
            map.put(type, registerBlock(type.id + "_controller", () -> new ChemMachineBlock(props(), type)));
        }
        return Collections.unmodifiableMap(map);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }
}
