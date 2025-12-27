package io.xk.worldgenpro.vanilla.enhance.structure.context;

import com.mojang.datafixers.util.Pair;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Int_IntList_Str_StrList_To_Int;
import io.xk.worldgenpro.vanilla.enhance.config.api.ElemConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckDistanceConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckSizeConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item.CheckDistanceConfigItem;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item.CheckSizeConfigItem;
import io.xk.worldgenpro.vanilla.enhance.structure.context.counter.FeatureCounter;
import io.xk.worldgenpro.vanilla.enhance.structure.context.counter.StructureCounter;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructurePoolMixinReflect;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_MIDDLE_CAPACITY;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_SMALL_CAPACITY;

public class StructureContext {
    // 已放置的结构片段记录表  -> {StructurePoolElement:generateCount<int>}
    public final Object2ObjectOpenHashMap<StructurePoolElement, Integer> placedElementMap = new Object2ObjectOpenHashMap<>(BASE_MIDDLE_CAPACITY);

    // 结构片段列表 -> [PoolStructurePiece]
    public final ObjectArrayList<PoolStructurePiece> pieceList = new ObjectArrayList<>();

    // 所有已知的结构池集 -> {StructurePool}
    public HashSet<StructurePool> hasInitStructurePools = new HashSet<>(16);

    // 结构元素最大生成数量表表 -> {StructurePoolElement:maxCount<int>}
    public Object2ObjectOpenHashMap<StructurePoolElement, Integer> poolElementMaxCounts = new Object2ObjectOpenHashMap<>(BASE_MIDDLE_CAPACITY);

    // 结构池总和最大生成数量表 -> {StructurePool:maxCount<int>}
    public Object2ObjectOpenHashMap<StructurePool,Integer> structurePoolMaxCountMap = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // 结构池已生成结构片段数量记录表 -> {StructurePool:generateCount<int>}
    public Object2ObjectOpenHashMap<StructurePool,Integer> placedPieceCountMap = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // 结构池元素最小生成数量表表 -> {StructurePoolElement:minCount<int>}
    public Object2ObjectOpenHashMap<StructurePoolElement, Integer> poolElementMinCounts = new Object2ObjectOpenHashMap<>(BASE_MIDDLE_CAPACITY);

    // 结构池元素预生成分配表 -> {StructurePool:{StructurePoolElement:minCount<int>}}
    public Object2ObjectOpenHashMap<StructurePool, StructurePoolElement[]> preGenerateStructureElement = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);


    // ============== 方法 ==============
    public void addPlacedElement(StructurePoolElement element) {
        placedElementMap.merge(element, 1, Integer::sum);
    }

    public void addPiece(PoolStructurePiece piece) {
        pieceList.add(piece);
    }

    public int getPlacedElementMapSize() {
        return placedElementMap.size();
    }

    public int getPieceListCount() {
        return pieceList.size();
    }

    public int getPlacedElementCount(StructurePoolElement element) {
        return placedElementMap.computeIfAbsent(element, e->0);
    }

    public FeatureCounter Feature = new FeatureCounter(this);

    public StructureCounter Structure = new StructureCounter(this);

    public void initElementCount(Random random, BlockPos pos, int maxDistanceFromCenter,
                                 List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig, StructurePool pool, Structure.Context vanillaContext){
        if (!hasInitStructurePools.contains(pool)){
            // 初始化生成数量表
            LinkedHashMap<String, Object> maxCountArgsMap = new LinkedHashMap<>(8);
            maxCountArgsMap.put("maxDistanceFromCenter", maxDistanceFromCenter);
            maxCountArgsMap.put("start_pos", pos);
            maxCountArgsMap.put("pos", pos);
            // 计算所有数量
            for (var pair : elementCountsConfig){
                StructurePoolElement structurePoolElement = pair.getFirst();

                // 计算最大生成数量
                int max_count = pair.getSecond().max_count().evaluate(random, WorldGenPro.expressionManager, maxCountArgsMap);
                poolElementMaxCounts.put(structurePoolElement, max_count);

                // 计算最小生成数量
                int min_count = pair.getSecond().min_count().evaluate(random, WorldGenPro.expressionManager, maxCountArgsMap);
                poolElementMinCounts.put(structurePoolElement, min_count);
            }
            // 初始化最小生成数量分配表
            loadMinCount(random,pos,maxDistanceFromCenter,elementCountsConfig,pool,vanillaContext);
        }
    }

    public void loadMinCount(
            Random random,
            BlockPos pos,
            int maxDistanceFromCenter,
            List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig,
            StructurePool pool,
            Structure.Context vanillaContext
    ) {
        // 1. 统计总 min_count
        int totalMinCount = 0;
        for (int min : poolElementMinCounts.values()) {
            totalMinCount += min;
        }

        // 1.5. 检查是否超过本结构池最大总生成数
        if (structurePoolMaxCountMap.containsKey(pool)){
            int totalMaxCount = structurePoolMaxCountMap.get(pool);
            if (totalMinCount > totalMaxCount) {
                throw new RuntimeException("Total min count("+totalMinCount+") cannot exceed max count("+totalMaxCount+")");
            }
        }

        // 2. 构造槽位数组（大小 = 总 min）
        List<StructurePoolElement> list = new ArrayList<>(totalMinCount);

        // 3. 先按元素的最小生成数量填满（不考虑顺序）
        for (var entry : poolElementMinCounts.object2ObjectEntrySet()) {
            StructurePoolElement element = entry.getKey();
            int count = entry.getValue();
            for (int i = 0; i < count; i++) {
                list.add(element);
            }
        }

        // 4. 使用 vanillaContext.seed() 进行真正的随机打乱
        Collections.shuffle(list, new java.util.Random(vanillaContext.seed()));
        // 5. 转换成数组
        StructurePoolElement[] finalArray = list.toArray(new StructurePoolElement[0]);
        // 6. 存入 preGenerateStructureElement（现在是 Pool → Element[]）
        preGenerateStructureElement.put(pool, finalArray);
    }

    public void getFromCheckSizeConfig(Random random, BlockPos pos, int maxDistanceFromCenter,
                                       List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig,StructurePool pool,LinkedHashMap<String, Object> maxCountArgsMap){
        // 寻找来自深度测试和距离测试目标池的最大数量
        CheckSizeConfig checkSizeConfig = StructurePoolMixinReflect.getCheckSizeConfig(pool);
        CheckDistanceConfig checkDistanceConfig = StructurePoolMixinReflect.getCheckDistanceConfig(pool);
        for (CheckSizeConfigItem sizeItem : checkSizeConfig.getItems()){
            if (sizeItem.structurePoolObject != null){
                StructurePool structurePool = sizeItem.structurePoolObject;
                List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig_1 = StructurePoolMixinReflect.getElementCountsConfig(structurePool);
                for (var pair : elementCountsConfig_1){
                    // 计算最大生成数
                    StructurePoolElement structurePoolElement = pair.getFirst();
                    int max_count = pair.getSecond().max_count().evaluate(random, WorldGenPro.expressionManager, maxCountArgsMap);
                    poolElementMaxCounts.put(structurePoolElement, max_count);
                }
            }
        }
    }

    public void getFromCheckDistanceConfig(Random random, BlockPos pos, int maxDistanceFromCenter,
                                       List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig,StructurePool pool,LinkedHashMap<String, Object> maxCountArgsMap){
        // 寻找来自距离测试和距离测试目标池的最大数量
        CheckDistanceConfig checkDistanceConfig = StructurePoolMixinReflect.getCheckDistanceConfig(pool);
        for (CheckDistanceConfigItem distanceItem : checkDistanceConfig.getItems()){
            if (distanceItem.structurePoolObject != null){
                StructurePool structurePool = distanceItem.structurePoolObject;
                List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig_1 = StructurePoolMixinReflect.getElementCountsConfig(structurePool);
                for (var pair : elementCountsConfig_1){
                    // 计算最大生成数
                    StructurePoolElement structurePoolElement = pair.getFirst();
                    int max_count = pair.getSecond().max_count().evaluate(random, WorldGenPro.expressionManager, maxCountArgsMap);
                    poolElementMaxCounts.put(structurePoolElement, max_count);
                }
            }
        }
    }

    public void CheckStructurePoolTotalMaxCount(Input_Int_IntList_Str_StrList_To_Int config,
                                                Random random, BlockPos pos, int maxDistanceFromCenter, StructurePool pool){
        if (!structurePoolMaxCountMap.containsKey(pool)){
            // 最大数量表达式参数列表
            LinkedHashMap<String, Object> maxCountArgsMap = new LinkedHashMap<>(8);
            maxCountArgsMap.put("maxDistanceFromCenter", maxDistanceFromCenter);
            maxCountArgsMap.put("start_pos", pos);
            maxCountArgsMap.put("pos", pos);

            // 初始化
            structurePoolMaxCountMap.put(pool,config.evaluate(random, WorldGenPro.expressionManager, maxCountArgsMap));
            placedPieceCountMap.put(pool,0);
        }
    }

    public void addElementCount(StructurePool structurePool) {
        placedPieceCountMap.merge(structurePool,1,Integer::sum);
    }

    // 实现minCount预分配生成表,设计上优先分配生成
    public void checkPreStructurePool(StructurePool structurePool,List<Pair<StructurePoolElement, ElemConfig>> elementCountsConfig){

    }
}
