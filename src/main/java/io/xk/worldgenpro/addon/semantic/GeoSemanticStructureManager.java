package io.xk.worldgenpro.addon.semantic;

import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import io.xk.worldgenpro.addon.semantic.data.ChunkSamples;
import io.xk.worldgenpro.addon.semantic.data.LevelBiomes;
import io.xk.worldgenpro.addon.semantic.data.StructureSetRegionData;
import io.xk.worldgenpro.addon.semantic.geo.GeoExpr;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.JigsawStructureReflect;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructureSetReflect;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.*;

/**
 * GeoSemanticStructureManager (updated to support multi-sampleLevel LevelBiomes)
 */
public class GeoSemanticStructureManager {

    public static final Logger LOGGER = LoggerFactory.getLogger("worldgenpro");

    // 记录各结构集的结构区域生成数据
    // { StructureSet : { WorldKey : { JigsawStructure : { RegionID : StructureSetRegionData } } }  }
    public final ConcurrentMap<StructureSet,
            ConcurrentMap<RegistryKey<World>,
                    ConcurrentMap<JigsawStructure, ConcurrentMap<Long, StructureSetRegionData>>>> geoStructureDatas = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);


    // 记录各结构集的地理语义配置
    // { StructureSet : GeoSemanticConfig }
    public final ConcurrentMap<StructureSet,GeoSemanticConfig> geoSemanticConfigs = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    // 记录各采样精度与其相对应的采样等级
    // {SampleSize : SampleLevel}
    public static ConcurrentMap<Integer, SampleLevel> sampleSizeToLevel = new ConcurrentHashMap<>(SAMPLE_LEVEL_CAPACITY);
    static {
        sampleSizeToLevel.put(32, SampleLevel.LEVEL_0);
        sampleSizeToLevel.put(16, SampleLevel.LEVEL_1);
        sampleSizeToLevel.put(8, SampleLevel.LEVEL_2);
    }

    // y 为群系边界长度
    public static ConcurrentMap<RegistryKey<World>,ConcurrentMap<SampleLevel,LevelBiomes>> worldLevelBiomes = new ConcurrentHashMap<>(BASE_WORLD_CAPACITY);

    // 记录各维度各结构集的各结构在各region生成情况
    // { WorldKey : { StructureSet : { Structure : { RegionID : GenerateResult } } } }
    public static ConcurrentMap<RegistryKey<World>,ConcurrentMap<StructureSet,ConcurrentMap<JigsawStructure,ConcurrentMap<Long,GenerateResult>>>> generateResults = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    // 记录高度上下文
    public static Object2ObjectOpenHashMap<RegistryKey<World>,HeightContext> heightContextMap = new Object2ObjectOpenHashMap<>();

    // 记录所有地理语义结构
    public static ConcurrentHashMap<JigsawStructure,ConcurrentHashMap<StructurePlacement,StructureSet>> geoJigsawStructures = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);


    /**
     * 服务器关闭时
     * @param server
     */
    public void onServerStopping(MinecraftServer server) {
        LOGGER.info("[GeoSemanticStructure] Process Server Stopping...");
        // 清理
        geoStructureDatas.clear();
        geoSemanticConfigs.clear();
        worldLevelBiomes.clear();
        geoJigsawStructures.clear();
        heightContextMap.clear();
        // 保存生成结果
        generateResults.clear();
    }

    /**
     * 服务器启动时
     * @param server
     */
    public void onServerStarting(MinecraftServer server) {
        LOGGER.info("[GeoSemanticStructure] Loading...");
        // 遍历 registry 的 pool sets, 并注册需要的那些
        Registry<StructureSet> structureSetRegistry = server.getRegistryManager().get(RegistryKeys.STRUCTURE_SET);
        for (StructureSet structureSet : structureSetRegistry) { // 这里直接是 Iterable
            GeoSemanticConfig geoSemanticConfig = StructureSetReflect.getGeoSemanticConfig(structureSet);
            GeoExpr geoExpr = geoSemanticConfig.getGeoExpr();
            if (geoExpr != null) {
                registerStructureSet(structureSet,geoSemanticConfig,server);
            }
        }
    }

    public void registerStructureSet(StructureSet structureSet,GeoSemanticConfig geoSemanticConfig,MinecraftServer server){
        // 注册到地理语义配置表里
        geoSemanticConfigs.put(structureSet,geoSemanticConfig);

        // 注册到结构数据表里
        for (StructureSet.WeightedEntry weightedEntry : structureSet.structures()){
            StructurePlacement structurePlacement = structureSet.placement();

            // 记录进地理语义拼图结构对应关系:jigsawStructure -> placement:structureSet
            ConcurrentHashMap<StructurePlacement,StructureSet> placementStructureMap = geoJigsawStructures.computeIfAbsent(
                    (JigsawStructure) weightedEntry.structure().value(),
                    structure -> new ConcurrentHashMap<>(BASE_SMALL_CAPACITY));
            placementStructureMap.put(structurePlacement,structureSet);

            // 构建生物群系
            JigsawStructureReflect.buildBiomeInvoke((JigsawStructure) weightedEntry.structure().value(),server);
        }
    }

    public static long getRegionId(int chunkX, int chunkZ, int spacing) {
        int rx = Math.floorDiv(chunkX, spacing);
        int rz = Math.floorDiv(chunkZ, spacing);
        return (((long) rx) << 32) | (rz & 0xffffffffL);
    }

    public static long getRegionId(ChunkPos chunkPos, int spacing) {
        int rx = Math.floorDiv(chunkPos.x, spacing);
        int rz = Math.floorDiv(chunkPos.z, spacing);
        return (((long) rx) << 32) | (rz & 0xffffffffL);
    }

    public static int[] getRegionArea(ChunkPos  chunkPos, int spacing) {
        int rx = Math.floorDiv(chunkPos.x, spacing);
        int rz = Math.floorDiv(chunkPos.z, spacing);
        return new int[]{rx, rz};
    }

    public boolean checkChunk(StructureSet structureSet, ChunkPos chunkPos, long seed) {
        if (!(structureSet.placement() instanceof RandomSpreadStructurePlacement randomSpread)) {
            return false; // 不是 RandomSpread 类型
        }
        int[] offsetsAndStarts = getOffsetsAndStarts(structureSet,chunkPos,seed);
        int separation = randomSpread.getSeparation();
        // 判断给定 chunk 是否在 separation 区域内
        int dx = chunkPos.x - offsetsAndStarts[2];
        int dz = chunkPos.z - offsetsAndStarts[3];
        return dx >= 0 && dx < separation && dz >= 0 && dz < separation;
    }

    public int[] getOffsetsAndStarts(StructureSet structureSet, ChunkPos chunkPos, long seed){
        if (!(structureSet.placement() instanceof RandomSpreadStructurePlacement randomSpread)) {
            return new int[]{}; // 不是 RandomSpread 类型
        }
        int spacing = randomSpread.getSpacing();
        int separation = randomSpread.getSeparation();
        int regionX = Math.floorDiv(chunkPos.x, spacing);
        int regionZ = Math.floorDiv(chunkPos.z, spacing);
        // 计算该 region 的中心偏移
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(0L));
        chunkRandom.setRegionSeed(seed, regionX, regionZ, randomSpread.getSalt());
        int k = spacing - separation;
        int offsetX = randomSpread.getSpreadType().get(chunkRandom, k);
        int offsetZ = randomSpread.getSpreadType().get(chunkRandom, k);
        // 允许生成的起始 chunk
        int startX = regionX * spacing + offsetX;
        int startZ = regionZ * spacing + offsetZ;
        return new int[]{offsetX, offsetZ, startX, startZ};
    }

    /**
     * 返回当前 region 中“可生成结构”的 chunk 列表（即 startX..startX+separation-1, startZ..startZ+separation-1）
     *
     * @param structureSet 结构集合（需为 RandomSpreadStructurePlacement）
     * @param chunkPos  任意位于目标 region 的区块坐标（用于确定 region）
     * @param seed   世界/placement 种子（用于计算固定偏移）
     * @return 列表（可能为空），不为 null
     */
    public List<ChunkPos> getAllowedGenerationChunks(StructureSet structureSet, ChunkPos chunkPos, long seed) {
        if (!(structureSet.placement() instanceof RandomSpreadStructurePlacement randomSpread)) {
            return new ObjectArrayList<>();
        }
        // 通过已有的方法计算 offset/start
        int[] offsetsAndStarts = getOffsetsAndStarts(structureSet, chunkPos, seed);
        if (offsetsAndStarts == null || offsetsAndStarts.length < 4) {
            return new ObjectArrayList<>();
        }
        int startX = offsetsAndStarts[2];
        int startZ = offsetsAndStarts[3];
        int separation = randomSpread.getSeparation();
        int spacing = randomSpread.getSpacing();
        // 防护：确保 separation 合理（通常 separation <= spacing）
        if (separation <= 0) return new ObjectArrayList<>();
        if (separation > spacing) separation = Math.max(1, Math.min(separation, spacing));
        ObjectArrayList<ChunkPos> res = new ObjectArrayList<>(separation * separation);
        for (int dx = 0; dx < separation; dx++) {
            for (int dz = 0; dz < separation; dz++) {
                res.add(new ChunkPos(startX + dx, startZ + dz));
            }
        }
        return res;
    }

    public ChunkPos getStructureInCurrentRegion(DynamicRegistryManager registryManager,
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
                                                JigsawStructure jigsawStructure) {
        // 获取维度键
        RegistryKey<World> worldKey = world.getRegistryKey();
        if (!(structurePlacement instanceof RandomSpreadStructurePlacement randomSpread)) {
            LOGGER.error("[GeoSemanticStructure] pool placement is not a RandomSpreadStructurePlacement");
            return null;
        }

        // NOTE 1. 获取 spacing 并计算 regionID
        int spacing = randomSpread.getSpacing();
        long regionID = getRegionId(chunkPos, spacing);

        // NOTE 2. 顶层：以 structureSet 为 key 获取 worldMap
        ConcurrentMap<RegistryKey<World>, ConcurrentMap<JigsawStructure, ConcurrentMap<Long, StructureSetRegionData>>> worldMap =
                geoStructureDatas.computeIfAbsent(structureSet, ss -> new ConcurrentHashMap<>(BASE_MIDDLE_CAPACITY));

        // NOTE 3. 第二层：以 worldKey 为 key 获取 regionMap
        ConcurrentMap<JigsawStructure, ConcurrentMap<Long, StructureSetRegionData>> regionMap =
                worldMap.computeIfAbsent(worldKey, wk -> new ConcurrentHashMap<>(BASE_WORLD_CAPACITY));

        // NOTE 4. 查找 Structure结构，如果已有缓存，就检查regionData.isStartChunk
        ConcurrentMap<Long, StructureSetRegionData> regionDataMap = regionMap.get(jigsawStructure);

        // NOTE 如果就检查 regionData 存在则获取
        if (regionDataMap != null) {
            if (regionDataMap.containsKey(regionID)) {
                return regionDataMap.get(regionID).generateChunkPos;
            }
        }

        // NOTE 5. 创建新的 regionData（第一次检测此 region）
        int[] regionArea = getRegionArea(chunkPos, spacing);
        StructureSetRegionData regionData = new StructureSetRegionData(
                registryManager,
                placementCalculator,
                structureAccessor,
                structureTemplateManager,
                generator,
                world,
                structureSet,
                geoSemanticConfig,
                geoSemanticConfig.getGeoExpr(),
                structurePlacement,
                list,
                chunkPos,
                chunkSectionPos,
                noiseConfig,
                regionID,
                jigsawStructure,
                getOffsetsAndStarts(structureSet,chunkPos,placementCalculator.getStructureSeed()),
                regionArea[0],
                regionArea[1]
        );

        // NOTE 检查这个 regionData 是否是空的,如果是空的则意味着这个 region 无法生成,则也记录此结构在此 region 生成失败
        if (regionData.generateChunkPos == null){
            recordGenerateResult(worldKey,structureSet,jigsawStructure,chunkPos,structureAccessor,false);
        }

        // NOTE 如果区域数据表存在,则存入,否则创建再存入
        if (regionDataMap != null) {
            regionDataMap.put(regionID, regionData);
        }else{
            regionMap.put(jigsawStructure,new ConcurrentHashMap<>(BASE_SMALL_CAPACITY){{put(regionID, regionData);}});
        }

        // NOTE 返回这个区域数据检查此区块是否能够生成
        return regionData.generateChunkPos;
    }

    // 检查一个结构是否应该在这个区块生成
    public boolean checkStructure(DynamicRegistryManager registryManager,
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
                                  StructureSet.WeightedEntry structureEntry) {
        // 获取维度键
        RegistryKey<World> worldKey = world.getRegistryKey();

        if (!(structurePlacement instanceof RandomSpreadStructurePlacement randomSpread)) {
            LOGGER.error("[GeoSemanticStructure] pool placement is not a RandomSpreadStructurePlacement");
            return false;
        }

        if ( ! (structureEntry.structure().value() instanceof JigsawStructure jigsawStructure) ){
            LOGGER.error("[GeoSemanticStructure] pool type is not a JigsawStructure");
            return false;
        }else{
            jigsawStructure = (JigsawStructure) structureEntry.structure().value();
        }

        // 1. 获取 spacing 并计算 regionID
        int spacing = randomSpread.getSpacing();
        long regionID = getRegionId(chunkPos, spacing);

        // 2. 顶层：以 structureSet 为 key 获取 worldMap
        ConcurrentMap<RegistryKey<World>, ConcurrentMap<JigsawStructure, ConcurrentMap<Long, StructureSetRegionData>>> worldMap =
                geoStructureDatas.computeIfAbsent(structureSet, ss -> new ConcurrentHashMap<>(BASE_MIDDLE_CAPACITY));

        // 3. 第二层：以 worldKey 为 key 获取 regionMap
        ConcurrentMap<JigsawStructure, ConcurrentMap<Long, StructureSetRegionData>> regionMap =
                worldMap.computeIfAbsent(worldKey, wk -> new ConcurrentHashMap<>(BASE_WORLD_CAPACITY));

        // 4. 查找 Structure结构，如果已有缓存，就检查regionData.isStartChunk
        ConcurrentMap<Long, StructureSetRegionData> regionDataMap = regionMap.get(jigsawStructure);

        // 如果就检查regionData存在则获取
        if (regionDataMap != null) {
            if (regionDataMap.containsKey(regionID)) {
                return regionDataMap.get(regionID).isStartChunk(chunkPos, world);
            }
        }

        int[] regionArea = getRegionArea(chunkPos, spacing);

        // 5. 创建新的 regionData（第一次进入此 region）
        StructureSetRegionData regionData = new StructureSetRegionData(
                registryManager,
                placementCalculator,
                structureAccessor,
                structureTemplateManager,
                generator,
                world,
                structureSet,
                geoSemanticConfig,
                geoSemanticConfig.getGeoExpr(),
                structurePlacement,
                list,
                chunkPos,
                chunkSectionPos,
                noiseConfig,
                regionID,
                jigsawStructure,
                getOffsetsAndStarts(structureSet,chunkPos,placementCalculator.getStructureSeed()),
                regionArea[0],
                regionArea[1]
        );

        // 检查这个regionData是否是空的,如果是空的则意味着这个region无法生成,则也记录此结构在此region生成失败
        if (regionData.generateChunkPos == null){
            recordGenerateResult(worldKey,structureSet,jigsawStructure,chunkPos,structureAccessor,false);
        }

        // 如果区域数据表存在,则存入,否则创建再存入
        if (regionDataMap != null) {
            regionDataMap.put(regionID, regionData);
        }else{
            regionMap.put(jigsawStructure,new ConcurrentHashMap<>(BASE_SMALL_CAPACITY){{put(regionID, regionData);}});
        }

        // 返回这个区域数据检查此区块是否能够生成
        return regionData.isStartChunk(chunkPos, world);
    }

    /**
     * 记录生成结果
     * @param structureSet 结构集
     * @param structure 结构
     * @param chunkPos 区块坐标
     * @param structureAccessor 访问器
     * @param success 是否成功生成
     */
    @Unique
    public static void recordGenerateResult(RegistryKey<World> worldRegistryKey,StructureSet structureSet, Structure structure, ChunkPos chunkPos, StructureAccessor structureAccessor, boolean success) {
        // NOTE 获取拼图结构
        JigsawStructure jigsawStructure = (JigsawStructure) structure;

        // NOTE 如果生成结果里不包含此世界则创建新的表
        ConcurrentMap<StructureSet, ConcurrentMap<JigsawStructure, ConcurrentMap<Long, GenerateResult>>> worldMap =
                generateResults.computeIfAbsent(worldRegistryKey,wk-> new ConcurrentHashMap<>(BASE_SMALL_CAPACITY));

        // NOTE 如果此世界表里不包含此结构集则创建新的表
        ConcurrentMap<JigsawStructure, ConcurrentMap<Long, GenerateResult>> jigsawStructureMap =  worldMap.computeIfAbsent(structureSet,ss->new ConcurrentHashMap<>());

        // NOTE 如果此结构集表里不包含此拼图结构则创建新的表
        ConcurrentMap<Long, GenerateResult> resultMap = jigsawStructureMap.computeIfAbsent(jigsawStructure,j->new ConcurrentHashMap<>());

        // NOTE 计算regionID
        RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement) structureSet.placement();
        int spacing = randomSpreadStructurePlacement.getSpacing();
        long regionID = getRegionId(chunkPos, spacing);

        resultMap.put(regionID,
                new GenerateResult(success,structureSet,jigsawStructure,chunkPos));
    }



    /**
     * 返回 chunk 的 ChunkSamples（如果已存在缓存）
     */
    public ChunkSamples getChunkSamples(ServerWorld world,
                                        ChunkPos chunkPos,
                                        int sampleSize,
                                        MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler,
                                        ChunkGenerator generator
    ){
        RegistryKey<World> worldKey = world.getRegistryKey();

        ConcurrentMap<SampleLevel, LevelBiomes> sampleLevelMap =
                worldLevelBiomes.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>(BASE_SMALL_CAPACITY));

        // 获取分辨率采样等级
        SampleLevel sl = sampleSizeToLevel.get(sampleSize);

        // 获取群系采样层
        LevelBiomes levelBiomes = sampleLevelMap.get(sl);

        if (levelBiomes == null){
            levelBiomes = new LevelBiomes(sl,sampleSize);

            sampleLevelMap.put(sl, levelBiomes);
        }

        return levelBiomes.getChunkSamples(chunkPos.x, chunkPos.z,generator,multiNoiseSampler);
    }

    public int getBiomeY(int worldY){
        return worldY >> 5;
    }

    public static int computeStructureY_LogicOnly(
            HeightProvider startHeightProvider,
            Optional<Heightmap.Type> heightmapOpt,
            ChunkGenerator generator,
            int x,int z,ServerWorld world
    ) {
        HeightContext heightContext = heightContextMap.computeIfAbsent(world.getRegistryKey(),worldRegistryKey -> new HeightContext(generator,world));
        int i = startHeightProvider.get(world.random, heightContext);
        return heightmapOpt.map(type -> i + generator.getHeightOnGround(x, z, type, world, world.getChunkManager().getNoiseConfig())).orElseGet(() -> i);
    }
}
