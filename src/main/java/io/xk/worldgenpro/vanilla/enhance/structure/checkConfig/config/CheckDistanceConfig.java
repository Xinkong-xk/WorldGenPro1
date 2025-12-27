package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config;

import com.mojang.serialization.Codec;

import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.DistanceType;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item.CheckDistanceConfigItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CheckDistanceConfig {

    public StructurePool parentPool;

    /** 新的：列表形式 **/
    private final List<CheckDistanceConfigItem> items;

    public CheckDistanceConfig(List<CheckDistanceConfigItem> items) {
        this.items = items;
    }

    public List<CheckDistanceConfigItem> getItems() {
        return items;
    }

    /** 构建所有项 **/
    public void buildConfig(MinecraftServer server) {
        for (CheckDistanceConfigItem item : items) {
            item.build(server);
            if (item.structurePoolObject == parentPool){
                // 目标结构池不能等于父结构池,否则会导致死循环
                throw new RuntimeException("Structure pool object Cannot Equal The Parent Structure Pool: "+item.getTemplatePool());
            }
        }
    }

    /** 判断哪些项满足距离条件，返回满足条件的项 **/
    public List<CheckDistanceConfigItem> match(BlockPos center, BlockPos pos) {
        List<CheckDistanceConfigItem> result = new ArrayList<>();

        for (CheckDistanceConfigItem item : items) {
            if (!item.buildResult) continue;

            double d = DistanceType.distance(item.distanceType, center, pos);
            if (d > item.getDistance()) {
                result.add(item);
            }
        }
        return result;
    }

    /** 新 Codec：列表形式 **/
    public static final Codec<CheckDistanceConfig> CODEC =
            CheckDistanceConfigItem.CODEC.listOf()
                    .xmap(CheckDistanceConfig::new, CheckDistanceConfig::getItems);
}
