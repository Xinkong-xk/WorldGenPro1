package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Str_StrList_To_Null;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

public class CheckSizeConfigItem {

    private final String templatePool;
    private final Input_Str_StrList_To_Null script;
    private final int sizeIndex;
    private final int priority;

    // 构建结果
    public StructurePool structurePoolObject = null;
    public boolean buildResult = false;

    public CheckSizeConfigItem(String templatePool,
                               Input_Str_StrList_To_Null script,
                               int sizeIndex,
                               int priority) {
        this.templatePool = templatePool;
        this.script = script;
        this.sizeIndex = sizeIndex;
        this.priority = priority;
    }

    public Input_Str_StrList_To_Null getScript() {
        return script;
    }

    public int getSizeIndex() {
        return sizeIndex;
    }

    public String getTemplatePool() {
        return templatePool;
    }

    public int getPriority() {
        return priority;
    }

    /** 构建单项 **/
    public void build(MinecraftServer server) {
        // 如果索引值是0,则认为构建失败,因为索引值为0无法检测
        if (sizeIndex == 0) {
            buildResult = false;
            return;
        }
        // 如果模版池不是空的,则加载模版池并且返回
        if (!templatePool.isEmpty()) {
            Optional<RegistryEntry.Reference<StructurePool>> s =
                    server.getRegistryManager()
                            .get(RegistryKeys.TEMPLATE_POOL)
                            .getEntry(RegistryKey.of(RegistryKeys.TEMPLATE_POOL, new Identifier(templatePool)));

            if (s.isPresent()) {
                structurePoolObject = s.get().value();
                buildResult = true;
                return;
            } else {
                throw new RuntimeException("[build CheckSizeConfig] : template pool not found: "+ templatePool);
            }
        }
        // 如果脚本项不是空的,则也认为成功构建
        if (!script.isEmpty){
            buildResult = true;
            return;
        }

        buildResult = false;
    }

    /** 单项 Codec **/
    public static final Codec<CheckSizeConfigItem> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING.optionalFieldOf("pool", "").forGetter(CheckSizeConfigItem::getTemplatePool),
                            Input_Str_StrList_To_Null.CODEC.optionalFieldOf("script", Input_Str_StrList_To_Null.ofExpr("")).forGetter(CheckSizeConfigItem::getScript),
                            Codec.INT.fieldOf("size_index").forGetter(CheckSizeConfigItem::getSizeIndex),
                            Codec.INT.optionalFieldOf("priority", 1000).forGetter(CheckSizeConfigItem::getPriority)
                    ).apply(instance, CheckSizeConfigItem::new)
            );
}
