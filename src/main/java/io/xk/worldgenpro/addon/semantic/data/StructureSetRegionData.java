package io.xk.worldgenpro.addon.semantic.data;

import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import io.xk.worldgenpro.addon.semantic.geo.GeoContext;
import io.xk.worldgenpro.addon.semantic.geo.GeoExpr;
import io.xk.worldgenpro.addon.semantic.geo.GeoExprEvaluator;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.JigsawStructure;

import java.util.List;

public class StructureSetRegionData {

    public GeoExpr geoExpr;
    public Long regionId;

    public int offsetX;
    public int offsetZ;
    public int startX;
    public int startZ;

    public DynamicRegistryManager registryManager;
    public StructurePlacementCalculator placementCalculator;
    public StructureAccessor structureAccessor;
    public StructureTemplateManager structureTemplateManager;
    public ChunkGenerator generator;
    public ServerWorld world;
    public StructureSet structureSet;
    public GeoSemanticConfig geoSemanticConfig;
    public StructurePlacement structurePlacement;
    public List<StructureSet.WeightedEntry> list;
    public ChunkPos chunkPos;
    public ChunkSectionPos chunkSectionPos;
    public NoiseConfig noiseConfig;
    public JigsawStructure jigsawStructure;
    public int regionX;
    public int regionZ;

    // 真正决定生成的地方
    public ChunkPos generateChunkPos = null;

    public StructureSetRegionData(DynamicRegistryManager registryManager,
                                  StructurePlacementCalculator placementCalculator,
                                  StructureAccessor structureAccessor,
                                  StructureTemplateManager structureTemplateManager,
                                  ChunkGenerator generator,
                                  ServerWorld world,
                                  StructureSet structureSet,
                                  GeoSemanticConfig geoSemanticConfig,
                                  GeoExpr geoExpr,
                                  StructurePlacement structurePlacement,
                                  List<StructureSet.WeightedEntry> list,
                                  ChunkPos chunkPos,
                                  ChunkSectionPos chunkSectionPos,
                                  NoiseConfig noiseConfig,
                                  Long regionId,
                                  JigsawStructure structure,
                                  int[] offsetsAndStarts,
                                  int regionX,
                                  int regionZ
    ) {
        this.geoExpr = geoExpr;
        this.regionId = regionId;

        this.offsetX = offsetsAndStarts[0];
        this.offsetZ = offsetsAndStarts[1];
        this.startX = offsetsAndStarts[2];
        this.startZ = offsetsAndStarts[3];

        this.registryManager = registryManager;
        this.placementCalculator = placementCalculator;
        this.structureAccessor = structureAccessor;
        this.structureTemplateManager = structureTemplateManager;
        this.generator = generator;
        this.world = world;
        this.structureSet = structureSet;
        this.geoSemanticConfig = geoSemanticConfig;
        this.chunkPos = chunkPos;
        this.chunkSectionPos = chunkSectionPos;
        this.noiseConfig = noiseConfig;
        this.list = list;
        this.structurePlacement  = structurePlacement;
        this.jigsawStructure = structure;
        this.regionX = regionX;
        this.regionZ = regionZ;

        // 初始化
        this.init();
    }

    public void init(){
        GeoExprEvaluator exprEvaluator = new GeoExprEvaluator(
                world.getServer(),
                new GeoContext(world,jigsawStructure),
                structureSet,
                jigsawStructure,
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
                noiseConfig
        );

        // 得到可以生成的section
        List<SectionHit> sectionHitList = exprEvaluator.evaluate(geoExpr);

        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(0L));
        chunkRandom.setRegionSeed(world.getSeed(), regionX, regionZ, structurePlacement.getSalt());

        // 从随机数里获取其中一个SectionHit作为生成坐标
        if (!sectionHitList.isEmpty()) {
            SectionHit generateSectionHit = sectionHitList.get(chunkRandom.nextInt(sectionHitList.size()));
            // System.out.println("GEN -> "+generateSectionHit);
            this.generateChunkPos = new ChunkPos(generateSectionHit.chunkX,generateSectionHit.chunkZ);
        }
    }

    public boolean isStartChunk(ChunkPos chunkPos, ServerWorld world) {
        if (this.generateChunkPos == null){
            return false;
        }
        return (chunkPos.x == this.generateChunkPos.x ) && (chunkPos.z == this.generateChunkPos.z);
    }
}
