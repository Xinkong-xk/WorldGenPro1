package io.xk.worldgenpro.addon.semantic.data;

import net.minecraft.world.biome.Biome;

public class SectionHit {
    public final int chunkX, chunkZ;
    public final int sampleIndex; // 0..15
    public final Biome biome;
    public final int worldX, worldZ;
    public final int sampleSize;
    public final int y;

    public static SectionHit of(ChunkSamples chunkSamples, int sampleIndex, int worldX, int worldZ, Biome biome){
        if (chunkSamples.sectionHitList[sampleIndex] == null){
            chunkSamples.sectionHitList[sampleIndex] = new SectionHit(
                    chunkSamples.chunkX,
                    chunkSamples.chunkZ,
                    sampleIndex,
                    worldX,
                    worldZ,
                    biome,
                    chunkSamples.sampleSize,0
            );
        }
        return chunkSamples.sectionHitList[sampleIndex];
    }

    public SectionHit(int chunkX, int chunkZ, int sampleIndex, int worldX, int worldZ, Biome biome, int sampleSize,int y) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.sampleIndex = sampleIndex;
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.biome = biome;
        this.sampleSize = sampleSize;
        this.y = y;
    }

    @Override
    public String toString() {
        return "SectionHit{" +
                "chunkX=" + chunkX +
                ", chunkZ=" + chunkZ +
                ", sampleIndex=" + sampleIndex +
                ", biome=" + biome +
                ", worldX=" + worldX +
                ", worldZ=" + worldZ +
                ", sampleSize=" + sampleSize +
                '}';
    }
}
