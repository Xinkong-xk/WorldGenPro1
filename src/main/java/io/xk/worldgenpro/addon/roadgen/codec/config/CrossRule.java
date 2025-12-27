package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 道路穿越规则,表示一条道路如何穿过目标群系
 * @param road 道路配置对象
 * @param points 此群系道路上所有的穿越点
 * @param decorations 此群系道路上所有装饰物
 */
public record CrossRule(
        RoadSettings road,
        Map<String,CrossPoints> points,
        List<DecorationEntry> decorations
) {
    public static final Codec<CrossRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RoadSettings.CODEC.optionalFieldOf("road",RoadSettings.getDefault()).forGetter(CrossRule::road),
            Codec.unboundedMap(Codec.STRING, CrossPoints.CODEC)
                    .optionalFieldOf("points", Map.of())
                    .forGetter(CrossRule::points),
            DecorationEntry.CODEC.listOf()
                    .optionalFieldOf("decorations", List.of())
                    .forGetter(CrossRule::decorations)
    ).apply(instance, CrossRule::new));

    @Override
    public @NotNull String toString() {
        return "CrossRule{" +
                "road=" + road +
                ", points=" + points +
                ", decorations=" + decorations +
                '}';
    }

    public void build(MinecraftServer server) {
        // 获取结构池注册项
        Registry<StructurePool> structurePoolRegistry = server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);

        // 构建点的结构池对象
        for (Map.Entry<String, CrossPoints> entry : points.entrySet()) {
            CrossPoints crossPoints = entry.getValue();
            crossPoints.poolObject = getStructurePool(structurePoolRegistry,crossPoints.pool);
        }

        // 构建装饰物的结构池对象
        for (DecorationEntry entry: decorations) {
            entry.poolObject = getStructurePool(structurePoolRegistry,entry.pool);
        }
    }

    public StructurePool getStructurePool(Registry<StructurePool> structurePoolRegistry, String pool) {
        var s = structurePoolRegistry.getEntry(RegistryKey.of(RegistryKeys.TEMPLATE_POOL,new Identifier(pool)));
        if (s.isPresent()){
            return s.get().value();
        } else {
            throw new RuntimeException("[RoadGen:Entry.Cross.CrossRule<build-StructurePool>] : pool not found :"+pool);
        }
    }
}
