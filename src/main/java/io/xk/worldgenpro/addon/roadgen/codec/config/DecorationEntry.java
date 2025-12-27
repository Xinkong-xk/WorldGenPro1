package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.api.vanilla.enhance.structure.IntRange;
import net.minecraft.structure.pool.StructurePool;

public class DecorationEntry{
    public String pool;
    public IntRange spacing;
    public float probability;
    public int size;
    public Offset offset;

    // 后置获取
    public StructurePool poolObject;

    public String getPool() {
        return pool;
    }

    public IntRange getSpacing() {
        return spacing;
    }

    public float getProbability() {
        return probability;
    }

    public int getSize() {
        return size;
    }

    public Offset getOffset() {
        return offset;
    }

    public DecorationEntry(String pool, IntRange spacing, float probability, int size, Offset offset) {
        this.pool = pool;
        this.spacing = spacing;
        this.probability = probability;
        this.size = size;
        this.offset = offset;
    }

    public static final Codec<DecorationEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pool").forGetter(DecorationEntry::getPool),
            IntRange.CODEC.optionalFieldOf("spacing",IntRange.of(1,1)).forGetter(DecorationEntry::getSpacing),
            Codec.FLOAT.optionalFieldOf("probability",1.0f).forGetter(DecorationEntry::getProbability),
            Codec.INT.optionalFieldOf("size",1).forGetter(DecorationEntry::getSize),
            Offset.CODEC.optionalFieldOf("offset",Offset.ZERO).forGetter(DecorationEntry::getOffset)
    ).apply(instance, DecorationEntry::new));

    @Override
    public String toString() {
        return "DecorationEntry{" +
                "pool='" + pool + '\'' +
                ", spacing=" + spacing +
                ", probability=" + probability +
                ", size=" + size +
                ", poolObject=" + poolObject +
                ", offset=" + offset +
                '}';
    }
}
