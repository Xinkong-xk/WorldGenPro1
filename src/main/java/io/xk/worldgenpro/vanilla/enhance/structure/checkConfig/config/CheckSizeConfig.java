package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config;

import com.mojang.serialization.Codec;

import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item.CheckSizeConfigItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;

import java.util.List;

public class CheckSizeConfig {
    public StructurePool parentPool;

    public final List<CheckSizeConfigItem> items;

    public CheckSizeConfig(List<CheckSizeConfigItem> items) {
        this.items = items;
    }

    public List<CheckSizeConfigItem> getItems() {
        return items;
    }

    /** 构建整个列表 **/
    public void buildConfig(MinecraftServer server) {
        for (CheckSizeConfigItem item : items) {
            item.build(server);
            if (item.structurePoolObject == parentPool){
                // 目标结构池不能等于父结构池,否则会导致死循环
                throw new RuntimeException("Structure pool object Cannot Equal The Parent Structure Pool: "+item.getTemplatePool());
            }
        }
    }

    /** Codec：通过 listOf() 转换 **/
    public static final Codec<CheckSizeConfig> CODEC =
            CheckSizeConfigItem.CODEC.listOf()
                    .xmap(CheckSizeConfig::new, CheckSizeConfig::getItems);

    @Override
    public String toString() {
        return "CheckSizeConfig{items=" + items + "}";
    }
}
