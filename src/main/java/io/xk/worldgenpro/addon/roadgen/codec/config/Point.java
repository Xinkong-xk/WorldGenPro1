package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * 点
 * @param structure_set 结构集
 * @param offset 坐标偏移值对象
 */
public record Point(
        Identifier structure_set,
        Offset offset
) {
    public static final Codec<Point> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("structure_set").forGetter(Point::structure_set),
            Offset.CODEC.optionalFieldOf("offset", Offset.ZERO).forGetter(Point::offset)
    ).apply(instance, Point::new));

    // 获取空,或者默认
    public static Point getDefault() {
       return new Point(new Identifier("minecraft:empty"), Offset.ZERO);
    }

    @Override
    public @NotNull String toString() {
        return "Point{" +
                "structure_set=" + structure_set +
                ", offset=" + offset +
                '}';
    }
}
