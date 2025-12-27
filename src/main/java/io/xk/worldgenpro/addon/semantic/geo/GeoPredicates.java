package io.xk.worldgenpro.addon.semantic.geo;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.semantic.utils.builtin.boundary.BoundaryIn;
import io.xk.worldgenpro.addon.semantic.utils.builtin.boundary.BoundaryOut;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class GeoPredicates {

    private static final Map<String, GeoPredicate> registry = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    public static void register(String name, GeoPredicate predicate) {
        registry.put(name.toLowerCase(), predicate);
    }

    public static GeoPredicate get(String name) {
        GeoPredicate p = registry.get(name.toLowerCase());
        if (p == null) throw new RuntimeException("Unknown predicate: " + name);
        return p;
    }

    static {
        // 注册内置地理谓词
        register("is", (biomes, context, structureSet, structure,
                        registryManager, placementCalculator, structureAccessor,
                        structureTemplateManager, generator, world,
                        geoSemanticConfig, structurePlacement, list, chunkPos,
                        chunkSectionPos, noiseConfig,inputGeoExpr,inputData) -> {
            // inputData 是 target 对应的 SectionHit 列表
            // biomes 是右侧指定的目标群系
            if (inputData == null) return List.of();

            // 过滤出位于指定群系内的 SectionHit
            return inputData.stream()
                    .filter(sh -> sh != null && biomes.contains(sh.biome))
                    .toList();
        });

        register("is_not", (biomes, context,  structureSet, structure,
                        registryManager, placementCalculator, structureAccessor,
                        structureTemplateManager, generator, world,
                        geoSemanticConfig, structurePlacement, list, chunkPos,
                        chunkSectionPos, noiseConfig,inputGeoExpr,inputData) -> {
            if (inputData == null) return List.of();

            // 过滤出不在指定群系中的 SectionHit
            return inputData.stream()
                    .filter(sh -> sh != null && !biomes.contains(sh.biome))
                    .toList();
        });

        register("boundary_out", (biomes, context,  structureSet, structure,
                             registryManager, placementCalculator, structureAccessor,
                             structureTemplateManager, generator, world,
                             geoSemanticConfig, structurePlacement, list, chunkPos,
                             chunkSectionPos, noiseConfig,inputGeoExpr,inputData) -> {
            // 创建边缘寻找器，使用修改后的重置版
            BoundaryOut edgeOfDetector = new BoundaryOut(
                    generator,
                    world,
                    noiseConfig,
                    structureSet,
                    structure,
                    WorldGenPro.geoSemanticStructureManager
            );
            return edgeOfDetector.findEdgeSections(
                    WorldGenPro.geoSemanticStructureManager.getAllowedGenerationChunks(structureSet,chunkPos,placementCalculator.getStructureSeed()),
                    new ObjectOpenHashSet<>(biomes),
                    chunkPos
            );
        });


        register("boundary_in", (biomes, context,  structureSet, structure,
                                  registryManager, placementCalculator, structureAccessor,
                                  structureTemplateManager, generator, world,
                                  geoSemanticConfig, structurePlacement, list, chunkPos,
                                  chunkSectionPos, noiseConfig,inputGeoExpr,inputData) -> {
            // 创建边缘寻找器，使用修改后的重置版
            BoundaryIn edgeOfDetector = new BoundaryIn(
                    generator,
                    world,
                    noiseConfig,
                    structureSet,
                    structure,
                    WorldGenPro.geoSemanticStructureManager
            );
            return edgeOfDetector.findEdgeSections(
                    WorldGenPro.geoSemanticStructureManager.getAllowedGenerationChunks(structureSet,chunkPos,placementCalculator.getStructureSeed()),
                    new ObjectOpenHashSet<>(biomes),
                    chunkPos
            );
        });


    }

    public static Map<String, GeoPredicate> getRegistry() {
        return Map.copyOf(registry);
    }
}
