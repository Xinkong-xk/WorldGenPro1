package io.xk.worldgenpro.addon.roadgen.features.roadlogic.api;

import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.features.roadlogic.RoadDirection;
import io.xk.worldgenpro.addon.roadgen.helpers.Records;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.*;

public class RoadPathCalculatorOptimized {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadGen.MOD_ID);

    // 测试
    private final static int neighborDistance = 4;

    // Cache for height values, mapping hashed (x, z) to height (y)
    public static final Map<Long, Integer> heightCache = new ConcurrentHashMap<>(BASE_GIANT_CAPACITY);

    public static int[][] neighborOffsets = new int[][] {
            { neighborDistance, 0 },
            { -neighborDistance, 0 },
            { 0, neighborDistance },
            { 0, -neighborDistance },
            { neighborDistance, neighborDistance },
            { neighborDistance, -neighborDistance },
            { -neighborDistance, neighborDistance },
            { -neighborDistance, -neighborDistance }
    };


    // Helper method to hash coordinates into a single long
    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, ServerWorld serverWorld, int maxSteps
    ) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Object2ObjectOpenHashMap<BlockPos, Node> allNodes = new Object2ObjectOpenHashMap<>(BASE_LARGE_CAPACITY);
        ObjectOpenHashSet<BlockPos> closedSet = new ObjectOpenHashSet<>(BASE_LARGE_CAPACITY);
        Object2ObjectOpenHashMap<BlockPos, ObjectArrayList<BlockPos>> interpolatedSegments = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);

        int startX = snapToGrid(start.getX(),neighborDistance);
        int startZ = snapToGrid(start.getZ(),neighborDistance);
        int endX = snapToGrid(end.getX(),neighborDistance);
        int endZ = snapToGrid(end.getZ(),neighborDistance);

        start = new BlockPos(startX, start.getY(), startZ);
        end = new BlockPos(endX, end.getY(), endZ);

        BlockPos startGround = new BlockPos(start.getX(), heightSampler(start.getX(), start.getZ(), serverWorld), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler(end.getX(), end.getZ(), serverWorld), end.getZ());

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);

        // 如果openSet不为空，并且没有到达最大步数,z则继续循环
        while (!openSet.isEmpty() && maxSteps-- > 0) {
            // 当前节点
            Node current = openSet.poll();
            // 成功找到一条路
            if (current.pos.withY(0).getManhattanDistance(endGround.withY(0)) < neighborDistance * 2) {
                LOGGER.info("成功构建道路 {}" , current.pos);
                return reconstructPath(current, width, interpolatedSegments);
            }

            closedSet.add(current.pos);
            allNodes.remove(current.pos);

            for (int[] offset : neighborOffsets) {
                BlockPos neighborXZ = current.pos.add(offset[0], 0, offset[1]);
                int y = heightSampler(neighborXZ.getX(), neighborXZ.getZ(), serverWorld);
                BlockPos neighborPos = new BlockPos(neighborXZ.getX(), y, neighborXZ.getZ());
                if (closedSet.contains(neighborPos)) continue;

                RegistryEntry<Biome> biomeRegistryEntry = biomeSampler(neighborPos, serverWorld);
                int biomeCost = biomeRegistryEntry.isIn(BiomeTags.IS_RIVER)
                        || biomeRegistryEntry.isIn(BiomeTags.IS_OCEAN)
                        || biomeRegistryEntry.isIn(BiomeTags.IS_DEEP_OCEAN) ? 50 : 0;
                int elevation = Math.abs(y - current.pos.getY());
                if (elevation > 3) {
                    continue;
                }
                int offsetSum = Math.abs(Math.abs(offset[0])) + Math.abs(offset[1]);
                double stepCost = (offsetSum == 2 * neighborDistance) ? 1.5 : 1;
                int terrainStabilityCost = calculateTerrainStability(neighborPos, y, serverWorld);
                if (terrainStabilityCost > 2) {
                    continue;
                }
                int yLevelCost = y == 62 ? 20 : 0;
                double tentativeG = current.gScore + stepCost
                        + elevation * 40
                        + biomeCost * 8
                        + yLevelCost * 8
                        + terrainStabilityCost * 16;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    double h = heuristic(neighborPos, endGround);
                    neighbor = new Node(neighborPos, current, tentativeG, tentativeG + h);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);

                    ObjectArrayList<BlockPos> segmentPoints = getBlockPos(offset, current);
                    interpolatedSegments.put(neighborPos, segmentPoints);
                }
            }
        }
        return Collections.emptyList();
    }

    private static @NotNull ObjectArrayList<BlockPos> getBlockPos(int[] offset, Node current) {
        ObjectArrayList<BlockPos> segmentPoints = new ObjectArrayList<>(neighborDistance);
        for (int i = 1; i < neighborDistance; i++) {
            int interpX = current.pos.getX() + (offset[0] * i) / neighborDistance;
            int interpZ = current.pos.getZ() + (offset[1] * i) / neighborDistance;
            BlockPos interpolated = new BlockPos(interpX, current.pos.getY(), interpZ);
            segmentPoints.add(interpolated);
        }
        return segmentPoints;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        double dxzApprox = Math.abs(dx) + Math.abs(dz) - 0.6 * Math.min(Math.abs(dx), Math.abs(dz));
        return dxzApprox * 30;
    }

    private static int calculateTerrainStability(BlockPos neighborPos, int y, ServerWorld serverWorld) {
        int cost = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos testPos = neighborPos.offset(direction);
            int testY = heightSampler(testPos.getX(), testPos.getZ(), serverWorld);
            int elevation = Math.abs(y - testY);
            cost += elevation;
            if (cost > 2) {
                return Integer.MAX_VALUE;
            }
        }
        return cost;
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(
            Node endNode, int width, Map<BlockPos, ObjectArrayList<BlockPos>> interpolatedPathMap
    ) {
        ObjectArrayList<Node> pathNodes = new ObjectArrayList<>(BASE_LARGE_CAPACITY);
        Node current = endNode;
        while (current != null) {
            pathNodes.addFirst(current); // 每次插入到最前面
            current = current.parent;
        }

        Object2ObjectLinkedOpenHashMap<BlockPos, Set<BlockPos>> roadSegments = new Object2ObjectLinkedOpenHashMap<>(BASE_LARGE_CAPACITY);
        ObjectOpenHashSet<BlockPos> widthCache = new ObjectOpenHashSet<>(BASE_LARGE_CAPACITY);

        for (Node node : pathNodes) {
            BlockPos pos = node.pos;
            ObjectArrayList<BlockPos> interpolated = interpolatedPathMap.getOrDefault(pos, new ObjectArrayList<>(16));
            RoadDirection roadDirection = RoadDirection.X_AXIS;
            if (!interpolated.isEmpty()) {
                BlockPos firstInterpolated = interpolated.getFirst();
                int dx = pos.getX() - firstInterpolated.getX();
                int dz = pos.getZ() - firstInterpolated.getZ();
                // 计算方向
                roadDirection = RoadDirection.fromDelta(dx, dz);

                for (BlockPos interp : interpolated) {
                    Set<BlockPos> widthSetInterp = generateWidth(interp, width / 2, widthCache, roadDirection);
                    roadSegments.put(interp, widthSetInterp);
                }
            }

            Set<BlockPos> widthSet = generateWidth(pos, width / 2, widthCache, roadDirection);
            roadSegments.put(pos, widthSet);
        }

        List<Records.RoadSegmentPlacement> result = new ArrayList<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : roadSegments.entrySet()) {
            result.add(new Records.RoadSegmentPlacement(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return result;
    }

    // Height sampler method
    private static int heightSampler(int x, int z, ServerWorld serverWorld) {
        long key = RoadGen.chunkKey(x, z);
        return heightCache.computeIfAbsent(key, k -> serverWorld.getChunkManager()
                .getChunkGenerator()
                .getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, serverWorld, serverWorld.getChunkManager().getNoiseConfig()));
    }

    // Biome sampler method
    private static RegistryEntry<Biome> biomeSampler(BlockPos pos, ServerWorld serverWorld) {
        return serverWorld.getBiome(pos);
    }

    private static class Node {
        BlockPos pos;
        Node parent;
        double gScore, fScore;

        Node(BlockPos pos, Node parent, double gScore, double fScore) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    /**
     * Align value down to a multiple of precision (power of two).
     */
    public static int snapToGrid(int value, int precision) {
        if ((precision & (precision - 1)) != 0) {
            throw new IllegalArgumentException("precision must be a power of two");
        }
        return value & -precision;
    }

    static Set<BlockPos> generateWidth(BlockPos center, int radius, Set<BlockPos> widthPositionsCache, RoadDirection direction) {
        Set<BlockPos> segmentWidthPositions = new HashSet<>(8);

        int centerX = center.getX();
        int centerZ = center.getZ();
        int y = 0;

        if (direction == RoadDirection.X_AXIS) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = new BlockPos(centerX, y, centerZ + dz);
                if (!widthPositionsCache.contains(pos)) {
                    widthPositionsCache.add(pos);
                    segmentWidthPositions.add(pos);
                }
            }
        } else if (direction == RoadDirection.Z_AXIS) {
            for (int dx = -radius; dx <= radius; dx++) {
                BlockPos pos = new BlockPos(centerX + dx, y, centerZ);
                if (!widthPositionsCache.contains(pos)) {
                    widthPositionsCache.add(pos);
                    segmentWidthPositions.add(pos);
                }
            }
        } else {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (direction == RoadDirection.DIAGONAL_2) {
                        if ((dx == -radius && dz == -radius) || (dx == radius && dz == radius)) {
                            continue;
                        }
                    }
                    if (direction == RoadDirection.DIAGONAL_1) {
                        if ((dx == -radius && dz == radius) || (dx == radius && dz == -radius)) {
                            continue;
                        }
                    }
                    BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
                    if (!widthPositionsCache.contains(pos)) {
                        widthPositionsCache.add(pos);
                        segmentWidthPositions.add(pos);
                    }
                }
            }
        }
        return segmentWidthPositions;
    }
}
