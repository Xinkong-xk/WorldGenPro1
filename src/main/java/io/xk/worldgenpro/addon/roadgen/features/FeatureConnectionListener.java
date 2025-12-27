package io.xk.worldgenpro.addon.roadgen.features;

import com.mojang.datafixers.util.Pair;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.roadgen.RoadConnectionConfigManager;
import io.xk.worldgenpro.addon.roadgen.api.GridGraph;
import io.xk.worldgenpro.addon.roadgen.api.data.RegionStructureMap;
import io.xk.worldgenpro.addon.roadgen.helpers.Records;
import io.xk.worldgenpro.addon.roadgen.helpers.WorldGenProStructureLocator;
import io.xk.worldgenpro.addon.roadgen.persistence.attachments.WorldDataAttachment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;
import static io.xk.worldgenpro.addon.roadgen.helpers.StructureConnector.cachedStructureConnections;

public class FeatureConnectionListener {
    @Unique
    public static int getWorldMiddleY(ServerWorld world) {
        return world.getBottomY() + world.getHeight() / 2;
    }

    @Unique
    public static void cacheNewConnection(ServerWorld serverWorld, BlockPos from, BlockPos to){
        // 创建了一个连接
        LOGGER.info("创建了一个连接在:{} -> {}",from.toString(),to.toString());
        List<Records.StructureConnection> connections = new ArrayList<>(
                serverWorld.getAttachedOrCreate(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new)
        );
        Records.StructureConnection structureConnection = new Records.StructureConnection(from, to,"");
        connections.add(structureConnection);
        serverWorld.setAttached(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
        cachedStructureConnections.add(structureConnection);
    }

    @Unique
    public static void searchAndCreateConnect(ServerWorld world, ChunkPos chunkPos){
        long chunkKey = RoadConnectionConfigManager.chunkKey(chunkPos.x, chunkPos.z);
        if (WorldGenPro.roadConnectionConfigManager.skipChunks.contains(chunkKey)){
            WorldGenPro.roadConnectionConfigManager.skipChunks.remove(chunkKey);
            return;
        }

        // ================= 开始搜索 =================

        // 检查此区块所在region是否被加载
        RegionStructureMap regionStructureMap = WorldGenPro.roadConnectionConfigManager.getRegionStructureMap(world.getRegistryKey());
        // 遍历所有已配置的连接结构对
        for (String fromStructureSetName : WorldGenPro.roadConnectionConfigManager.FromStructureSetNameToPair.keySet()) {
            // 获取from结构集对象
            StructureSet fromStructureSet = WorldGenPro.roadConnectionConfigManager.getStructureSet(fromStructureSetName);
            // 如果是散布结构
            if (fromStructureSet.placement() instanceof RandomSpreadStructurePlacement randomSpreadStructurePlacement) {
                int spacing = randomSpreadStructurePlacement.getSpacing();
                // 计算regionID
                long regionId = RoadConnectionConfigManager.getRegionId(chunkPos,spacing);
                // 检查这个结构集的这个region是否已经被加载过(无论是否生成了结构),如果没有,则进行下一步:使用双端三次搜索法
                GridGraph fromGridGraph = regionStructureMap.regionMap.computeIfAbsent(fromStructureSetName, s -> new GridGraph(spacing));
                if (! fromGridGraph.hasVisitedCell(regionId)){
                    // 输出测试
                    LOGGER.info("[RoadGen:Search]: Trying to find New Structure Connection At {}",regionId);
                    // (1) 进行第一次搜索,from端,from找to
                    // 搜索from结构(记录)
                    Pair<BlockPos, RegistryEntry<Structure>> fromResultPair = WorldGenProStructureLocator.locateStructure(
                            world,
                            fromStructureSetName,
                            fromStructureSet,
                            chunkPos.getCenterAtY(getWorldMiddleY(world)),
                            100,
                            true,
                            fromGridGraph,
                            true,
                            chunkPos,
                            true,
                            null
                            );
                    // (2) 如果成功找到了,则开始第二次搜索
                    if (fromResultPair != null) {
                        String toStructureSetName = WorldGenPro.roadConnectionConfigManager.FromStructureSetNameToPair.get(fromStructureSetName).getRight();
                        // 获取to结构集对象
                        StructureSet toStructureSet = WorldGenPro.roadConnectionConfigManager.getStructureSet(toStructureSetName);
                        // 获取搜索时的中心坐标
                        BlockPos centerPos = fromResultPair.getFirst();
                        GridGraph toGridGraph = regionStructureMap.regionMap.get(toStructureSetName);
                        // 搜索to结构(记录)
                        Pair<BlockPos, RegistryEntry<Structure>> toResultPair = WorldGenProStructureLocator.locateStructure(
                                world,
                                toStructureSetName,
                                toStructureSet,
                                centerPos,
                                100,
                                true,
                                toGridGraph,
                                true,
                                chunkPos,
                                true,
                                null
                                );
                        // 如果成功找到to结构,则开始第三次搜索
                        if (toResultPair != null) {
                            BlockPos toResultCenterPos = toResultPair.getFirst();
                            // 先连接from.1和to
                            cacheNewConnection(world,fromResultPair.getFirst(),toResultPair.getFirst());
                            // 遍历生成连接
                            for (int i = 0; i<5; i++){
                                Pair<BlockPos, RegistryEntry<Structure>> newFromResultPair = WorldGenProStructureLocator.locateStructure(
                                        world,
                                        fromStructureSetName,
                                        fromStructureSet,
                                        toResultCenterPos,
                                        100,
                                        true,
                                        fromGridGraph,
                                        true,
                                        chunkPos,
                                        true,
                                        null
                                        );
                                if (newFromResultPair != null) {
                                    cacheNewConnection(world,newFromResultPair.getFirst(),toResultPair.getFirst());
                                    // 并且记录这个结构为不能继续迭代连接
                                    ChunkPos newFromChunkPos = new ChunkPos(newFromResultPair.getFirst());
                                    WorldGenPro.roadConnectionConfigManager.skipChunks.add(
                                            RoadConnectionConfigManager.chunkKey(newFromChunkPos.x, newFromChunkPos.z)
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
