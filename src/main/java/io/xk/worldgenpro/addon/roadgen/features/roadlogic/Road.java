package io.xk.worldgenpro.addon.roadgen.features.roadlogic;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.api.vanilla.enhance.structure.IntRange;
import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.codec.config.CrossRule;
import io.xk.worldgenpro.addon.roadgen.codec.config.DecorationEntry;
import io.xk.worldgenpro.addon.roadgen.codec.config.RoadEntry;
import io.xk.worldgenpro.addon.roadgen.config.ModConfig;
import io.xk.worldgenpro.addon.roadgen.api.data.RoadData;
import io.xk.worldgenpro.addon.roadgen.features.config.RoadFeatureConfig;
import io.xk.worldgenpro.addon.roadgen.features.roadlogic.api.RoadPathCalculatorOptimized;
import io.xk.worldgenpro.addon.roadgen.helpers.Records;
import io.xk.worldgenpro.addon.roadgen.persistence.attachments.WorldDataAttachment;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;

import java.util.ArrayList;
import java.util.List;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;
import static io.xk.worldgenpro.addon.roadgen.config.ModConfig.averagingRadius;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_MIDDLE_CAPACITY;

public class Road {

    public ServerWorld serverWorld;
    public Records.StructureConnection structureConnection;
    public RoadFeatureConfig context;
    public String roadEntryKey;
    public RoadEntry roadEntry;

    private static final int ROAD_END_SKIP = 60;

    public Road(ServerWorld serverWorld, Records.StructureConnection structureConnection, RoadFeatureConfig config) {
        this.serverWorld = serverWorld;
        this.structureConnection = structureConnection;
        this.context = config;
        this.roadEntryKey = structureConnection.roadEntryKey();
        this.roadEntry = WorldGenPro.roadConnectionConfigManager.getRoadEntry(this.roadEntryKey);
    }

    public void generateRoad(int maxSteps){
        // DEBUG
        LOGGER.info("[RoadGen:New Road Generate] 创建了一条新的道路在: {} -> {}",structureConnection.from(),structureConnection.to());

        Random random = Random.create(this.serverWorld.getSeed());
        int width = getRandomWidth(random, context);
        int type = allowedRoadTypes(random);
        // if all road types are disabled in config
        if (type == -1) {
            return;
        }

        // 获取材质
        List<BlockState> material = (type == 1) ? getRandomNaturalRoadMaterials(random, context) : getRandomArtificialRoadMaterials(random, context);

        // 起始和结束坐标
        BlockPos start = structureConnection.from();
        BlockPos end = structureConnection.to();

        // 计算路径曲线上的各个路径中心点
        List<Records.RoadSegmentPlacement> roadSegmentPlacementList = RoadPathCalculatorOptimized.calculateAStarRoadPath(start, end, width, serverWorld, maxSteps);
        // 拷贝表
        List<RoadData> roadDataList = new ArrayList<>(serverWorld.getAttachedOrCreate(WorldDataAttachment.ROAD_DATA_LIST, ArrayList::new));

        // NOTE [优化]创建区块坐标HashSet
        Object2ObjectLinkedOpenHashMap<Long,List<RoadData.SegmentData>> roadSegmentLinkMap = new Object2ObjectLinkedOpenHashMap<>(roadSegmentPlacementList.size()*2);
        List<BlockPos> roadNodes = new ArrayList<>(roadSegmentPlacementList.size());

        for (Records.RoadSegmentPlacement roadSegmentPlacement : roadSegmentPlacementList) {
            roadNodes.add(roadSegmentPlacement.middlePos());
        }

        // NOTE 模仿原逻辑进行预构建
        makeRoadSegmentLinkMap(serverWorld,roadSegmentLinkMap,roadSegmentPlacementList,roadNodes);

        // 添加进世界持久化
        roadDataList.add(new RoadData(width, type, material,roadSegmentPlacementList,roadNodes,roadSegmentLinkMap,roadEntryKey));
        serverWorld.setAttached(WorldDataAttachment.ROAD_DATA_LIST, roadDataList);

        if (roadSegmentPlacementList.isEmpty()){
            LOGGER.info("正在生成的道路: {}:{}->{}, 没有成功生成任何segment",structureConnection.roadEntryKey(),structureConnection.from(),structureConnection.to());
        }else{
            LOGGER.info("完成道路生成,生成了{}个segment",roadSegmentPlacementList.size());
        }
    }

    public void makeRoadSegmentLinkMap(ServerWorld serverWorld,
                                              Object2ObjectLinkedOpenHashMap<Long,List<RoadData.SegmentData>> roadSegmentLinkMap,
                                              List<Records.RoadSegmentPlacement> roadSegmentPlacementList,
                                              List<BlockPos> roadNodes){
        int segmentIndex = 0;
        int listSize = roadSegmentPlacementList.size();

        // 临时储存
        IntArrayList i_ArrayList = new IntArrayList(listSize);
        IntArrayList segmentIndex_ArrayList = new IntArrayList(listSize);
        ObjectArrayList<Records.RoadSegmentPlacement> segment_ArrayList = new ObjectArrayList<>(listSize);
        LongArrayList chunkKey_ArrayList = new LongArrayList(listSize);


        // STEP[1]: 构建所有segment
        for (int i = 2; i < listSize - 2; i++) {
            segmentIndex ++;
            Records.RoadSegmentPlacement segment = roadSegmentPlacementList.get(i);
            BlockPos segmentMiddlePos = segment.middlePos();

            // // offset to pool
            if (segmentIndex < ROAD_END_SKIP || segmentIndex > listSize - ROAD_END_SKIP) continue;

            // 获取中点坐标
            ChunkPos middleChunkPos = new ChunkPos(segmentMiddlePos);

            // 计算区块键
            long chunkKey = RoadGen.chunkKey(middleChunkPos.x,middleChunkPos.z);
            roadSegmentLinkMap.computeIfAbsent(chunkKey,key -> new ArrayList<>());

            // 添加进临时列表
            chunkKey_ArrayList.add(chunkKey);
            segmentIndex_ArrayList.add(segmentIndex);
            segment_ArrayList.add(segment);
            i_ArrayList.add(i);
        }

        // NOTE 如果segment列表是空的话则直接返回
        if (segmentIndex_ArrayList.isEmpty()){
            return;
        }

        // STEP[2]: 生成装饰物打点列表
        Object2ObjectOpenHashMap<DecorationEntry, IntOpenHashSet> decorationEntry_ArrayList = new Object2ObjectOpenHashMap<>(BASE_MIDDLE_CAPACITY);
        var allDecorationEntries = new ObjectArrayList<>(this.roadEntry.getOtherCrossRules().values());
        if (this.roadEntry.getMainCrossRule() != null) {
            allDecorationEntries.add(this.roadEntry.mainCrossRule);
        }
        for (CrossRule crossRule : allDecorationEntries){
            for (DecorationEntry decorationEntry : crossRule.decorations()){
                IntRange intRange = decorationEntry.getSpacing();
                IntOpenHashSet intOpenHashSet = new IntOpenHashSet(4);
                // NOTE 开始打点
                int count = segmentIndex_ArrayList.getFirst();
                while (count <= segmentIndex_ArrayList.getLast()){
                    int spacing = intRange.random(serverWorld.random);
                    // 计算随机概率

                    intOpenHashSet.add(count + spacing);

                    count += spacing;
                }
                decorationEntry_ArrayList.put(decorationEntry,intOpenHashSet);
            }
        }

        // STEP[3]: 计算装饰物和包装SegmentData
        for (int x = 0; x<chunkKey_ArrayList.size(); x++) {
            long currentChunkKey = chunkKey_ArrayList.getLong(x);
            int currentI = i_ArrayList.getInt(x);
            int currentSegmentIndex = segmentIndex_ArrayList.getInt(x);
            Records.RoadSegmentPlacement currentSegment = segment_ArrayList.get(x);

            // 获取坐标
            BlockPos segmentMiddlePos = currentSegment.middlePos();
            // 获取前后的坐标
            BlockPos prevPos = roadNodes.get(currentI - 2);
            BlockPos nextPos = roadNodes.get(currentI + 2);

            int averageY = this.getAverageY(currentI,roadNodes,segmentMiddlePos);

            BlockPos averagedPos = new BlockPos(segmentMiddlePos.getX(), averageY, segmentMiddlePos.getZ());

            // 获取装饰物结构
            Pair<BlockPos,DecorationEntry> decorationStructure = getDecoration(
                    averagedPos,
                    currentSegmentIndex,
                    nextPos,
                    prevPos,
                    roadNodes,
                    serverWorld.random,
                    decorationEntry_ArrayList
            );

            if (decorationStructure == null){
                roadSegmentLinkMap.get(currentChunkKey).add(new RoadData.SegmentData(
                        currentI,
                        currentSegmentIndex,
                        currentSegment,
                        RoadData.PlacedDecoration.EMPTY,
                        averageY));
            }else{
                roadSegmentLinkMap.get(currentChunkKey).add(new RoadData.SegmentData(
                        currentI,
                        currentSegmentIndex,
                        currentSegment,
                        new RoadData.PlacedDecoration(
                                decorationStructure.getLeft(),
                                decorationStructure.getRight().getPool(),
                                decorationStructure.getRight().getProbability(),
                                decorationStructure.getRight().getOffset()
                        ),
                        averageY
                ));
            }
        }
    }

    public int getAverageY(int currentI,List<BlockPos> roadNodes,BlockPos segmentMiddlePos){
        int sumY = 0;
        int countY = 0;
        for (int j = currentI - averagingRadius; j <= currentI + averagingRadius; j++) {
            if (j >= 0 && j < roadNodes.size()) {
                BlockPos samplePos = roadNodes.get(j);
                sumY += serverWorld.getTopY(
                        Heightmap.Type.WORLD_SURFACE_WG,
                        samplePos.getX(),
                        samplePos.getZ()
                );
                countY++;
            }
        }
        return countY > 0 ? (sumY + countY / 2) / countY : segmentMiddlePos.getY();
    }

    private Pair<BlockPos,DecorationEntry> getDecoration(
                                      BlockPos placePos,
                                      int segmentIndex,
                                      BlockPos nextPos,
                                      BlockPos prevPos,
                                      List<BlockPos> middleBlockPositions,
                                      Random random,
                                      Object2ObjectOpenHashMap<DecorationEntry,IntOpenHashSet> decorationEntry_ArrayList
    ) {
        int dx = nextPos.getX() - prevPos.getX();
        int dz = nextPos.getZ() - prevPos.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        int normDx = length != 0 ? (int) Math.round(dx / length) : 0;
        int normDz = length != 0 ? (int) Math.round(dz / length) : 0;
        Vec3i directionVector = new Vec3i(normDx, 0, normDz);

        Vec3i orthogonalVector = new Vec3i(-directionVector.getZ(), 0, directionVector.getX());
        boolean isEnd = segmentIndex != middleBlockPositions.size() - 65; // 如果是末尾
        BlockPos shiftedPos;

        // 起始点和末尾点路标,但是暂时不实现
        if (segmentIndex == 65 || segmentIndex == middleBlockPositions.size() - 65) {
            return null;
        }

        // 获取此segment处的生物群系
        Biome biome = this.serverWorld.getChunkManager().getChunkGenerator().getBiomeSource().getBiome(
                BiomeCoords.fromBlock(placePos.getX()),
                BiomeCoords.fromBlock(placePos.getY()),
                BiomeCoords.fromBlock(placePos.getZ()),
                this.serverWorld.getChunkManager().getNoiseConfig().getMultiNoiseSampler()
        ).value();

        CrossRule currentCrossRule = null;
        if (this.roadEntry.otherCrossRules.containsKey(biome)){
            currentCrossRule = this.roadEntry.otherCrossRules.get(biome);
        }else{
            if (this.roadEntry.getMainCrossRule() != null){
                currentCrossRule = this.roadEntry.getMainCrossRule();
            }
        }

        // NOTE 遍历这个道路穿越规则里的装饰物列表
        if (currentCrossRule != null) {
            for (DecorationEntry decorationEntry : currentCrossRule.decorations()){
                if (decorationEntry_ArrayList.containsKey(decorationEntry)){
                    IntOpenHashSet intOpenHashSet = decorationEntry_ArrayList.get(decorationEntry);
                    if (intOpenHashSet.contains(segmentIndex)){
                        boolean leftRoadSide = random.nextBoolean();
                        shiftedPos = leftRoadSide ? placePos.add(orthogonalVector.multiply(2)) : placePos.subtract(orthogonalVector.multiply(2));
                        return new Pair<>(shiftedPos,decorationEntry);
                    }
                }
            }
        }
        return null;
    }

    private static int allowedRoadTypes(Random deterministicRandom) {
        if (ModConfig.allowArtificial && ModConfig.allowNatural){
            return getRandomRoadType(deterministicRandom);
        }
        else if (ModConfig.allowArtificial){
            return 0;
        }
        else if (ModConfig.allowNatural) {
            return 1;
        }
        else {
            return -1;
        }
    }

    private static int getRandomRoadType(Random random) {
        return random.nextBetween(0, 1);
    }

    private static List<BlockState> getRandomNaturalRoadMaterials(Random random, RoadFeatureConfig config) {
        List<List<BlockState>> materialsList = config.getNaturalMaterials();
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static List<BlockState> getRandomArtificialRoadMaterials(Random random, RoadFeatureConfig config) {
        List<List<BlockState>> materialsList = config.getArtificialMaterials();
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static int getRandomWidth(Random random, RoadFeatureConfig config) {
        List<Integer> widthList = config.getWidths();
        return widthList.get(random.nextInt(widthList.size()));
    }
}
