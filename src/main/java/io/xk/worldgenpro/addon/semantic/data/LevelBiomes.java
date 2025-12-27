package io.xk.worldgenpro.addon.semantic.data;

import io.xk.worldgenpro.addon.semantic.SampleLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.*;


public class LevelBiomes {
    // 采样尺寸
    public int sampleSize;
    // 采样等级
    public SampleLevel sampleLevel;
    // 预计算
    public final float perSide;
    // 期望长度
    public final int expectedLength;
    /** 父级调色板: 存所有出现过的生物群系, 预扩容为BASE_BIG_CAPACITY */
    public ObjectArrayList<Biome> biomePalette = new ObjectArrayList<>(BASE_BIG_CAPACITY);
    /** chunk → samples 映射,预扩容为BASE_LARGE_CAPACITY */
    private final ConcurrentMap<Long, ChunkSamples> samplesByChunk = new ConcurrentHashMap<>(BASE_LARGE_CAPACITY);

    public LevelBiomes(SampleLevel sl, int sampleSize) {
        this.sampleSize = sampleSize;
        this.sampleLevel = sl;
        this.perSide = 16f / sampleSize;
        if (sampleSize == 32) {
            this.expectedLength = 1;
        } else {
            int n = 16 / sampleSize;
            this.expectedLength = n * n;
        }
    }

    /**
     * 工具：编码 chunkKey
 */
    public static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xffffffffL);
    }

    /**
     * 将 biome 放入父调色板（如需要）
     * 返回：此 biome 在调色板中的 short index
     */
    public short ensureBiomeInPalette(Biome biome) {
        int idx = biomePalette.indexOf(biome);
        if (idx >= 0) return (short) idx;

        idx = biomePalette.size();
        biomePalette.add(biome);
        return (short) idx;
    }

    /**
     * 将一组biome放入父调色板（如需要）
     * 返回：此组biome 在调色板中的 short[index...]
     */
    public ObjectOpenHashSet<Short> ensureBiomeInPalette(ObjectOpenHashSet<Biome> biomes) {
        ObjectOpenHashSet<Short> res = new ObjectOpenHashSet<>(biomes.size());
        for(Biome b : biomes){
            res.add(ensureBiomeInPalette(b));
        }
        return res;
    }

    /**
     * 获取 chunk 样本（若无则自动采样并加入 palette）
     */
    public ChunkSamples getChunkSamples(
            int cx, int cz,
            ChunkGenerator generator,
            MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler
    ) {
        long key = chunkKey(cx, cz);
        // 获取缓存
        ChunkSamples cached = samplesByChunk.get(key);
        if (cached != null) return cached;

        int sampleSize = this.sampleSize;
        float perSide = this.perSide;
        int chunkStartX = cx << 4;
        int chunkStartZ = cz << 4;

        // 收集 raw biomes（未 palette 化）
        Biome[] raw = new Biome[expectedLength];
        int idx = 0;

        if (perSide >= 1f) {
            int n = (int) perSide;
            for (int iz = 0; iz < n; iz++) {
                for (int ix = 0; ix < n; ix++) {
                    int bx = chunkStartX + ix * sampleSize;
                    int bz = chunkStartZ + iz * sampleSize;

                    Biome biome = generator.getBiomeSource()
                            .getBiome(BiomeCoords.fromBlock(bx), 0, BiomeCoords.fromBlock(bz), multiNoiseSampler)
                            .value();

                    raw[idx++] = biome;
                }
            }
        } else {
            // sampleSize=32 → chunkStart+16 = 中心点
            int bx = chunkStartX + 16;
            int bz = chunkStartZ + 16;

            Biome biome = generator.getBiomeSource()
                    .getBiome(BiomeCoords.fromBlock(bx), 0, BiomeCoords.fromBlock(bz), multiNoiseSampler)
                    .value();

            raw[0] = biome;

            // 2x2 chunk 共享
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    int ncx = cx + dx;
                    int ncz = cz + dz;
                    long nk = chunkKey(ncx, ncz);
                    samplesByChunk.put(nk,
                            buildChunkSamples(raw, ncx, ncz, nk, sampleSize));
                }
            }
            return samplesByChunk.get(key);
        }

        // palette 化：raw → (chunkPalette + byte array)
        ChunkSamples cs = buildChunkSamples(raw, cx, cz, key, sampleSize);
        samplesByChunk.put(key, cs);
        return cs;
    }


    /**
     * 构造 ChunkSamples：raw → (chunkPalette + localIndexTmp)
     **/
    private ChunkSamples buildChunkSamples(Biome[] raw, int cx, int cz, long key, int sampleSize) {

        // localIndexTmp：每个 sample 对应本地 palette 的索引
        byte[] localIndexTmp = new byte[raw.length];

        // 本 chunk 的 palette（存父级 palette 的索引）
        short[] localPaletteMap = new short[raw.length];
        int localPaletteSize = 0;

        for (int i = 0; i < raw.length; i++) {
            Biome biome = raw[i];

            // 取得在父级 palette 中的 ID（byte）
            short parentId = ensureBiomeInPalette(biome);

            // 查找 parentId 是否已经存在于本地 palette
            int localIndex = -1;
            for (int p = 0; p < localPaletteSize; p++) {
                if (localPaletteMap[p] == parentId) {
                    localIndex = p;
                    break;
                }
            }

            // 如果不存在 → 加入 palette
            if (localIndex < 0) {
                localIndex = localPaletteSize;
                localPaletteMap[localPaletteSize++] = parentId;
            }

            // 记录 sample → local palette 映射
            localIndexTmp[i] = (byte) localIndex;
        }

        // 真正的 chunk palette 为前 localPaletteSize 个
        short[] chunkPalette = Arrays.copyOf(localPaletteMap, localPaletteSize);

        return new ChunkSamples(
                localIndexTmp,
                chunkPalette,
                cx, cz, sampleSize,
                this
        );
    }


    /**
     * 工具：从 chunkSamples 解析真正的 Biome
     *  */
    public Biome resolveBiome(ChunkSamples cs, int sampleIndex) {
        short parentIdx = cs.getParentPaletteIndex(sampleIndex);
        return biomePalette.get(parentIdx);
    }

    /**
     * 检查是否包含目标（使用 palette）
     *  */
    public boolean containsTargetInChunk(int cx, int cz, Set<Biome> target) {
        ChunkSamples cs = samplesByChunk.get(chunkKey(cx, cz));
        if (cs == null) return false;

        for (int i = 0; i < cs.samples.length; i++) {
            Biome b = resolveBiome(cs, i);
            if (target.contains(b)) return true;
        }
        return false;
    }

    /**
     * 清理
     **/
    public void removeChunk(int cx, int cz) {
        samplesByChunk.remove(chunkKey(cx, cz));
    }

    public void clearAll() {
        samplesByChunk.clear();
    }

    public int storedChunkCount() {
        return samplesByChunk.size();
    }
}
