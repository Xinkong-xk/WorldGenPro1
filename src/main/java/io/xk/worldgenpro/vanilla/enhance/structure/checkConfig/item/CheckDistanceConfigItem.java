package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Str_StrList_To_Null;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.DistanceType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

public class CheckDistanceConfigItem {

    private final String templatePool;
    private final Input_Str_StrList_To_Null script;
    private final int distance;
    private final String distanceTypeText;
    private final int priority;

    // 实际数据
    public StructurePool structurePoolObject = null;
    public DistanceType distanceType;
    public boolean buildResult = false;

    public CheckDistanceConfigItem(String templatePool,
                                   Input_Str_StrList_To_Null script,
                                   int distance,
                                   String distanceTypeText,
                                   int priority) {
        this.templatePool = templatePool;
        this.script = script;
        this.distance = distance;
        this.distanceTypeText = distanceTypeText;
        this.priority = priority;
    }

    public void build(MinecraftServer server){
        if (distance <= 0){
            buildResult = false;
            return;
        }
        // 如果距离类型是空的,则认为构建失败
        if (!distanceTypeText.isEmpty()){
            distanceType = DistanceType.fromName(distanceTypeText);
        } else {
            buildResult = false;
            return;
        }
        // 如果模版池不是空的,则加载模版池并且返回
        if (!templatePool.isEmpty()){
            Optional<RegistryEntry.Reference<StructurePool>> s =
                    server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL)
                            .getEntry(RegistryKey.of(RegistryKeys.TEMPLATE_POOL,new Identifier(this.templatePool)));

            if (s.isPresent()){
                structurePoolObject = s.get().value();
                buildResult = true;
                return;
            } else {
                throw new RuntimeException("[build CheckDistanceConfig] : template pool not found: "+ templatePool);
            }
        }
        // 如果脚本项不是空的,则也认为成功构建
        if (!script.isEmpty){
            buildResult = true;
            return;
        }

        buildResult = false;
    }

    public int getDistance() { return distance; }
    public int getPriority() { return priority; }
    public Input_Str_StrList_To_Null getScript() { return script; }
    public String getTemplatePool() { return templatePool; }
    public String getDistanceTypeText() { return distanceTypeText; }

    /** 单项 Codec **/
    public static final Codec<CheckDistanceConfigItem> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING.optionalFieldOf("pool","").forGetter(CheckDistanceConfigItem::getTemplatePool),
                            Input_Str_StrList_To_Null.CODEC.optionalFieldOf("script", Input_Str_StrList_To_Null.ofExpr("")).forGetter(CheckDistanceConfigItem::getScript),
                            Codec.INT.fieldOf("distance").forGetter(CheckDistanceConfigItem::getDistance),
                            Codec.STRING.optionalFieldOf("distance_type","euclidean3d").forGetter(CheckDistanceConfigItem::getDistanceTypeText),
                            Codec.INT.optionalFieldOf("priority",1000).forGetter(CheckDistanceConfigItem::getPriority)
                    ).apply(instance, CheckDistanceConfigItem::new)
            );
}
