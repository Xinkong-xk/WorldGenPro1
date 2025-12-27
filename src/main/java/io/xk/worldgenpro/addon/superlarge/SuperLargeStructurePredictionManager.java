package io.xk.worldgenpro.addon.superlarge;

import com.mojang.datafixers.util.Either;
import io.xk.worldgenpro.vanilla.enhance.chunk.TASChunkLoader;
import io.xk.worldgenpro.vanilla.enhance.config.SuperLargeConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashCallable;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 超大型结构预测管理器
 * 保存结构：Map<Structure, Map<RegistryKey<World>, Map<Long, RegionPrediction>>>
 */
public class SuperLargeStructurePredictionManager {

    // 文件名
    public static final String FILE_NAME = "super_large_structure_predictions.dat";

    // 总缓存表
    public final Map<Structure, Map<RegistryKey<World>, Map<Long, RegionPrediction>>> cache
            = new ConcurrentHashMap<>();

    // 结构Placement缓存表
    public final ConcurrentHashMap<Structure,RandomSpreadStructurePlacement> structurePlacementCache = new ConcurrentHashMap<>();

    // 超大型结构扫描范围缓存表
    public final ConcurrentHashMap<Structure,Integer> structureScanRangeCache = new ConcurrentHashMap<>();

    // 保存路径
    public final Path savePath;

    // 结构注册表
    public final net.minecraft.registry.Registry<Structure> structureRegistry;

    public Registry<StructureSet> structureSetRegistry;

    public SuperLargeStructurePredictionManager(Path worldDir,
                                                net.minecraft.registry.Registry<Structure> registry) {
        // 直接放在存档根目录，例如 world/super_large_structure_predictions.dat
        this.savePath = worldDir.resolve(FILE_NAME);
        this.structureRegistry = registry;
    }

    /**
     * 添加一个超大型结构
     * @param jigsawStructure jigsaw结构对象
     * @param superLargeConfig 超大型结构生成配置
     * @param server 服务器
     */
    public void addSuperLargeStructure(JigsawStructure jigsawStructure, SuperLargeConfig superLargeConfig, MinecraftServer server) {
        // 对每一个维度都创建缓存表
        cache.put(jigsawStructure, Util.make(new ConcurrentHashMap<>(),map->{
            for (ServerWorld world : server.getWorlds()) {
                RegistryKey<World> worldRegistryKey = world.getRegistryKey();
                map.put(worldRegistryKey,new ConcurrentHashMap<>());
            }
        }));

        // 缓存超大型结构扫描范围
        int chunkRange = (superLargeConfig.getScanRange() + 15) / 16;  // 经典写法，等效于向上取整(blockRange / 16)
        structureScanRangeCache.put(jigsawStructure, chunkRange);

        // 缓存超大型结构的placement
        getStructurePlacement(jigsawStructure,server);
    }

    public Map<Long, RegionPrediction> getRegionMap(Structure structure, RegistryKey<World> dim) {
        return cache
                .computeIfAbsent(structure, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(dim, d -> new ConcurrentHashMap<>());
    }

    public static long getRegionId(int chunkX, int chunkZ, int spacing) {
        int rx = Math.floorDiv(chunkX, spacing);
        int rz = Math.floorDiv(chunkZ, spacing);
        return (((long) rx) << 32) ^ (rz & 0xffffffffL);
    }

    public static long getRegionId(ChunkPos chunkPos, int spacing) {
        int rx = Math.floorDiv(chunkPos.x, spacing);
        int rz = Math.floorDiv(chunkPos.z, spacing);
        return (((long) rx) << 32) ^ (rz & 0xffffffffL);
    }

    public RegionPrediction getOrPredict(
            Structure structure,
            RegistryKey<World> dim,
            long regionId,
            Function<Long, RegionPrediction> predictor
    ) {
        Map<Long, RegionPrediction> regionMap = getRegionMap(structure, dim);

        RegionPrediction existing = regionMap.get(regionId);
        if (existing != null) return existing;

        return regionMap.computeIfAbsent(regionId, predictor);
    }

    public RandomSpreadStructurePlacement getStructurePlacement(Structure structure, MinecraftServer server) {
        if (structureSetRegistry == null){
            structureSetRegistry = server.getRegistryManager().get(RegistryKeys.STRUCTURE_SET);
        }

        return structurePlacementCache.computeIfAbsent(structure,structure1 -> {
            for (StructureSet structureSet : structureSetRegistry) { // 这里直接是 Iterable
                for (StructureSet.WeightedEntry structureEntry : structureSet.structures()) {
                    if (structureEntry.structure().value() == structure) {
                        if (structureSet.placement() instanceof RandomSpreadStructurePlacement randomSpreadStructurePlacement){
                            return randomSpreadStructurePlacement;
                        }
                    }
                }
            }
            return null;
        });
    }

    public void putPrediction(
            Structure structure,
            RegistryKey<World> dim,
            long regionId,
            RegionPrediction result
    ) {
        getRegionMap(structure, dim).put(regionId, result);
    }


    public List<ChunkPos> predictSurroundingRegions(ChunkPos centerPos, ServerWorld world) {
        // 返回所有需要被触发加载的区块列表
        List<ChunkPos> result = new ObjectArrayList<>();

        // 遍历所有超大型结构
        for (Structure structure : cache.keySet()) {

            // 获取此维度下的子缓存表
            Map<Long, RegionPrediction> regionMap = getRegionMap(structure, world.getRegistryKey());
            // 获取结构的placement
            RandomSpreadStructurePlacement placement = getStructurePlacement(structure, world.getServer());

            if (placement == null) continue;

            // 获取此结构的spacing
            int spacing = placement.getSpacing();
            // 获取此结构的扫描范围
            int scanRange = structureScanRangeCache.get(structure);
            // 计算此区块所在的region
            int centerRegionX = Math.floorDiv(centerPos.x, spacing);
            int centerRegionZ = Math.floorDiv(centerPos.z, spacing);

            // 遍历检查周围 3×3 region
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // 计算被检查的region坐标
                    int regionX = centerRegionX + dx;
                    int regionZ = centerRegionZ + dz;
                    // 计算regionId
                    long regionId = (((long) regionX) << 32) ^ (regionZ & 0xffffffffL);

                    // 1. 检查是否被预测过
                    RegionPrediction prediction = regionMap.get(regionId);
                    // 如果预测过且已生成，则跳过
                    if (prediction != null && prediction.isGenerated) {
                        continue;
                    }

                    // 2. 如果没有预测，则先预测
                    if (prediction == null) {
                        // 计算一个大致在被预测region中心位置的区块坐标,避免落在region边缘
                        int cx = regionX * spacing + spacing / 2;
                        int cz = regionZ * spacing + spacing / 2;
                        ChunkPos startChunk = placement.getStartChunk(world.getSeed(), cx, cz);
                        prediction = new RegionPrediction(startChunk);
                        regionMap.put(regionId, prediction);
                    }

                    // 如果预测后发现不会生成（startChunk == null）
                    ChunkPos startChunk = prediction.startChunk();
                    if (startChunk == null) {
                        continue;
                    }

                    // 3. 检查距离(检查切比雪夫距离)
                    int dxChunk = Math.abs(startChunk.x - centerPos.x);
                    int dzChunk = Math.abs(startChunk.z - centerPos.z);
                    int dist = Math.max(dxChunk, dzChunk);

                    // 4. 若在生成范围内，则加入并标记生成
                    if (dist <= scanRange) {
                        result.add(startChunk);
                        prediction.isGenerated = true; // 标记为已生成
                        System.out.println("[CHECK] -> " + startChunk.getBlockPos(0,0,0) + " distance:" + dist);
                    }
                }
            }
        }

        return result;
    }

    public void onChunkLoad(StructureWorldAccess worldAccess, ServerWorld world, StructureAccessor structureAccessor, Chunk chunk) {
        List<ChunkPos> result = predictSurroundingRegions(chunk.getPos(), world);
        // addStructureReferences(worldAccess,structureAccessor,chunk, TASChunkLoader.loadChunksSync(world, result, ChunkStatus.STRUCTURE_STARTS));

        // 异步加载 chunk
        TASChunkLoader.loadChunksAsync(world, result, ChunkStatus.STRUCTURE_STARTS)
                .thenAcceptAsync(chunks -> {
                    // 回到主线程执行
                    addStructureReferences(worldAccess, structureAccessor, chunk, chunks);
                    System.out.println("check: " + chunk.getPos() + " -> " + result);
                }, world.getServer()); // world.getServer() executor 确保在主线程执行

        System.out.println("check: "+chunk.getPos().toString()+" -> "+result.toString());
    }

    public void onChunkLoad_2(StructureWorldAccess worldAccess, ServerWorld world, StructureAccessor structureAccessor, Chunk chunk) {
        List<ChunkPos> positions = predictSurroundingRegions(chunk.getPos(), world);

        // 异步加载 chunk，只做只读操作
        TASChunkLoader.loadChunksAsync(world, positions, ChunkStatus.STRUCTURE_STARTS)
                .thenAcceptAsync(eithers -> {
                    // 在异步线程中创建 snapshot
                    List<Map<Structure, StructureStart>> snapshots = new ArrayList<>();
                    for (Either<Chunk, ChunkHolder.Unloaded> e : eithers) {
                        e.left().ifPresent(chunk2 -> snapshots.add(new HashMap<>(chunk2.getStructureStarts())));
                    }

                    // 回到主线程应用 snapshot
                    world.getServer().submit(() -> {
                        addStructureReferencesSnapshot(worldAccess, structureAccessor, chunk, snapshots);
                    });
                }, world.getServer());
    }


    public void clear() {
        cache.clear();
    }

    public void addStructureReferencesSnapshot(
            StructureWorldAccess world,
            StructureAccessor structureAccessor,
            Chunk chunk,
            List<Map<Structure, StructureStart>> snapshots
    ) {
        ChunkPos pos = chunk.getPos();
        int l = pos.getStartX();
        int m = pos.getStartZ();
        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk);

        for (Map<Structure, StructureStart> map : snapshots) {
            for (StructureStart structureStart : map.values()) {
                if (structureStart.hasChildren() && structureStart.getBoundingBox().intersectsXZ(l, m, l + 15, m + 15)) {
                    structureAccessor.addStructureReference(chunkSectionPos, structureStart.getStructure(),
                            ChunkPos.toLong(structureStart.getPos().x, structureStart.getPos().z),
                            chunk);
                }
            }
        }
    }


    /**
     * From {@linkplain net.minecraft.world.gen.chunk.ChunkGenerator#addStructureReferences}
     */
    public void addStructureReferences(StructureWorldAccess world,
                                       StructureAccessor structureAccessor,
                                       Chunk chunk,
                                       List<Either<Chunk, ChunkHolder.Unloaded>> structureStartChunks) {
        if (structureStartChunks.isEmpty()) return;

        ChunkPos chunkPos = chunk.getPos();
        int l = chunkPos.getStartX();
        int m = chunkPos.getStartZ();
        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk);

        for (Either<Chunk, ChunkHolder.Unloaded> either : structureStartChunks) {
            Chunk startChunk = either.left().get();
            long p = ChunkPos.toLong(startChunk.getPos().x, startChunk.getPos().z);
            System.out.println("(1)");
            for (StructureStart structureStart : startChunk.getStructureStarts().values()) {
                try {
                    System.out.println("(2)");
                    if (structureStart.hasChildren() && structureStart.getBoundingBox().intersectsXZ(l, m, l + 15, m + 15)) {
                        structureAccessor.addStructureReference(chunkSectionPos, structureStart.getStructure(), p, chunk);
                        DebugInfoSender.sendStructureStart(world, structureStart);
                    }
                } catch (Exception var21) {
                    CrashReport crashReport = CrashReport.create(var21, "Generating pool reference");
                    CrashReportSection crashReportSection = crashReport.addElement("Structure");
                    Optional<? extends Registry<Structure>> optional = world.getRegistryManager().getOptional(RegistryKeys.STRUCTURE);
                    crashReportSection.add(
                            "Id",
                            (CrashCallable<String>)(() -> (String)optional.map(structureTypeRegistry -> structureTypeRegistry.getId(structureStart.getStructure()).toString())
                                    .orElse("UNKNOWN"))
                    );
                    crashReportSection.add("Name", (CrashCallable<String>)(() -> Registries.STRUCTURE_TYPE.getId(structureStart.getStructure().getType()).toString()));
                    crashReportSection.add("Class", (CrashCallable<String>)(() -> structureStart.getStructure().getClass().getCanonicalName()));
                    throw new CrashException(crashReport);
                }
            }
        }
    }

}
