package io.xk.worldgenpro.addon.semantic.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.biome.Biome;

/**
 * Chunk 样本结构（palette + byte samples）
 */
public final class ChunkSamples {
    /** chunk 的内部采样（指向 biomePalette 的本地索引） */
    public final byte[] samples;
    /** 本 chunk 的调色板（short 指向父级 LevelBiomes 的调色板） */
    public final short[] biomePalette;
    /**
     * 区块坐标X
     */
    public final int chunkX;
    /**
     * 区块坐标Z
     */
    public final int chunkZ;
    /**
     * 采样大小
     */
    public final int sampleSize;
    /**
     * section缓存
     */
    public SectionHit[] sectionHitList;

    public ObjectOpenHashSet<Biome> biomesSet = null;

    public LevelBiomes levelBiomes;

    public ChunkSamples(byte[] samples, short[] palette,
                        int chunkX, int chunkZ,  int sampleSize,LevelBiomes levelBiomes) {
        this.samples = samples;
        this.biomePalette = palette;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.sampleSize = sampleSize;
        this.sectionHitList = new SectionHit[samples.length];
        this.levelBiomes = levelBiomes;
    }

    /** 返回父调色板的索引 */
    public short getParentPaletteIndex(int localIndex) {
        int bi = Byte.toUnsignedInt(samples[localIndex]);
        return biomePalette[bi];
    }

    public boolean isPure(Biome target){
        return biomePalette.length == 1;
    }

    public boolean isPure(ObjectOpenHashSet<Biome> targets, LevelBiomes levelBiomes){
        if (biomesSet == null){
            biomesSet = new ObjectOpenHashSet<>(biomePalette.length*2);
            for (int i = 0; i < biomePalette.length; i++){
                biomesSet.add(levelBiomes.biomePalette.get(biomePalette[i]));
            }
        }
        return targets.equals(biomesSet);
    }

    public boolean hasTarget(Biome target) {
        short parentId = levelBiomes.ensureBiomeInPalette(target);
        // palette 一般只有 1~4 个元素，因此线性扫描最快
        for (short p : this.biomePalette) {
            if (p == parentId) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTarget(ObjectOpenHashSet<Biome> targets) {
        ObjectOpenHashSet<Short> parentIds = levelBiomes.ensureBiomeInPalette(targets);
        // palette 一般只有 1~4 个元素，因此线性扫描最快
        for (short p : this.biomePalette) {
            if (parentIds.contains(p)) {
                return true;
            }
        }
        return false;
    }

    public ObjectArrayList<Integer> getOthers(ObjectOpenHashSet<Biome> targets, LevelBiomes levelBiomes) {
        // 获取 targets 的 parent 调色板索引
        ObjectOpenHashSet<Short> targetParentIds = levelBiomes.ensureBiomeInPalette(targets);
        ObjectArrayList<Integer> result = new ObjectArrayList<>(this.samples.length);
        // 本 chunk 的 parent palette
        final int len = this.samples.length;
        for (int i = 0; i < len; i++) {
            // 本地索引 → parent 索引
            int localIndex = Byte.toUnsignedInt(this.samples[i]);
            short parentId = this.biomePalette[localIndex];
            // 如果不是目标群系，记录下这个采样点的 sectionIndex
            if (targetParentIds.contains(parentId)) {
                result.add(i);
            }
        }
        return result;
    }

}