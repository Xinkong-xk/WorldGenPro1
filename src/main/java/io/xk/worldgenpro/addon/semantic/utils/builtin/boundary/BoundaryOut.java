package io.xk.worldgenpro.addon.semantic.utils.builtin.boundary;

import io.xk.worldgenpro.addon.semantic.GeoSemanticStructureManager;
import io.xk.worldgenpro.addon.semantic.SampleLevel;
import io.xk.worldgenpro.addon.semantic.data.ChunkSamples;
import io.xk.worldgenpro.addon.semantic.data.SectionHit;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.JigsawStructure;

import java.util.*;


public class BoundaryOut {

    private final JigsawStructure structure;
    private final StructureSet structureSet;
    private final GeoSemanticStructureManager geoManager;

    // 保留一些引用（兼容原构造）
    private final ChunkGenerator generator;
    private final ServerWorld world;
    private final NoiseConfig noiseConfig;

    // 其中之一
    public HeightProvider heightProvider;
    public Optional<Heightmap.Type> type;
    public MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler;

    public static int[][] neigh = {{-1,0},{1,0},{0,-1},{0,1}};

    public BoundaryOut(
            ChunkGenerator generator,
                          ServerWorld world,
                          NoiseConfig noiseConfig,
                          StructureSet structureSet,
                          JigsawStructure structure,
                          GeoSemanticStructureManager geoManager) {
        this.generator = generator;
        this.world = Objects.requireNonNull(world);
        this.noiseConfig = noiseConfig;
        this.multiNoiseSampler = noiseSampler();
        this.structure = structure;
        this.structureSet = structureSet;
        this.type = structure.projectStartToHeightmap;
        this.heightProvider = structure.startHeight;
        this.geoManager = Objects.requireNonNull(geoManager);

    }

    private MultiNoiseUtil.MultiNoiseSampler noiseSampler() {
        return this.world.getChunkManager().getNoiseConfig().getMultiNoiseSampler();
    }

    // 用于 map key
    private static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xffffffffL);
    }


    private ChunkSamples getSample(int cx, int cz, SampleLevel level) {
        return this.geoManager.getChunkSamples(
                this.world,
                new ChunkPos(cx,cz),
                level.sampleSize,
                this.multiNoiseSampler,
                generator
        );
    }


    public List<SectionHit> findEdgeSections(
            List<ChunkPos> candidateChunks,
            ObjectOpenHashSet<Biome> target,
            ChunkPos chunkPos
    ) {
        if (candidateChunks == null || candidateChunks.isEmpty()) return new ArrayList<>();
        if (target == null) return new ArrayList<>();
        // 1) 构造 levelMap（L0→L1）
        Object2ObjectOpenHashMap<Long, ChunkSamples> levelMap = new Object2ObjectOpenHashMap<>(candidateChunks.size() * 2);
        // 只采样LEVEL0和LEVEL1等级
        SampleLevel[] levels = {
                SampleLevel.LEVEL_0,
                SampleLevel.LEVEL_1,
        };

        for (ChunkPos pos : candidateChunks) {
            int cx = pos.x;
            int cz = pos.z;
            long key = chunkKey(cx, cz);

            for (SampleLevel level : levels) {
                ChunkSamples sample = getSample(cx, cz, level);
                if (sample != null && sample.hasTarget(target)) {
                    levelMap.put(key, sample);
                    break;
                }
            }
        }

        // 2) 找出边界 chunks
        Set<Long> findChunks = new ObjectOpenHashSet<>(levelMap.size());

        for (Map.Entry<Long, ChunkSamples> e : levelMap.entrySet()) {
            ChunkSamples cs = e.getValue();
            if (cs == null) continue;

            for (int[] n : neigh) {
                int nx = cs.chunkX + n[0];
                int nz = cs.chunkZ + n[1];
                long nk = chunkKey(nx, nz);

                ChunkSamples ncs = levelMap.get(nk);
                if (ncs == null) {
                    ncs = getSample(nx,nz,SampleLevel.LEVEL_1);
                }
                if (!ncs.hasTarget(target))  {
                    findChunks.add(nk);
                }
            }
        }

        // 检查
        if (findChunks.isEmpty()){
            return new ArrayList<>();
        }

        // 3) 对 findChunks 刷新分辨率重采样到 LEVEL_1 等级(16*16)
        Object2ObjectOpenHashMap<Long, ChunkSamples> Level1Map =
                new Object2ObjectOpenHashMap<>(findChunks.size() * 2);

        for (long k : findChunks) {
            int cx = (int)(k >> 32);
            int cz = (int) k;

            if (levelMap.containsKey(k) &&
                    levelMap.get(k).sampleSize == SampleLevel.LEVEL_1.sampleSize) {

                Level1Map.put(k, levelMap.get(k));

            } else {
                ChunkSamples l2samples = getSample(cx, cz, SampleLevel.LEVEL_1);
                Level1Map.put(k, l2samples);
                levelMap.put(k, l2samples);
            }
        }

        // 4) 直接返回Level1Map里的
        List<SectionHit> hitList = new ObjectArrayList<>();

        // Level1Map
        for (long key : Level1Map.keySet()) {
            ChunkSamples cs = Level1Map.get(key);
            if (cs == null) continue;
            // 区块起始世界坐标（左上角）
            int chunkWorldX = cs.chunkX << 4;
            int chunkWorldZ = cs.chunkZ << 4;
            // 获取Y
            int worldX = chunkWorldX + 8;
            int worldZ = chunkWorldZ + 8;
            hitList.add(
                    SectionHit.of(cs,
                            0,
                            worldX, worldZ,
                            cs.levelBiomes.biomePalette.get(cs.biomePalette[cs.samples[0]])
                    )
            );

        }
        return hitList;
    }


}
