package io.xk.worldgenpro.mixin.vanilla.enhance.template_pool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.datamixin.mixintype.utils.structurepool.StructurePoolUtil;
import io.xk.worldgenpro.utils.MathUtils;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.*;
import io.xk.worldgenpro.vanilla.enhance.structure.CustomStructurePoolGenerator;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Int_IntList_Str_StrList_To_Int;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Str_StrList_To_Null;
import io.xk.worldgenpro.vanilla.enhance.config.api.ElemConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckDistanceConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckSizeConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.element.CheckDistanceConfigElement;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.element.CheckSizeConfigElement;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item.CheckDistanceConfigItem;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item.CheckSizeConfigItem;
import io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable. MutableObject;
import org.spongepowered.asm.mixin.*;

import java.util.*;

import com.mojang.datafixers.util.Pair;

/**
 * Mixin to make StructurePool$weight configurable via ConfigurableNumber
 * and support dynamic weight sampling, including new configurable "max_count".
 */
@Mixin(StructurePool.class)
public abstract class StructurePoolMixin {

    /**
     * elementCountsConfig:
     * List<Pair<StructurePoolElement, Pair<weightExpr, countExpr>>>
     */
    @Unique
    private ObjectArrayList<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig;

    @Shadow @Final
    private ObjectArrayList<StructurePoolElement> elements;

    @Shadow @Final
    private RegistryEntry<StructurePool> fallback;

    @Shadow
    public abstract RegistryEntry<StructurePool> getFallback();

    @Shadow
    public abstract int getElementCount();

    @Shadow @Final @Mutable
    private static Codec<StructurePool> CODEC;

    // 回退结构池
    @Shadow
    @Final
    private static MutableObject<Codec<RegistryEntry<StructurePool>>> FALLBACK;

    // 深度检查配置项
    @Unique
    private CheckSizeConfig checkSizeConfig = null;

    // 距离检查配置项
    @Unique
    private CheckDistanceConfig checkDistanceConfig = null;

    // 结构模版池最大生成数量
    @Unique
    public Input_Int_IntList_Str_StrList_To_Int maxCount;

    // 结构池脚本
    @Unique
    private Input_Str_StrList_To_Null script;


    /*Rewrite Codec to support ConfigurableNumber weight + max_count*/
    static {
        CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        // 回退结构池
                        Codecs.createLazy(FALLBACK::getValue)
                                .fieldOf("fallback")
                                .forGetter(StructurePool::getFallback),
                        // 元素列表
                        StructurePoolUtil.CODEC.listOf()
                                .fieldOf("elements")
                                .forGetter(pool -> ((StructurePoolMixin) (Object) pool).elementCountsConfig),
                        // 深度末尾配置项
                        CheckSizeConfig.CODEC.optionalFieldOf("check_size",new CheckSizeConfig(List.of())) // 默认是空列表
                                .forGetter(pool -> ((StructurePoolMixin) (Object) pool).checkSizeConfig),
                        // 距离末尾配置
                        CheckDistanceConfig.CODEC.optionalFieldOf("check_distance",new CheckDistanceConfig(List.of())) // 默认是空列表
                                .forGetter(pool -> ((StructurePoolMixin) (Object) pool).checkDistanceConfig),
                        // 整个模版池的最大生成数量
                        Input_Int_IntList_Str_StrList_To_Int.CODEC
                                .optionalFieldOf("max_count", Input_Int_IntList_Str_StrList_To_Int.ofSingleInt(Integer.MAX_VALUE))
                                .forGetter(pool -> ((StructurePoolMixin) (Object) pool).maxCount),
                        // 结构池脚本,每次生成都调用一次
                        Input_Str_StrList_To_Null.CODEC
                                .optionalFieldOf("script", new Input_Str_StrList_To_Null(List.of("")))
                                .forGetter(pool -> ((StructurePoolMixin) (Object) pool).script)
                ).apply(instance, StructurePoolMixin::create)
        );
    }

    /**
     * StructurePool 构造桥接函数，多出的 max_count 字段会影响“flat”数量。
     */
    @Unique
    private static StructurePool create(
            RegistryEntry<StructurePool> fallback,
            List<Pair<StructurePoolElement, ElemConfig>> entries,
            CheckSizeConfig checkSizeConfig,
            CheckDistanceConfig checkDistanceConfig,
            Input_Int_IntList_Str_StrList_To_Int maxCount,
            Input_Str_StrList_To_Null script
    ) {
        // NOTE 初始化流程(实际上不需要,只是过个流程,表示完成了初始化)
        ObjectArrayList<Pair<StructurePoolElement, Integer>> resolved = new ObjectArrayList<>();
        ObjectArrayList<StructurePoolElement> flat = new ObjectArrayList<>();
        for (var pair : entries) {
            resolved.add(Pair.of(pair.getFirst(), 1));
            flat.add(pair.getFirst());
        }
        // 创建结构池
        StructurePool pool = new StructurePool(fallback, resolved);
        StructurePoolMixin mixin = (StructurePoolMixin) (Object) pool;

        // NOTE 结构池元素
        mixin.elementCountsConfig = new ObjectArrayList<>(entries);
        mixin.elements.clear();
        mixin.elements.addAll(flat);

        // 设置深度末尾配置项
        mixin.checkSizeConfig = checkSizeConfig;
        // 设置距离末尾配置项
        mixin.checkDistanceConfig = checkDistanceConfig;

        // 设置父结构池
        mixin.checkSizeConfig.parentPool = pool;
        mixin.checkDistanceConfig.parentPool = pool;

        // 设置结构池最大生成数量
        mixin.maxCount = maxCount;

        // 设置脚本
        mixin.script = script;

        // 返回结构池
        return pool;
    }

    /**
     * 仅用于单次随机选择结构（不展开表）
     */
    @Unique
    private StructurePoolElement customRandomElement(
            Structure.Context context,
            Identifier id,
            int size,
            BlockPos pos,
            boolean useExpansionHack,
            int maxDistanceFromCenter,
            ChunkRandom random
    ) {
        // 权重表达式参数列表
        LinkedHashMap<String, Object> weightArgsMap = new LinkedHashMap<>(6);
        weightArgsMap.put("maxDistanceFromCenter", maxDistanceFromCenter);
        weightArgsMap.put("c_distance", 0);
        weightArgsMap.put("e_distance", 0);
        weightArgsMap.put("distance", 0);
        weightArgsMap.put("start_pos", pos);
        weightArgsMap.put("pos", pos);

        // 动态计算权重表
        ObjectArrayList<Pair<StructurePoolElement, Integer>> weighted = new ObjectArrayList<>(this.elementCountsConfig.size());
        int total = 0;

        for (var pair : this.elementCountsConfig) {
            Input_Int_IntList_Str_StrList_To_Int weightExpr = pair.getSecond().weight();
            StructurePoolElement structurePoolElement = pair.getFirst();
            // 计算权重
            int weight = Math.max(0, weightExpr.evaluate(random, WorldGenPro.expressionManager, weightArgsMap));
            // 如果权重大于0则添加
            if (weight > 0) {
                weighted.add(Pair.of(structurePoolElement, weight));
                total += weight;
            }
        }

        if (weighted.isEmpty()) return EmptyPoolElement.INSTANCE;

        int r = random.nextInt(total);
        int acc = 0;
        for (var p : weighted) {
            acc += p.getSecond();
            if (r < acc) return p.getFirst();
        }

        return weighted.getLast().getFirst();
    }

    // 计算深度与索引值是否相等
    @Unique
    private static boolean matchDepth(int maxSize, int minY, int index) {
        maxSize = maxSize-1;
        // 1. 检查负数 index 的合法范围：不能 < -maxSize
        if (index < -maxSize) {
            throw new IllegalArgumentException("index < -maxSize is not allowed");
        }
        // 2. 计算最终的 target 深度
        int target;
        if (index >= 0) {
            // 正数的 index 直接当作深度
            target = index;
        } else {
            // 负数 index 从末尾推（例如 -1 → maxSize, -2 → maxSize-1）
            target = maxSize + index + 1;
        }
        // 3. 比较 minY 和 target
        return minY == target;
    }


    /**
     * 返回随机打乱后的“元素展开表”
     * max_count 的影响在这里体现
     */
    @Unique
    private List<StructurePoolElement> getCustomElementIndicesInRandomOrder(
            CustomStructurePoolGenerator generator,
            PoolStructurePiece piece,
            MutableObject<VoxelShape> pieceShape,
            int currentSize,
            boolean modifyBoundingBox,
            BlockPos currentPos,
            Random random,
            BlockPos startPos,
            Identifier id,
            int maxDistanceFromCenter,
            BlockPos centerPos,
            int maxSize,
            Structure.Context vanillaContext,
            StructureContext structureContext
    ) {
        // 设置起始坐标
        if (startPos == null) startPos = currentPos;

        // 获取结构池对象
        StructurePool structurePool = ((StructurePool) (Object) this);

        // 检查是否成功初始化最大生成数量,注意先后顺序,先计算最大总生成数,再计算最小生成数
        structureContext.CheckStructurePoolTotalMaxCount(maxCount,random,startPos,maxDistanceFromCenter,structurePool);
        structureContext.initElementCount(random,startPos,maxDistanceFromCenter,elementCountsConfig,structurePool,vanillaContext);

        // NOTE 构建参数列表 - Args
        // 计算距离参数
        int c_distance = MathUtils.chebyshev3D(startPos, currentPos);
        int e_distance = (int) MathUtils.euclidean3D(startPos, currentPos);

        //  构建参数列表
        LinkedHashMap<String, Object> argsMap = new LinkedHashMap<>(11);
        argsMap.put("maxDistanceFromCenter", maxDistanceFromCenter);
        argsMap.put("c_distance", c_distance);
        argsMap.put("e_distance", e_distance);
        argsMap.put("distance", e_distance);
        argsMap.put("startPos", startPos);
        argsMap.put("centerPos", centerPos);
        argsMap.put("maxSize", maxSize);
        argsMap.put("random", random);
        argsMap.put("vanillaContext", vanillaContext);
        argsMap.put("pos", currentPos);
        argsMap.put("currentSize", currentSize);

        // 先检测深度测试配置和距离测试配置
        List<StructurePoolElement> checkConfigResult = this.checkConfig_getCustomElementIndicesInRandomOrder(
                generator, piece, pieceShape, currentSize, modifyBoundingBox,
                currentPos, random, startPos, id, maxDistanceFromCenter,
                centerPos, maxSize, vanillaContext,structureContext);
        if (checkConfigResult != null) {
            // 执行脚本
            this.script.run(WorldGenPro.expressionManager,argsMap);
            // 返回
            return checkConfigResult;
        }

        // 如果检测不到则使用默认方法返回结果
        List<StructurePoolElement> rawMethodResult =  this.raw_getElementIndicesInRandomOrder(
                generator, piece, pieceShape, currentSize, modifyBoundingBox,
                currentPos, random, startPos, id, maxDistanceFromCenter,
                centerPos, maxSize, vanillaContext,structureContext,
                structurePool,argsMap
        );
        if (!rawMethodResult.isEmpty()){
            // 执行脚本
            this.script.run(WorldGenPro.expressionManager,argsMap);
        }else{
            rawMethodResult.add(EmptyPoolElement.INSTANCE);
        }

        // 返回
        return rawMethodResult;
    }

    @Unique
    private List<StructurePoolElement> checkConfig_getCustomElementIndicesInRandomOrder(
            CustomStructurePoolGenerator generator,
            PoolStructurePiece piece,
            MutableObject<VoxelShape> pieceShape,
            int currentSize,
            boolean modifyBoundingBox,
            BlockPos currentPos,
            Random random,
            BlockPos startPos,
            Identifier id,
            int maxDistanceFromCenter,
            BlockPos centerPos,
            int maxSize,
            Structure.Context vanillaContext,
            StructureContext structureContext
    ){
        //  Step 1: 收集所有满足条件的 SizeConfig / DistanceConfig
        ObjectArrayList<Object> matchedConfigs = new ObjectArrayList<>(4);

        // --- 所有 sizeConfigs ---
        for (CheckSizeConfigItem cfg : this.checkSizeConfig.getItems()) {
            if (cfg.buildResult && matchDepth(maxSize, currentSize, cfg.getSizeIndex())) {
                matchedConfigs.add(cfg);
            }
        }

        // --- 所有 distanceConfigs ---
        for (CheckDistanceConfigItem cfg : this.checkDistanceConfig.getItems()) {
            if (cfg.buildResult &&
                    DistanceType.distance(cfg.distanceType, centerPos, currentPos) > cfg.getDistance()) {
                matchedConfigs.add(cfg);
            }
        }

        //  Step 2: 如果没有匹配的配置，则返回原始逻辑
        if (matchedConfigs.isEmpty()) {
            return null;
        }

        //  Step 3: 按 priority 选择优先级最高（数值最小）项
        matchedConfigs.sort(Comparator.comparingInt(cfg -> {
            if (cfg instanceof CheckSizeConfigItem) return ((CheckSizeConfigItem) cfg).getPriority();
            if (cfg instanceof CheckDistanceConfigItem) return ((CheckDistanceConfigItem) cfg).getPriority();
            return Integer.MAX_VALUE;
        }));

        //  找出优先级最小的一个,也就是优先级最高的
        Object best = matchedConfigs.getFirst();  // priority 最小

        //  Step 4: 根据类型执行对应 findCheckConfigElements
        if (best instanceof CheckSizeConfigItem sizeCfgItem) {
            return CheckSizeConfigElement.findCheckConfigElements(
                    generator, piece, pieceShape, currentSize, modifyBoundingBox,
                    currentPos, random, startPos, id, maxDistanceFromCenter,
                    centerPos, maxSize, vanillaContext,
                    (StructurePool) (Object) this, sizeCfgItem,structureContext
            );
        } else if (best instanceof CheckDistanceConfigItem distCfgItem) {
            return CheckDistanceConfigElement.findCheckConfigElements(
                    generator, piece, pieceShape, currentSize, modifyBoundingBox,
                    currentPos, random, startPos, id, maxDistanceFromCenter,
                    centerPos, maxSize, vanillaContext,
                    (StructurePool) (Object) this, distCfgItem,structureContext
            );
        }

        //  如果什么都没能返回,则返回null
        return null;
    }

    @Unique
    private List<StructurePoolElement> raw_getElementIndicesInRandomOrder(
            CustomStructurePoolGenerator generator,
            PoolStructurePiece piece,
            MutableObject<VoxelShape> pieceShape,
            int currentSize,
            boolean modifyBoundingBox,
            BlockPos currentPos,
            Random random,
            BlockPos startPos,
            Identifier id,
            int maxDistanceFromCenter,
            BlockPos centerPos,
            int maxSize,
            Structure.Context vanillaContext,
            StructureContext structureContext,
            StructurePool structurePool,
            LinkedHashMap<String, Object> argsMap
    ){
        //  创建返回列表
        ObjectArrayList<StructurePoolElement> list = new ObjectArrayList<>();
        //  检查结构池最大生成数量,如果超过最大值,则直接返回空列表
        if (structureContext.placedPieceCountMap.get(structurePool) >= structureContext.structurePoolMaxCountMap.get(structurePool)){
            list.add(EmptyPoolElement.INSTANCE);
            return list;
        }

        //  生成展开表
        for (var pair : elementCountsConfig) {
            StructurePoolElement element = pair.getFirst();
            //  检查是否超出最大数量限制
            Integer maxAllowed = structureContext.poolElementMaxCounts.computeIfAbsent(element, k -> {
                //  最大数量表达式参数列表
                LinkedHashMap<String, Object> maxCountArgsMap = new LinkedHashMap<>(8);
                maxCountArgsMap.put("maxDistanceFromCenter", maxDistanceFromCenter);
                maxCountArgsMap.put("start_pos", startPos);
                maxCountArgsMap.put("pos", startPos);
                return pair.getSecond().max_count().evaluate(random, WorldGenPro.expressionManager, maxCountArgsMap);
            } );

            // 检查最大数量
            int placed = generator.structureContext.getPlacedElementCount(element);
            if (placed >= maxAllowed) continue;

            //  获取权重表达式
            Input_Int_IntList_Str_StrList_To_Int weightExpr = pair.getSecond().weight();
            //  测试:执行过程脚本
            pair.getSecond().script().run(WorldGenPro.expressionManager, argsMap);
            //  计算权重
            int weight = Math.max(0, weightExpr.evaluate(random, WorldGenPro.expressionManager, argsMap));
            //  添加
            for (int i = 0; i < weight; i++) {
                list.add(element);
            }
        }
        return Util.copyShuffled(list, random);
    }

}
