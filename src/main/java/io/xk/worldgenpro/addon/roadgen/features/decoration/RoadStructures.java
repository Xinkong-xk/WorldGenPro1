package io.xk.worldgenpro.addon.roadgen.features.decoration;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.roadgen.api.data.RoadData;
import io.xk.worldgenpro.addon.roadgen.features.RoadFeature;
import io.xk.worldgenpro.addon.roadgen.helpers.StructureTemplatePlacer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.pool.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

public class RoadStructures {

    public static void tryPlaceDecorations(
            ObjectOpenHashSet<RoadData.PlacedDecoration> roadDecorationPlacementPositions,
            StructureWorldAccess worldAccess
    ) {
        // 如果没有可生成的装饰物则直接返回
        if (roadDecorationPlacementPositions.isEmpty()) {
            return;
        }

        for (RoadData.PlacedDecoration placedDecoration : roadDecorationPlacementPositions) {
            BlockPos placePos = placedDecoration.pos();
            String structurePoolName = placedDecoration.pool();

            // NOTE 获取表面坐标
            BlockPos surfacePos = placePos.withY(
                    worldAccess.getTopY(
                            Heightmap.Type.WORLD_SURFACE_WG,
                            placePos.getX(),
                            placePos.getZ()
                    ));

            // 如果是水则放置浮标
            BlockState blockStateAtPos = worldAccess.getBlockState(surfacePos.down());
            if (blockStateAtPos.equals(Blocks.WATER.getDefaultState())) {
                // If it's water, place a buoy
                RoadStructures.placeBuoy(worldAccess, surfacePos);
                // 并且返回
                return;
            }

            // 如果有模板池则尝试生成
            if (structurePoolName != null && !structurePoolName.isEmpty()) {
                ServerWorld serverWorld = worldAccess.toServerWorld();
                // 获取结构模版池
                StructurePool structurePool = WorldGenPro.roadConnectionConfigManager.getStructurePool(structurePoolName);
                // 获取结构池元素
                StructurePoolElement structurePoolElement = structurePool.getRandomElement(serverWorld.getRandom());

                // 放置结构池元素
                serverWorld.getServer().executeSync(()->{
                    // 如果不可以放置则返回
                    if (!placeAllowed(worldAccess, surfacePos)) return;

                    // 放置结构
                    LOGGER.info("成功放置了装饰在: {},{}", surfacePos,structurePoolName);
                    StructureTemplatePlacer.placeStructure(
                            structurePoolElement,
                            serverWorld.getStructureTemplateManager(),
                            surfacePos.add(
                                    placedDecoration.offset().x(),
                                    placedDecoration.offset().y(),
                                    placedDecoration.offset().z()
                            ), // 添加偏移值
                            serverWorld
                    );
                });
            }
        }
    }

    protected static boolean placeAllowed( StructureWorldAccess world,BlockPos surfacePos) {
        BlockState blockStateBelow = world.getBlockState(surfacePos.down());

        boolean belowInvalid = blockStateBelow.isOf(Blocks.WATER)
                || blockStateBelow.isOf(Blocks.LAVA)
                || blockStateBelow.isIn(BlockTags.LOGS)
                || RoadFeature.dontPlaceHere.contains(blockStateBelow.getBlock());

        return !belowInvalid;
    }

    public static void placeBuoy(StructureWorldAccess worldAccess, BlockPos surfacePos) {
        worldAccess.setBlockState(surfacePos.down(), Blocks.SPRUCE_PLANKS.getDefaultState(), 3);
        worldAccess.setBlockState(surfacePos, Blocks.SPRUCE_FENCE.getDefaultState(), 3);
    }
}
