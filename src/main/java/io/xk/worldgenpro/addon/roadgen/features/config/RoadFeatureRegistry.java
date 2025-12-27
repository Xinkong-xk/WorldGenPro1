package io.xk.worldgenpro.addon.roadgen.features.config;

import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.features.RoadFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoadFeatureRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadGen.MOD_ID);

    public static void registerFeatures() {
        LOGGER.info("[RoadGen] Registering road_feature");
        Registry.register(Registries.FEATURE, Identifier.of(RoadGen.MOD_ID, "road_feature"), RoadFeature.ROAD_FEATURE);
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Feature.LOCAL_MODIFICATIONS,
                RoadFeature.ROAD_FEATURE_PLACED_KEY
        );
    }
}
