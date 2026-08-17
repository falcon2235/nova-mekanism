package com.falcon2235.moremultiblock.worldgen;

import com.falcon2235.moremultiblock.MMMConfig;
import com.falcon2235.moremultiblock.MekanismMoreMultiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * A biome modifier that adds an ore feature only when its config switch is on, so a
 * modpack can turn off any of our ores that its other mods already provide.
 *
 * <p>Forge's built-in {@code forge:add_features} modifier has no way to consult a
 * config, hence this small custom type. The ore name matches the keys under
 * {@code integration.ore_generation} in the config.
 */
public record ConfigurableOreModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features,
                                      String oreName) implements BiomeModifier {

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MekanismMoreMultiblock.MODID);

    public static final RegistryObject<Codec<ConfigurableOreModifier>> CODEC =
            BIOME_MODIFIERS.register("configurable_ore", () -> RecordCodecBuilder.<ConfigurableOreModifier>mapCodec(instance ->
                    instance.group(
                            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableOreModifier::biomes),
                            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ConfigurableOreModifier::features),
                            Codec.STRING.fieldOf("ore").forGetter(ConfigurableOreModifier::oreName)
                    ).apply(instance, ConfigurableOreModifier::new)).codec());

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !MMMConfig.generateOre(oreName)) {
            return;
        }
        features.forEach(feature ->
                builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature));
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }

    public static void register(IEventBus bus) {
        BIOME_MODIFIERS.register(bus);
    }

    /** Registry key helper so the JSON files can be validated against a known registry. */
    public static net.minecraft.resources.ResourceKey<Codec<? extends BiomeModifier>> key() {
        return net.minecraft.resources.ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                new net.minecraft.resources.ResourceLocation(MekanismMoreMultiblock.MODID, "configurable_ore"));
    }

    /** Unused, kept so the Registries import documents which registry the features come from. */
    static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<PlacedFeature>> FEATURE_REGISTRY =
            Registries.PLACED_FEATURE;
}
