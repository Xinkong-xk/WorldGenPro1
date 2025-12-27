package io.xk.worldgenpro.addon.datamixin.mixintype.utils.structurepool;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import io.xk.worldgenpro.addon.datamixin.config.DataMixinElement;
import io.xk.worldgenpro.vanilla.enhance.config.api.ElemConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructurePoolMixinReflect;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.structure.pool.LegacySinglePoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;

import static io.xk.worldgenpro.addon.datamixin.DataMixinManager.LOGGER;

public class StructurePoolAdd {
    public static void add(StructurePool pool,
                           DataMixinElement dataMixinElement) {

        Pair<StructurePoolElement, ElemConfig> pairCodec = StructurePoolUtil.CODEC.parse(
                JsonOps.INSTANCE,
                dataMixinElement.value.getAsJsonObject()
                ).resultOrPartial(error -> LOGGER.error("Failed parsing \n{}\n- {}", dataMixinElement, error))
                .orElse(null);

        if (pairCodec == null) {
            throw new RuntimeException("Failed parsing");
        }

        // 添加进elements列表
        ObjectArrayList<StructurePoolElement> elements = StructurePoolMixinReflect.getElements(pool);
        elements.add(pairCodec.getFirst());

        // 添加进elementCountsConfig列表
        java.util.List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig = StructurePoolMixinReflect.getElementCountsConfig(pool);
        elementCountsConfig.add(pairCodec);
    }
}
