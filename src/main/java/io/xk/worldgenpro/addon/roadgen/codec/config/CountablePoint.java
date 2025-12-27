package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * 可计数的点
 * @param structure_set 结构集
 * @param offset 坐标偏移值
 * @param roadCount 道路数量
 */
public record CountablePoint(
        Identifier structure_set,
        Offset offset,
        int roadCount
) {
    public static final Codec<CountablePoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("structure_set").forGetter(CountablePoint::structure_set),
            Offset.CODEC.optionalFieldOf("offset", Offset.ZERO).forGetter(CountablePoint::offset),
            Codec.INT.optionalFieldOf("road_count",1).forGetter(CountablePoint::roadCount)
    ).apply(instance, CountablePoint::new));

    // 获取空,或者默认
    public static CountablePoint getDefault() {
       return new CountablePoint(new Identifier("minecraft:empty"), Offset.ZERO,1);
    }

    @Override
    public @NotNull String toString() {
        return "CountablePoint{" +
                "structure_set=" + structure_set +
                ", offset=" + offset +
                ", roadCount=" + roadCount +
                '}';
    }
}
