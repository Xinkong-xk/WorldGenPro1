package io.xk.worldgenpro.vanilla.enhance.structure.reflect;

import io.xk.worldgenpro.mixin.vanilla.enhance.template_pool.StructurePoolMixin;
import io.xk.worldgenpro.vanilla.enhance.config.api.ElemConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckDistanceConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckSizeConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.CustomStructurePoolGenerator;
import io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableObject;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

/**
 * Reflect helper for StructurePoolMixin – exports all @Unique fields and methods.
 */
public final class StructurePoolMixinReflect {

    /** mixin 最终注入的 class（仍是 StructurePool 本类） */
    private static final Class<?> TARGET = StructurePool.class;

    /** 缓存的方法表 */
    private static final Object2ObjectOpenHashMap<String, Method> METHODS = new Object2ObjectOpenHashMap<>();

    /** 缓存字段 */
    public static Field F_elementCountsConfig;

    // 检测配置
    public static Field checkSizeConfig;
    public static Field checkDistanceConfig;

    public static Field elements;
    public static Field elementCountsConfig;

    public static ObjectArrayList<StructurePoolElement> getElements(StructurePool structurePool){
        try{
            return (ObjectArrayList<StructurePoolElement>) elements.get(structurePool);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            // ------------ 1. 扫描所有字段 ------------
            for (Field f : TARGET.getDeclaredFields()) {
                f.setAccessible(true);
                switch (f.getName()) {
                    case "elementCountsConfig" ->
                            F_elementCountsConfig = f;
                    case "checkSizeConfig"->
                            checkSizeConfig = f;
                    case "checkDistanceConfig"->
                            checkDistanceConfig = f;
                    case "elements"->
                        elements = f;
                    default -> {}
                }
            }

            if (F_elementCountsConfig == null)
                throw new RuntimeException("Cannot find field: elementCountsConfig");

            // ------------ 2. 扫描所有 mixin 注入的方法 ------------
            for (Method m : TARGET.getDeclaredMethods()) {
                String name = m.getName();
                if (name.startsWith("custom")
                        || name.startsWith("getCustom")
                        || name.startsWith("initMax")
                        || name.startsWith("getStructureTemplate")) {

                    m.setAccessible(true);
                    METHODS.put(name, m);
                }
            }

            LOGGER.info("[Reflect] StructurePoolMixinReflect.class init: {} methods cached", METHODS.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed initializing StructurePoolMixinReflect", e);
        }
    }


    //   通用方法调用
    @SuppressWarnings("unchecked")
    private static <T> T call(Object instance, String name, Object... args) {
        try {
            Method m = METHODS.get(name);
            if (m == null)
                throw new IllegalArgumentException("Mixin method not found: " + name);

            return (T) m.invoke(instance, args);

        } catch (Exception e) {
            throw new RuntimeException("Error invoking " + name, e);
        }
    }


    //   字段读取 API
    /** 读取 elementCountsConfig */
    public static List<Pair<StructurePoolElement, ElemConfig>> getElementCountsConfig(StructurePool pool) {
        try {
            return (List<Pair<StructurePoolElement, ElemConfig>>) F_elementCountsConfig.get(pool);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    //   方法调用 API（类型安全封装）
    /** 调用 customRandomElement */
    public static StructurePoolElement customRandomElement(
            StructurePool pool,
            Structure.Context ctx,
            Identifier id,
            int size,
            BlockPos pos,
            boolean hack,
            int maxDist,
            ChunkRandom random
    ) {
        return call(pool, "customRandomElement", ctx, id, size, pos, hack, maxDist, random);
    }

    /** 调用 initMaxCount */
    public static void initMaxCount(
            StructurePool pool,
            Random random,
            BlockPos pos,
            int maxDist
    ) {
        call(pool, "initMaxCount", random, pos, maxDist);
    }

    /** 调用 getCustomElementIndicesInRandomOrder */
    public static List<StructurePoolElement> getCustomElementIndicesInRandomOrder(
            StructurePool pool,
            CustomStructurePoolGenerator generator,
            PoolStructurePiece piece,
            MutableObject<VoxelShape> shape,
            int currentSize,
            boolean modifyBoundingBox,
            BlockPos current,
            Random random,
            BlockPos start,
            Identifier id,
            int maxDistanceFromCenter,
            BlockPos centerPos,
            int maxSize,
            Structure.Context vanillaContext,
            StructureContext structureContext) {
        return call(pool, "getCustomElementIndicesInRandomOrder",
                generator, piece, shape, currentSize, modifyBoundingBox,
                current, random, start, id, maxDistanceFromCenter,centerPos,maxSize,
                vanillaContext,structureContext
        );
    }

    /** 调用 getStructureTemplate */
    public static Identifier getStructureTemplate(
            StructurePool pool,
            StructurePoolElement element,
            CustomStructurePoolGenerator generator,
            PoolStructurePiece piece,
            MutableObject<VoxelShape> shape,
            int minY,
            boolean modifyBB,
            BlockPos current,
            Random random,
            BlockPos start,
            Identifier id,
            int maxDist,
            LinkedHashMap<String, Object> argsMap
    ) {
        return call(pool, "getStructureTemplate",
                element, generator, piece, shape, minY, modifyBB,
                current, random, start, id, maxDist, argsMap);
    }

    public static CheckSizeConfig getCheckSizeConfig(StructurePool structurePool) {
        try {
            return (CheckSizeConfig) checkSizeConfig.get(structurePool);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CheckDistanceConfig getCheckDistanceConfig(StructurePool structurePool) {
        try {
            return (CheckDistanceConfig) checkDistanceConfig.get(structurePool);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
