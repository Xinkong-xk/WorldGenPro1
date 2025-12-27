package io.xk.worldgenpro.addon.semantic.geo;

import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import io.xk.worldgenpro.addon.semantic.data.SectionHit;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.JigsawStructure;

import java.util.ArrayList;
import java.util.List;

public class GeoExprEvaluator implements GeoExpr.Visitor<List<SectionHit>> {

    private final MinecraftServer server;
    private final GeoContext context;
    private final StructureSet structureSet;
    private final JigsawStructure structure;

    // ⭐ 新增：所有需要转传给 predicate 的参数
    private final DynamicRegistryManager registryManager;
    private final StructurePlacementCalculator placementCalculator;
    private final StructureAccessor structureAccessor;
    private final StructureTemplateManager structureTemplateManager;
    private final ChunkGenerator generator;
    private final ServerWorld world;
    private final GeoSemanticConfig geoSemanticConfig;
    private final StructurePlacement structurePlacement;
    private final List<StructureSet.WeightedEntry> list;
    private final ChunkPos chunkPos;
    private final ChunkSectionPos chunkSectionPos;
    private final NoiseConfig noiseConfig;

    public GeoExprEvaluator(
            MinecraftServer server,
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
            NoiseConfig noiseConfig
    ) {
        this.server = server;
        this.context = context;
        this.structureSet = structureSet;
        this.structure = structure;
        this.registryManager = registryManager;
        this.placementCalculator = placementCalculator;
        this.structureAccessor = structureAccessor;
        this.structureTemplateManager = structureTemplateManager;
        this.generator = generator;
        this.world = world;
        this.geoSemanticConfig = geoSemanticConfig;
        this.structurePlacement = structurePlacement;
        this.list = list;
        this.chunkPos = chunkPos;
        this.chunkSectionPos = chunkSectionPos;
        this.noiseConfig = noiseConfig;
    }

    public List<SectionHit> evaluate(GeoExpr expr) {
        return expr.accept(this);
    }

    @Override
    public List<SectionHit> visitIsNotBiome(GeoExpr.IsNotBiomeGeoExpr e) {
        if (e.biomeGeoExpr.biomes == null) {
            // 寻找群系
            e.biomeGeoExpr.loadBiomes(server);
        }

        GeoPredicate predicate = GeoPredicates.get("is_not");

        return predicate.apply(
                e.biomeGeoExpr.biomes,
                context,
                structureSet,
                structure,
                registryManager,
                placementCalculator,
                structureAccessor,
                structureTemplateManager,
                generator,
                world,
                geoSemanticConfig,
                structurePlacement,
                list,
                chunkPos,
                chunkSectionPos,
                noiseConfig,
                e.target,
                e.target.accept(this)
        );
    }

    @Override
    public List<SectionHit> visitBinary(GeoExpr.BinaryGeoExpr e) {
        List<SectionHit> left = e.left.accept(this);
        List<SectionHit> right = e.right.accept(this);

        return switch (e.op) {
            case "AND" -> getAND(left, right);
            case "OR" -> getOR(left, right);
            default -> throw new IllegalArgumentException("Unknown binary op: " + e.op);
        };
    }

    public List<SectionHit> getAND(List<SectionHit> left, List<SectionHit> right) {
        // 用 HashSet 做 membership test
        ObjectOpenHashSet<Object> setA = new ObjectOpenHashSet<>(left.size() * 2); // 提前扩容减少 rehash
        setA.addAll(left);
        // 遍历 B，取交集
        List<SectionHit> out = new ObjectArrayList<>();
        for (SectionHit x : right) {
            if (setA.contains(x)) {
                out.add(x);
            }
        }
        return out;
    }

    public List<SectionHit> getOR(List<SectionHit> left, List<SectionHit> right) {
        int capacity = left.size() + right.size();
        ObjectOpenHashSet<SectionHit> set = new ObjectOpenHashSet<>(capacity * 2); // 预分配避免 rehash
        set.addAll(left);
        set.addAll(right);
        return new ObjectArrayList<>(set);
    }

    @Override
    public List<SectionHit> visitIsBiome(GeoExpr.IsBiomeGeoExpr e) {
        if (e.biomeGeoExpr.biomes == null) {
            // 寻找群系
            e.biomeGeoExpr.loadBiomes(server);
        }

        GeoPredicate predicate = GeoPredicates.get("is");

        return predicate.apply(
                e.biomeGeoExpr.biomes,
                context,
                structureSet,
                structure,
                registryManager,
                placementCalculator,
                structureAccessor,
                structureTemplateManager,
                generator,
                world,
                geoSemanticConfig,
                structurePlacement,
                list,
                chunkPos,
                chunkSectionPos,
                noiseConfig,
                e.target,
                e.target.accept(this)
        );
    }

    @Override
    public List<SectionHit> visitPredicate(GeoExpr.PredicateCallGeoExpr e) {

        GeoPredicate predicate = GeoPredicates.get(e.name);

        List<Biome> combined = new ArrayList<>();

        for (GeoExpr arg : e.args) {
            if (arg instanceof GeoExpr.BiomeGeoExpr is) {
                if (is.biomes == null) is.loadBiomes(server);
                combined.addAll(is.biomes);
            }
            else {
                throw new IllegalArgumentException("Geo predicate argument must be biome-related: " + arg.getClass());
            }
        }

        return predicate.apply(
                combined,
                context,
                structureSet,
                structure,
                registryManager,
                placementCalculator,
                structureAccessor,
                structureTemplateManager,
                generator,
                world,
                geoSemanticConfig,
                structurePlacement,
                list,
                chunkPos,
                chunkSectionPos,
                noiseConfig,
                null,
                null
        );
    }

    @Override
    public List<SectionHit> visitBiome(GeoExpr.BiomeGeoExpr e) {
        return List.of();
    }
}
