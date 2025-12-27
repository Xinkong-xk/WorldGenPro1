package io.xk.worldgenpro.addon.roadgen.events;



import com.mojang.datafixers.util.Pair;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.roadgen.api.GridGraph;
import io.xk.worldgenpro.addon.roadgen.codec.config.CountablePoint;
import io.xk.worldgenpro.addon.roadgen.codec.config.RoadEntry;
import io.xk.worldgenpro.addon.roadgen.codec.config.Point;
import io.xk.worldgenpro.addon.roadgen.api.data.RegionStructureMap;
import io.xk.worldgenpro.addon.roadgen.features.RoadFeature;
import io.xk.worldgenpro.addon.roadgen.features.config.RoadFeatureConfig;
import io.xk.worldgenpro.addon.roadgen.features.roadlogic.Road;
import io.xk.worldgenpro.addon.roadgen.features.roadlogic.api.RoadPathCalculatorOptimized;
import io.xk.worldgenpro.addon.roadgen.helpers.Records;
import io.xk.worldgenpro.addon.roadgen.helpers.StructureConnector;
import io.xk.worldgenpro.addon.roadgen.helpers.WorldGenProStructureLocator;
import io.xk.worldgenpro.addon.roadgen.persistence.attachments.WorldDataAttachment;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.xk.worldgenpro.WorldGenPro.MOD_ID;


public class ModEventHandler {

    private static final int THREAD_COUNT= 7;

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    private static final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    public static void createNewStructureConnection(ServerWorld serverWorld,BlockPos from,BlockPos to,String roadEntryKey) {
        List<Records.StructureConnection> connections = new ArrayList<>(
                serverWorld.getAttachedOrCreate(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new)
        );
        Records.StructureConnection structureConnection = new Records.StructureConnection(to, from,roadEntryKey);
        connections.add(structureConnection);
        serverWorld.setAttached(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
        StructureConnector.cachedStructureConnections.add(structureConnection);
    }

    public static void register() {

        // 服务器世界加载事件
        ServerWorldEvents.LOAD.register((server, serverWorld) -> {
            // 重新启动执行器如果需要的话
            restartExecutorIfNeeded();

            // 获取RegionStructureMap对象
            RegionStructureMap regionStructureMap = WorldGenPro.roadConnectionConfigManager.getRegionStructureMap(serverWorld.getRegistryKey());

            // 遍历所有已配置的连接结构集对
            for (RoadEntry roadEntry : WorldGenPro.roadConnectionConfigManager.entries) {

                // 获取from结构集名称,结构集对象
                Point fromPoint = roadEntry.getFrom();
                String fromStructureSetName = fromPoint.structure_set().toString();
                StructureSet fromStructureSet = WorldGenPro.roadConnectionConfigManager.getStructureSet(fromStructureSetName);

                // 获取to结构集名称,结构集对象,连接道路数
                CountablePoint toPoint = roadEntry.getTo();
                String toStructureSetName = toPoint.structure_set().toString();
                StructureSet toStructureSet = WorldGenPro.roadConnectionConfigManager.getStructureSet(toStructureSetName);
                int roadCount = toPoint.roadCount();
                int initialLocatingCount = roadEntry.getConnection().initialLocatingCount();

                // 获取To结构集的GridGraph
                GridGraph toStructureSetGridGraph = regionStructureMap.getGridGraph(toStructureSetName,toStructureSet);
                // 获取From结构集的GridGraph
                GridGraph fromStructureSetGridGraph = regionStructureMap.getGridGraph(fromStructureSetName,fromStructureSet);

                // 需要跳过的To结构region表
                LongOpenHashSet foundRegions = new LongOpenHashSet();

                // 如果from结构集跟to结构集相同则使用相同的foundRegions
                boolean isSame = fromStructureSet == toStructureSet;

                // 如果是随机散布类型结构
                if (toStructureSet.placement() instanceof RandomSpreadStructurePlacement randomSpreadStructurePlacement) {
                    // NOTE 获取这个结构集的放置规则,则找到initialLocatingCount个to结构集,并且每一个都生成roadCount个道路
                    BlockPos spawnPos = serverWorld.getSpawnPos();

                    for (int i=0; i<initialLocatingCount; i++) {
                        Pair<BlockPos, RegistryEntry<Structure>> findToStructureResult = WorldGenProStructureLocator.locateStructure(
                                serverWorld,
                                toStructureSetName,
                                toStructureSet,
                                spawnPos,
                                500,
                                true, // 跳过已引用的结构
                                toStructureSetGridGraph,
                                true,
                                null,
                                false,
                                isSame ? foundRegions : null // [!] 如果from和to是相同的,则使用foundRegions来避免to和from定位到同一个结构
                        );

                        // 如果找到了to结构
                        if (findToStructureResult != null) {
                            // 找到的to结构坐标
                            BlockPos toStructurePos = findToStructureResult.getFirst();

                            // 开始搜索road_count个from结构
                            for (int x=0; x<roadCount; x++){
                                Pair<BlockPos, RegistryEntry<Structure>> findFromStructureResult = WorldGenProStructureLocator.locateStructure(
                                        serverWorld,
                                        fromStructureSetName,
                                        fromStructureSet,
                                        toStructurePos,
                                        500,
                                        false,
                                        fromStructureSetGridGraph,
                                        true,
                                        null,
                                        false,
                                        foundRegions
                                );
                                // 如果也找到了,则进行道路连接
                                if (findFromStructureResult != null) {
                                    BlockPos fromStructurePos = findFromStructureResult.getFirst();
                                    // 创建新的结构连接
                                    createNewStructureConnection(serverWorld,fromStructurePos,toStructurePos,roadEntry.getRoadEntryKey());
                                    // 尝试生成新的道路
                                    tryGenerateNewRoads(serverWorld,true, 20000);
                                }
                            }
                        }
                    }
                }
            }
        });

        ServerWorldEvents.UNLOAD.register((server, serverWorld) -> {
            Future<?> task = runningTasks.remove(serverWorld.getRegistryKey().getValue().toString());
            if (task != null && !task.isDone()) {
                task.cancel(true);
                LOGGER.debug("Aborted running road task for world: {}", serverWorld.getRegistryKey().getValue());
            }
        });

        ServerTickEvents.START_WORLD_TICK.register((serverWorld) -> {
            if (!serverWorld.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) return;
            tryGenerateNewRoads(serverWorld, true, 5000);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            RoadPathCalculatorOptimized.heightCache.clear();
            runningTasks.values().forEach(future -> future.cancel(true));
            runningTasks.clear();
            executor.shutdownNow();
            LOGGER.debug("SettlementRoads: ExecutorService shut down.");
        });
    }

    public static void tryGenerateNewRoads(ServerWorld serverWorld, Boolean async, int steps) {
        if (!StructureConnector.cachedStructureConnections.isEmpty()) {
            // 拿出一个
            Records.StructureConnection structureConnection = StructureConnector.cachedStructureConnections.poll();
            ConfiguredFeature<?, ?> feature = serverWorld.getRegistryManager()
                    .get(RegistryKeys.CONFIGURED_FEATURE)
                    .get(RoadFeature.ROAD_FEATURE_KEY);

            if (feature != null && feature.config() instanceof RoadFeatureConfig roadConfig) {
                if (async) {
                    Future<?> future = executor.submit(() -> {
                        try {
                            new Road(serverWorld, structureConnection, roadConfig).generateRoad(steps);
                        } catch (Exception e) {
                            LOGGER.error("Error generating road", e);
                        }
                    });
                    runningTasks.put(serverWorld.getRegistryKey().getValue().toString(), future);
                }
                else {
                    new Road(serverWorld, structureConnection, roadConfig).generateRoad(steps);
                }
            }
        }
    }

    private static void restartExecutorIfNeeded() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newFixedThreadPool(THREAD_COUNT);
            LOGGER.debug("SettlementRoads: ExecutorService restarted.");
        }
    }
}
