package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 坐标偏移值对象
 * @param x x方向偏移
 * @param y y方向偏移
 * @param z z方向偏移
 */
public record Offset(int x, int y, int z) {
    public static final Codec<Offset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("x",0).forGetter(Offset::x),
            Codec.INT.optionalFieldOf("y",0).forGetter(Offset::y),
            Codec.INT.optionalFieldOf("z",0).forGetter(Offset::z)
    ).apply(instance, Offset::new));

    public static final Offset ZERO = new Offset(0,0,0);
}
