package io.xk.worldgenpro.addon.roadgen.features;

import com.mojang.serialization.Codec;
import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.config.ModConfig;
import io.xk.worldgenpro.addon.roadgen.api.data.RoadData;
import io.xk.worldgenpro.addon.roadgen.features.config.RoadFeatureConfig;
import io.xk.worldgenpro.addon.roadgen.features.decoration.RoadStructures;
import io.xk.worldgenpro.addon.roadgen.features.roadlogic.api.RoadPathCalculatorOptimized;
import io.xk.worldgenpro.addon.roadgen.helpers.Records;
import io.xk.worldgenpro.addon.roadgen.persistence.attachments.WorldDataAttachment;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadGen.MOD_ID);

    public static final Set<Block> dontPlaceHere = new HashSet<>();
    static {
        dontPlaceHere.add(Blocks.PACKED_ICE);
        dontPlaceHere.add(Blocks.ICE);
        dontPlaceHere.add(Blocks.BLUE_ICE);
        dontPlaceHere.add(Blocks.TALL_SEAGRASS);
        dontPlaceHere.add(Blocks.MANGROVE_ROOTS);
    }

    public static final RegistryKey<PlacedFeature> ROAD_FEATURE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(RoadGen.MOD_ID, "road_feature_placed"));
    public static final RegistryKey<ConfiguredFeature<?,?>> ROAD_FEATURE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(RoadGen.MOD_ID, "road_feature"));
    public static final Feature<RoadFeatureConfig> ROAD_FEATURE = new RoadFeature(RoadFeatureConfig.CODEC);
    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<RoadFeatureConfig> context) {

        // 如果缓存过大则清理
        if (RoadPathCalculatorOptimized.heightCache.size() > 100_000){
            RoadPathCalculatorOptimized.heightCache.clear();
        }

        // 获取服务器世界对象
        ServerWorld serverWorld = context.getWorld().toServerWorld();
        // 获取结构访问器
        StructureWorldAccess structureWorldAccess = context.getWorld();

        // 获取结构地点寻找数据
        List<RoadData> roadDataList = structureWorldAccess.toServerWorld().getAttached(WorldDataAttachment.ROAD_DATA_LIST);

        // 如果没有则直接返回
        if (roadDataList == null || roadDataList.isEmpty()) {
            return false;
        }

        // 尝试寻找新的结构连接（？）
        // tryFindNewStructureConnection(villageLocations, serverWorld,context);

        // 路旁装饰物缓存表
        ObjectOpenHashSet<RoadData.PlacedDecoration> roadDecorationCache = new ObjectOpenHashSet<>();

        // 生成道路的逻辑
        runRoadLogic(structureWorldAccess, context, roadDecorationCache);

        // 路结构尝试在这里生成
        RoadStructures.tryPlaceDecorations(roadDecorationCache,context.getWorld());

        // 认为成功生成了
        return true;
    }


    private void tryFindNewStructureConnection(List<BlockPos> villageLocations, ServerWorld serverWorld,FeatureContext<RoadFeatureConfig> context) {
        serverWorld.getServer().execute(() -> {
            FeatureConnectionListener.searchAndCreateConnect(context.getWorld().toServerWorld(),new ChunkPos(context.getOrigin()));
        });
    }


    // 执行道路逻辑
    private void runRoadLogic(StructureWorldAccess structureWorldAccess,
                              FeatureContext<RoadFeatureConfig> context,
                              ObjectOpenHashSet<RoadData.PlacedDecoration> roadDecorationCache) {
        // 半径
        int averagingRadius = ModConfig.averagingRadius;

        // 获取道路数据列表
        List<RoadData> roadDataList = structureWorldAccess.toServerWorld().getAttached(WorldDataAttachment.ROAD_DATA_LIST);
        // 如果道路数据列表不存在则直接返回
        if (roadDataList == null) return;

        // 获取此区块的原点
        ChunkPos currentChunkPos = new ChunkPos(context.getOrigin());
        Set<BlockPos> posAlreadyContainsSegment = new ObjectOpenHashSet<>();

        for (RoadData data : roadDataList) {

            int roadType = data.getRoadType();
            List<BlockState> materials = data.getMaterials();

            // NOTE [优化] 预构建的替代集合
            Object2ObjectLinkedOpenHashMap<Long, List<RoadData.SegmentData>> roadSegmentLinkMap = data.getRoadSegmentLinkMap();

            // NOTE [检测] 如果道路segment链表是空的则跳过
            if (roadSegmentLinkMap.isEmpty()) continue;

            // NOTE [优化] 预构建的替代列表
            List<BlockPos> middlePositions = data.getRoadNodes();

            // 获取当前区块的区块键[Long]
            long chunkKey = RoadGen.chunkKey(currentChunkPos.x,currentChunkPos.z);

            if (roadSegmentLinkMap.containsKey(chunkKey)) {
                List<RoadData.SegmentData> segmentDatas = roadSegmentLinkMap.get(chunkKey);    // 获取segmentData列表,表示这个区块内所有的segmentData

                // 遍历这个列表
                for (RoadData.SegmentData segmentData : segmentDatas) {
                    // int segmentIndex = segmentData.segmentIndex();                          // 获取index
                    int i = segmentData.i();                                                // 获取i

                    // 如果已经包含了则跳过
                    if (posAlreadyContainsSegment.contains(middlePositions.get(i))) continue;

                    // 获取segment
                    Records.RoadSegmentPlacement segment = segmentData.placement();
                    // 获取middle坐标
                    BlockPos segmentMiddlePos = segment.middlePos();

                    // DEBUG 测试输出
                    // LOGGER.info("生成了道路地物在区块: {}",currentChunkPos);

                    // 添加装饰物
                    if (segmentData.placedDecoration() != RoadData.PlacedDecoration.EMPTY){
                        // 生成随机数检查概率
                        float p = segmentData.placedDecoration().p();
                        if (context.getRandom().nextFloat() < p) {
                            roadDecorationCache.add(segmentData.placedDecoration());
                        }
                    }

                    // 放置道路方块
                    for (BlockPos widthBlock : segment.positions()) {
                        BlockPos correctedYPos = new BlockPos(widthBlock.getX(), segmentData.averageY(), widthBlock.getZ());
                        placeOnSurface(structureWorldAccess, correctedYPos, materials, roadType, context.getRandom());
                    }

                    // NOTE 添加进已经包含的segment集合
                    posAlreadyContainsSegment.add(segmentMiddlePos);

                    // NOTE 删除已经放置的地物数据
                    roadSegmentLinkMap.remove(chunkKey);
                }
            }
        }
    }


    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placePos, List<BlockState> material, int natural, Random random) {
        double naturalBlockChance = 0.5;
        BlockPos surfacePos = placePos;
        if (natural == 1 || ModConfig.averagingRadius == 0) {
            surfacePos = structureWorldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, placePos);
        }
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(structureWorldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, surfacePos).down());
        // place road
        if (natural == 0 || random.nextDouble() < naturalBlockChance) {
            placeRoadBlock(structureWorldAccess, blockStateAtPos, surfacePos, material, random);
        }
    }

    private void placeRoadBlock(StructureWorldAccess structureWorldAccess, BlockState blockStateAtPos, BlockPos surfacePos, List<BlockState> materials, Random deterministicRandom) {
        // If not water, just place the road
        if (!placeAllowedCheck(blockStateAtPos.getBlock())
                || (!structureWorldAccess.getBlockState(surfacePos.down()).isOpaque())
                && !structureWorldAccess.getBlockState(surfacePos.down(2)).isOpaque()
                //&& !structureWorldAccess.getBlockState(surfacePos.down(3)).isOpaque())
                //|| structureWorldAccess.getBlockState(surfacePos.up(3)).isOpaque()
        ) {
            return;
        }
        BlockState material = materials.get(deterministicRandom.nextInt(materials.size()));
        setBlockState(structureWorldAccess, surfacePos.down(), material);

        for (int i = 0; i < 3; i++) {
            BlockState blockStateUp = structureWorldAccess.getBlockState(surfacePos.up(i));
            if (!blockStateUp.getBlock().equals(Blocks.AIR) && !blockStateUp.isIn(BlockTags.LOGS) && !blockStateUp.isIn(BlockTags.FENCES)) {
                setBlockState(structureWorldAccess, surfacePos.up(i), Blocks.AIR.getDefaultState());
            }
            else {
                break;
            }
        }

        BlockPos belowPos1 = surfacePos.down(2);
        BlockState belowState1 = structureWorldAccess.getBlockState(belowPos1);

        if (belowState1.getBlock().equals(Blocks.GRASS_BLOCK)) {
            setBlockState(structureWorldAccess, belowPos1, Blocks.DIRT.getDefaultState());
        }
    }

    private boolean placeAllowedCheck (Block blockToCheck) {
        return !(dontPlaceHere.contains(blockToCheck)
                || blockToCheck.getDefaultState().isIn(BlockTags.LEAVES)
                || blockToCheck.getDefaultState().isIn(BlockTags.LOGS)
                || blockToCheck.getDefaultState().isIn(BlockTags.UNDERWATER_BONEMEALS)
                || blockToCheck.getDefaultState().isIn(BlockTags.WOODEN_FENCES)
                || blockToCheck.getDefaultState().isIn(BlockTags.PLANKS)
        );
    }
}

