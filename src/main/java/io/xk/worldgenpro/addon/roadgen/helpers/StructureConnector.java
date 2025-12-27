package io.xk.worldgenpro.addon.roadgen.helpers;

import io.xk.worldgenpro.addon.roadgen.persistence.attachments.WorldDataAttachment;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

public class StructureConnector {

    public static Queue<Records.StructureConnection> cachedStructureConnections = new ArrayDeque<>();
    
    public static void cacheNewConnection(ServerWorld serverWorld,String structureToLocate, boolean locateAtPlayer) {
        StructureLocator.locateConfiguredStructure(serverWorld, structureToLocate,1, locateAtPlayer);

        List<BlockPos> villagePosList = serverWorld.getAttachedOrCreate(WorldDataAttachment.STRUCTURE_LOCATIONS,()->new Records.StructureLocationData(List.of())).structureLocations();
        if (villagePosList == null || villagePosList.size() < 2) {
            return;
        }
        createNewStructureConnection(serverWorld);
    }

    private static void createNewStructureConnection(ServerWorld serverWorld) {
        // 获取村庄坐标列表
        List<BlockPos> villagePosList = serverWorld.getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS).structureLocations();

        // 获取最后一个
        BlockPos latestVillagePos = villagePosList.get(villagePosList.size() - 1);

        // 获取结构定位数据
        Records.StructureLocationData structureLocationData = serverWorld.getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS);

        // 获取世界里的结构定位数据
        List<BlockPos> worldStructureLocations = structureLocationData.structureLocations();

        // 获取最后一个村庄最近的结构
        BlockPos closestVillage = findClosestStructure(latestVillagePos, worldStructureLocations);

        // 如果不是空的
        if (closestVillage != null) {
            List<Records.StructureConnection> connections = new ArrayList<>(
                    serverWorld.getAttachedOrCreate(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new)
            );
            if (!connectionExists(connections, latestVillagePos, closestVillage)) {
                // 创建了一个连接
                LOGGER.info("创建了一个连接在:{} -> {}",latestVillagePos.toString(),closestVillage.toString());
                Records.StructureConnection structureConnection = new Records.StructureConnection(latestVillagePos, closestVillage,"");
                connections.add(structureConnection);
                serverWorld.setAttached(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
                cachedStructureConnections.add(structureConnection);
            }
        }
    }

    private static boolean connectionExists(List<Records.StructureConnection> existingConnections, BlockPos a, BlockPos b) {
        for (Records.StructureConnection connection : existingConnections) {
            if ((connection.from().equals(a) && connection.to().equals(b)) ||
                    (connection.to().equals(b) && connection.from().equals(a))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findClosestStructure(BlockPos currentVillage, List<BlockPos> allVillages) {
        BlockPos closestVillage = null;
        double minDistance = Double.MAX_VALUE;

        for (BlockPos village : allVillages) {
            if (!village.equals(currentVillage)) {
                double distance = currentVillage.getSquaredDistance(village);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestVillage = village;
                }
            }
        }
        return closestVillage;
    }
}
