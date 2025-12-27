package io.xk.worldgenpro.addon.datamixin.mixintype;

import io.xk.worldgenpro.addon.datamixin.DataMixinManager;
import io.xk.worldgenpro.addon.datamixin.api.DataMixinFunction;
import io.xk.worldgenpro.addon.datamixin.api.DataMixinOp;
import io.xk.worldgenpro.addon.datamixin.config.DataMixinElement;
import io.xk.worldgenpro.addon.datamixin.mixintype.utils.structurepool.StructurePoolAdd;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_MIDDLE_CAPACITY;

public class MixinTypeManager {

    public static Object2ObjectOpenHashMap<DataMixinOp, Object2ObjectOpenHashMap<String, DataMixinFunction<?>>> mixinTypeMap = new Object2ObjectOpenHashMap<>(BASE_MIDDLE_CAPACITY);

    public static <T> void register(DataMixinOp op, String mixinType, DataMixinFunction<T> func){
        mixinTypeMap.computeIfAbsent(op, k -> new Object2ObjectOpenHashMap<>())
                .put(mixinType, func);
    }

    public static DataMixinFunction getFunction(DataMixinOp op, String mixinType){
        if (mixinTypeMap.containsKey(op)){
            Object2ObjectOpenHashMap<String, DataMixinFunction<?>> map = mixinTypeMap.get(op);
            if(map.containsKey(mixinType)) return map.get(mixinType);
        }
        return null;
    }

    public static void applyMixin(MinecraftServer server, DataMixinElement dataMixinElement, DataMixinOp op, String mixinType){
        DataMixinFunction mixinFunction = getFunction(op, mixinType);
        if (mixinFunction == null) throw  new RuntimeException("No mixin function found for " + mixinType);

        // 获取目标结构池
        StructurePool structurePool = server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL).get(new Identifier(dataMixinElement.parentFile.mixinTarget));
        String mixinTarget = dataMixinElement.parentFile.mixinTarget;

        // 执行mixin方法
        mixinFunction.mixin(
                server,
                dataMixinElement,
                mixinType,
                mixinTarget,
                structurePool
                );
        DataMixinManager.LOGGER.info("Successfully applied mixin(type : {}) {} to {}",mixinType,dataMixinElement,mixinTarget);
    }

    static {
        // template_pool 类型的 DataMixin 操作方法
        register(
                DataMixinOp.ADD,
                "template_pool",
                (server,dataMixinElement,mixinType,mixinTarget,mixinTargetObject) -> {
                    StructurePool structurePool = (StructurePool)mixinTargetObject;
                    StructurePoolAdd.add(structurePool ,dataMixinElement);
                }
        );

        register(
                DataMixinOp.REMOVE,
                "template_pool",
                (server,dataMixinElement,mixinType,mixinTarget,mixinTargetObject) -> {
                    StructurePool structurePool = (StructurePool)mixinTargetObject;
                }
        );

        register(
                DataMixinOp.REPLACE,
                "template_pool",
                (server,dataMixinElement,mixinType,mixinTarget,mixinTargetObject) -> {
                    StructurePool structurePool = (StructurePool)mixinTargetObject;
                }
        );
    }
}
