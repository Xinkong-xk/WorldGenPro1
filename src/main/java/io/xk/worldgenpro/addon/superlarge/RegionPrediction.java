package io.xk.worldgenpro.addon.superlarge;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;

public class RegionPrediction {
    public ChunkPos startChunk;    // null 表示不会生成
    public boolean isGenerated;    // 新字段：是否已经真正生成

    public RegionPrediction(ChunkPos startChunk) {
        this.startChunk = startChunk;
        this.isGenerated = false;
    }

    public RegionPrediction(ChunkPos startChunk, boolean isGenerated) {
        this.startChunk = startChunk;
        this.isGenerated = isGenerated;
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    public void setGenerated(boolean generated) {
        this.isGenerated = generated;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();

        if (startChunk != null) {
            tag.putInt("x", startChunk.x);
            tag.putInt("z", startChunk.z);
        }

        // 新字段：保存是否生成
        tag.putBoolean("isGenerated", isGenerated);

        return tag;
    }

    public static RegionPrediction fromNbt(NbtCompound tag) {
        ChunkPos pos = null;
        if (tag.contains("x")) {
            pos = new ChunkPos(tag.getInt("x"), tag.getInt("z"));
        }

        // 旧存档兼容：如果没有这个字段，默认 false
        boolean isGenerated = tag.contains("isGenerated") && tag.getBoolean("isGenerated");

        return new RegionPrediction(pos, isGenerated);
    }

    public ChunkPos startChunk() {
        return startChunk;
    }
}
