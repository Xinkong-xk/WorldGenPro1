package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * 道路的基础配置
 * @param roadWidth 道路宽度
 * @param roadType 道路类型，道路的外观由道路类型决定
 */
public record RoadSettings(
        int roadWidth,
        Identifier roadType
) {
    public static final Codec<RoadSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("road_width").forGetter(RoadSettings::roadWidth),
            Identifier.CODEC.fieldOf("road_type").forGetter(RoadSettings::roadType)
    ).apply(instance, RoadSettings::new));

    public static RoadSettings getDefault() {
        return new RoadSettings(0,new Identifier("minecraft:empty"));
    }

    @Override
    public @NotNull String toString() {
        return "RoadSettings{" +
                "roadWidth=" + roadWidth +
                ", roadType=" + roadType +
                '}';
    }
}
