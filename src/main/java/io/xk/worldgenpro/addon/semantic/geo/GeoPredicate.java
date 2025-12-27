package io.xk.worldgenpro.addon.semantic.geo;

import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import io.xk.worldgenpro.addon.semantic.data.SectionHit;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.JigsawStructure;

import java.util.List;

@FunctionalInterface
public interface GeoPredicate {
    List<SectionHit> apply(
            List<Biome> biomes,
            GeoContext context,
            StructureSet structureSet,
            JigsawStructure structure,
            DynamicRegistryManager registryManager,
            StructurePlacementCalculator placementCalculator,
            StructureAccessor structureAccessor,
            StructureTemplateManager structureTemplateManager,
            ChunkGenerator generator,
            ServerWorld world,
            GeoSemanticConfig geoSemanticConfig,
            StructurePlacement structurePlacement,
            List<StructureSet.WeightedEntry> list,
            ChunkPos chunkPos,
            ChunkSectionPos chunkSectionPos,
            NoiseConfig noiseConfig,
            GeoExpr inputGeoExpr,
            List<SectionHit> inputData
    );
}
