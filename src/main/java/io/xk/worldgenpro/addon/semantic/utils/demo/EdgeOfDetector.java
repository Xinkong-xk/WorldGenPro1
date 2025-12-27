//package io.xk.worldgenpro.vanilla.enhance.semantic.utils.demo;
//
//import io.xk.worldgenpro.vanilla.enhance.semantic.GeoSemanticStructureManager;
//import io.xk.worldgenpro.vanilla.enhance.semantic.data.ChunkSamples;
//import io.xk.worldgenpro.vanilla.enhance.semantic.data.LevelBiomes;
//import io.xk.worldgenpro.vanilla.enhance.semantic.SampleLevel;
//import io.xk.worldgenpro.vanilla.enhance.semantic.data.SectionHit;
//import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
//import net.minecraft.server.world.ServerWorld;
//import net.minecraft.pool.StructureSet;
//import net.minecraft.util.math.ChunkPos;
//import net.minecraft.world.Heightmap;
//import net.minecraft.world.biome.Biome;
//import net.minecraft.world.biome.source.util.MultiNoiseUtil;
//import net.minecraft.world.gen.noise.NoiseConfig;
//import net.minecraft.world.gen.chunk.ChunkGenerator;
//import net.minecraft.world.gen.pool.JigsawStructure;
//
//import java.util.*;
//
///**
// * EdgeOfDetector (完全版)
// *
// * - 完全使用 GeoSemanticStructureManager#getBiome(...) 进行采样（因此使用全局 LevelBiomes 缓存）
// * - 移除 LevelResult，使用本地 Biome[] 作为采样结果容器（保留完整 biome 信息）
// * - 保持 L0/L1/L2 分层、候选判定、重采样、精确 4 邻判定流程
// */
//public class EdgeOfDetector {
//
//    private final JigsawStructure pool;
//    private final StructureSet structureSet;
//    private final GeoSemanticStructureManager geoManager;
//
//    // 保留一些引用（兼容原构造）
//    private final ChunkGenerator generator;
//    private final ServerWorld world;
//    private final NoiseConfig noiseConfig;
//
//    public Heightmap.Type type;
//    public MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler;
//
//    public static int[][] neigh = {{-1,0},{1,0},{0,-1},{0,1}};
//
//    public EdgeOfDetector(
//            ChunkGenerator generator,
//                          ServerWorld world,
//                          NoiseConfig noiseConfig,
//                          StructureSet structureSet,
//                          JigsawStructure pool,
//                          GeoSemanticStructureManager geoManager) {
//        this.generator = generator;
//        this.world = Objects.requireNonNull(world);
//        this.noiseConfig = noiseConfig;
//        this.multiNoiseSampler = noiseSampler();
//        this.pool = pool;
//        this.structureSet = structureSet;
//        this.type = pool.projectStartToHeightmap.get();
//        this.geoManager = Objects.requireNonNull(geoManager);
//    }
//
//    private MultiNoiseUtil.MultiNoiseSampler noiseSampler() {
//        return this.world.getChunkManager().getNoiseConfig().getMultiNoiseSampler();
//    }
//
//    // 用于 map key
//    private static long chunkKey(int cx, int cz) {
//        return (((long) cx) << 32) | (cz & 0xffffffffL);
//    }
//
//
//    private ChunkSamples getSample(int cx, int cz, SampleLevel level) {
//        return this.geoManager.getChunkSamples(
//                this.world,
//                new ChunkPos(cx,cz),
//                this.type,
//                level.sampleSize,
//                this.multiNoiseSampler,
//                noiseConfig,
//                generator
//        );
//    }
//
//    private LevelBiomes getLevelBiomes(SampleLevel level) {
//        return this.geoManager.worldLevelBiomesWithHeightMap.get(world.getRegistryKey()).get(type).get(level);
//    }
//
//    /**
//     * 新版 findEdgeSections：
//     *
//     * - 不再以 spacing 正方形扫描
//     * - 直接使用 GeoSemanticStructureManager.getOffsetsAndStarts(...) 返回的
//     *   “可生成结构的全部区块列表”进行扫描。
//     *
//     * @param candidateChunks GeoSemanticStructureManager.getOffsetsAndStarts 返回的全部候选结构区块
//     * @param target 结构可用的目标群系类型
//     */
//    public List<SectionHit> findEdgeSections(
//            List<ChunkPos> candidateChunks,
//            ObjectOpenHashSet<Biome> target
//    ) {
//        if (candidateChunks == null || candidateChunks.isEmpty()) return new ArrayList<>();
//        if (target == null) return new ArrayList<>();
//        // 1) 构造 levelMap（L0→L1→L2）
//        Object2ObjectOpenHashMap<Long, ChunkSamples> levelMap = new Object2ObjectOpenHashMap<>(candidateChunks.size() * 2);
//        SampleLevel[] levels = {
//                SampleLevel.LEVEL_0,
//                SampleLevel.LEVEL_1,
//                SampleLevel.LEVEL_2,
//        };
//
//        for (ChunkPos pos : candidateChunks) {
//            int cx = pos.x;
//            int cz = pos.z;
//            long key = chunkKey(cx, cz);
//
//            for (SampleLevel level : levels) {
//                ChunkSamples sample = getSample(cx, cz, level);
//                if (sample != null && sample.hasTarget(target, getLevelBiomes(level))) {
//                    levelMap.put(key, sample);
//                    break;
//                }
//            }
//        }
//
//        // 2) 找出边界 chunk
//        // 临接和需要刷新的
//        Set<Long> refineSet = new ObjectOpenHashSet<>(levelMap.size());
//
//        for (Map.Entry<Long, ChunkSamples> e : levelMap.entrySet()) {
//            long key = e.getKey();
//            ChunkSamples cs = e.getValue();
//            if (cs == null) continue;
//
//            boolean isBoundary = false;
//
//            for (int[] n : neigh) {
//                int nx = cs.chunkX + n[0];
//                int nz = cs.chunkZ + n[1];
//                long nk = chunkKey(nx, nz);
//
//                // 无邻居或邻居不是纯净的 → 是边界
//                ChunkSamples ncs = levelMap.get(nk);
//                if (ncs == null) {
//                    continue;
//                }
//                LevelBiomes levelBiomes = getLevelBiomes(GeoSemanticStructureManager.sampleSizeToLevel.get(ncs.sampleSize));
//                if (! ncs.isPure(target,levelBiomes) && ncs.hasTarget(target, levelBiomes))  {
//                    isBoundary = true;
//                    refineSet.add(nk);
//                }
//            }
//
//            if (isBoundary) {
//                refineSet.add(key);
//            }
//        }
//
//        // 检查
//        if (refineSet.isEmpty()){
//            System.err.println("No refineSet sections found");
//            return new ArrayList<>();
//        }
//
//        // 3) 对 candidate 及邻居刷新分辨率重采样到 LEVEL_2 等级
//        Object2ObjectOpenHashMap<Long, ChunkSamples> Level2Map = new Object2ObjectOpenHashMap<>(refineSet.size()*2);
//        for (long k : refineSet) {
//            int cx = (int)(k >> 32);
//            int cz = (int) k;
//            // 如果已经是3级采样了则直接引用levelMap里的缓存
//            if (levelMap.containsKey(k)) {
//                if (levelMap.get(k).sampleSize == SampleLevel.LEVEL_2.sampleSize){
//                    Level2Map.put(k, levelMap.get(k));
//                }else{
//                    // 如果不是则重新采样
//                    ChunkSamples l2samples = getSample(cx, cz, SampleLevel.LEVEL_2);
//                    Level2Map.put(k, l2samples);
//                    levelMap.put(k, l2samples);
//                }
//            }else{
//                ChunkSamples l2samples = getSample(cx, cz, SampleLevel.LEVEL_2);
//                Level2Map.put(k, l2samples);
//                levelMap.put(k, l2samples);
//            }
//        }
//
//        // 4) 精确扫描 Level2Map 里每格，判定边界
//        List<SectionHit> hitList = new ObjectArrayList<>();
//        SampleLevel currentLevel = SampleLevel.LEVEL_2;
//
//        // Level2Map
//        final int sampleSize = currentLevel.sampleSize;                // 每个小 section 的边长（8格）
//        final int perSide    = 16 / sampleSize;  //
//
//        for (long key : Level2Map.keySet()) {
//            ChunkSamples cs = Level2Map.get(key);
//            if (cs == null) continue;
//
//            int cx = cs.chunkX;
//            int cz = cs.chunkZ;
//            LevelBiomes levelBiomes = getLevelBiomes(currentLevel);
//
//            // 既不是纯净 chunk 且包含目标群系，才处理
//            if (!cs.isPure(target,levelBiomes) && cs.hasTarget(target,levelBiomes)) {
//
//                ObjectArrayList<Integer> others = cs.getOthers(target, levelBiomes);
//                if (others.isEmpty()) continue;
//
//                // 区块起始世界坐标（左上角）
//                int chunkWorldX = cx << 4;
//                int chunkWorldZ = cz << 4;
//
//                int y = geoManager.heightSamples.get(world.getRegistryKey()).get(type).get(key);
//
//                for (int sampleIndex : others) {
//                    // 解码 idx，在“先 z 后 x”采样模式下仍然正确：
//                    // iz：行（z方向）
//                    // ix：列（x方向）
//                    int localZ = sampleIndex / perSide;  // 0~3
//                    int localX = sampleIndex % perSide;  // 0~3
//                    // section 的中心点坐标 = 起始 + section偏移
//                    int worldX = chunkWorldX + localX * sampleSize;
//                    int worldZ = chunkWorldZ + localZ * sampleSize;
//
//                    hitList.add(
//                            SectionHit.of(cs,
//                                    sampleIndex,
//                                    worldX, worldZ,
//                                    y,
//                                    levelBiomes.biomePalette.get(cs.biomePalette[cs.samples[sampleIndex]])
//                            )
//                    );
//
//                }
//            }
//        }
//        return hitList;
//    }
//
//
//}
