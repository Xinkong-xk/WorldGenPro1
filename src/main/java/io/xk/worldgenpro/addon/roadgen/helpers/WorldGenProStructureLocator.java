package io.xk.worldgenpro.addon.roadgen.helpers;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.roadgen.RoadConnectionConfigManager;
import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.api.GridGraph;
import io.xk.worldgenpro.addon.semantic.GeoSemanticStructureManager;
import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructurePresence;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

public class WorldGenProStructureLocator {

    public static final Logger LOGGER = LoggerFactory.getLogger(RoadGen.MOD_ID);

    public static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(
            id -> Text.translatable("commands.locate.structure.invalid", id)
    );
    public static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(
            id -> Text.translatable("commands.locate.structure.not_found", id)
    );

    // 搜索结构
    @Nullable
    public static Pair<BlockPos, RegistryEntry<Structure>> locateStructure(
            ServerWorld world,
            String structureSetName,
            StructureSet structureSet,
            BlockPos center,
            int radius,
            boolean skipReferencedStructures,
            GridGraph gridGraph,
            boolean record,
            ChunkPos initChunkPos,
            boolean skipLoadedRegion,
            LongOpenHashSet skipRegions
    ) {
        StructurePlacementCalculator structurePlacementCalculator = world.getChunkManager().getStructurePlacementCalculator();
        Map<StructurePlacement, Set<RegistryEntry<Structure>>> map = new Object2ObjectArrayMap<>();
        Iterator<StructureSet.WeightedEntry> var8 = structureSet.structures().iterator();

        while(var8.hasNext()) {
            StructureSet.WeightedEntry entry = var8.next();
            RegistryEntry<Structure> registryEntry = entry.structure();
            Iterator var10 = structurePlacementCalculator.getPlacements(registryEntry).iterator();

            while(var10.hasNext()) {
                StructurePlacement structurePlacement = (StructurePlacement)var10.next();
                ((Set)map.computeIfAbsent(structurePlacement, (placement) -> {
                    return new ObjectArraySet();
                })).add(registryEntry);
            }
        }

        if (map.isEmpty()) {
            return null;
        } else {
            Pair<BlockPos, RegistryEntry<Structure>> pair = null;
            double d = Double.MAX_VALUE;
            StructureAccessor structureAccessor = world.getStructureAccessor();
            List<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> list = new ArrayList(map.size());
            Iterator<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> var13 = map.entrySet().iterator();

            while(var13.hasNext()) {
                Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry = (Map.Entry)var13.next();
                StructurePlacement structurePlacement2 = (StructurePlacement)entry.getKey();
                if (structurePlacement2 instanceof ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
                    Pair<BlockPos, RegistryEntry<Structure>> pair2 = locateConcentricRingsStructure((Set)entry.getValue(), world, structureAccessor, center,
                            skipReferencedStructures, concentricRingsStructurePlacement,initChunkPos);
                    if (pair2 != null) {
                        BlockPos blockPos = (BlockPos)pair2.getFirst();
                        double e = center.getSquaredDistance(blockPos);
                        if (e < d) {
                            d = e;
                            pair = pair2;
                        }
                    }
                } else if (structurePlacement2 instanceof RandomSpreadStructurePlacement) {
                    list.add(entry);
                }
            }

            if (!list.isEmpty()) {
                int i = ChunkSectionPos.getSectionCoord(center.getX());
                int j = ChunkSectionPos.getSectionCoord(center.getZ());

                for(int k = 0; k <= radius; ++k) {
                    boolean bl = false;
                    Iterator<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> var30 = list.iterator();

                    while(var30.hasNext()) {
                        Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry2 = (Map.Entry)var30.next();
                        RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry2.getKey();
                        Pair<BlockPos, RegistryEntry<Structure>> pair3 = locateRandomSpreadStructure(
                                (Set)entry2.getValue(),
                                world,
                                structureAccessor,
                                i, j, k,
                                skipReferencedStructures,
                                structurePlacementCalculator.getStructureSeed(),
                                randomSpreadStructurePlacement,
                                structureSet,
                                structureSetName,
                                gridGraph,
                                record,
                                initChunkPos,
                                skipLoadedRegion,
                                skipRegions);
                        if (pair3 != null) {
                            bl = true;
                            double f = center.getSquaredDistance((Vec3i)pair3.getFirst());
                            if (f < d) {
                                d = f;
                                pair = pair3;
                            }
                        }
                    }
                    if (bl) {
                        return pair;
                    }
                }
            }
            return pair;
        }
    }

    @Nullable
    public static Pair<BlockPos, RegistryEntry<Structure>> locateConcentricRingsStructure(Set<RegistryEntry<Structure>> structures,
                                                                                          ServerWorld world, StructureAccessor structureAccessor,
                                                                                          BlockPos center,
                                                                                          boolean skipReferencedStructures,
                                                                                          ConcentricRingsStructurePlacement placement,
                                                                                          ChunkPos initChunkPos) {
        List<ChunkPos> list = world.getChunkManager().getStructurePlacementCalculator().getPlacementPositions(placement);
        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        } else {
            Pair<BlockPos, RegistryEntry<Structure>> pair = null;
            double d = Double.MAX_VALUE;
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            Iterator<ChunkPos> var12 = list.iterator();

            while(var12.hasNext()) {
                ChunkPos chunkPos = (ChunkPos)var12.next();
                mutable.set(ChunkSectionPos.getOffsetPos(chunkPos.x, 8), 32, ChunkSectionPos.getOffsetPos(chunkPos.z, 8));
                double e = mutable.getSquaredDistance(center);
                boolean bl = pair == null || e < d;
                if (bl) {
                    Pair<BlockPos, RegistryEntry<Structure>> pair2 = locateStructure_custom(structures, world, structureAccessor,
                            skipReferencedStructures, placement,
                            chunkPos,
                            world.getChunkManager().getChunkGenerator(),
                            chunkPos.x,
                            chunkPos.z,
                            initChunkPos);
                    if (pair2 != null) {
                        pair = pair2;
                        d = e;
                    }
                }
            }

            return pair;
        }
    }

    @Nullable
    private static Pair<BlockPos, RegistryEntry<Structure>> locateRandomSpreadStructure(
            Set<RegistryEntry<Structure>> structures,
            ServerWorld world,
            StructureAccessor structureAccessor,
            int centerChunkX,
            int centerChunkZ,
            int radius,
            boolean skipReferencedStructures,
            long seed,
            RandomSpreadStructurePlacement placement,
            StructureSet structureSet,
            String structureSetName,
            GridGraph gridGraph,
            boolean record,
            ChunkPos initChunkPos,
            boolean skipLoadedRegion,
            LongOpenHashSet skipRegions
    ) {

        int spacing = placement.getSpacing();

        for(int j = -radius; j <= radius; ++j) {
            boolean bl = j == -radius || j == radius;

            for(int k = -radius; k <= radius; ++k) {
                boolean bl2 = k == -radius || k == radius;
                if (bl || bl2) {
                    int l = centerChunkX + spacing * j;
                    int m = centerChunkZ + spacing * k;

                    long regionId = RoadConnectionConfigManager.getRegionId(l,m, spacing);

                    // TODO 如果skipRegions集合不为Null,并且此region在集合里,则跳过
                    if (skipRegions != null && skipRegions.contains(regionId)) {
                        continue;
                    }

                    // TODO 如果gridGraph不为Null,并且skipLoadedRegion为true则进行跳过已加载region的判断
                    if (gridGraph != null && skipLoadedRegion){
                        if (gridGraph.cellToPoint.containsKey(regionId)) {
                            continue;
                        }
                    }

                    int rx = RoadConnectionConfigManager.toR(l, spacing);
                    int rz = RoadConnectionConfigManager.toR(m, spacing);

                    ChunkPos chunkPos = placement.getStartChunk(seed, l, m);

                    // 无论是否找到,都记录
                    Pair<BlockPos, RegistryEntry<Structure>> pair = locateStructure_custom(
                            structures,
                            world,
                            structureAccessor,
                            skipReferencedStructures,
                            placement,
                            chunkPos,
                            world.getChunkManager().getChunkGenerator(),
                            chunkPos.x,
                            chunkPos.z,
                            initChunkPos
                    );

                    // TODO 如果不为Null则记录
                    if (gridGraph != null){
                        if (pair == null){
                            // 如果允许记录,则进行标记
                            if (record){
                                // 如果没有找到,则标记空cell
                                gridGraph.markEmptyCell(rx,rz);
                            }
                        }else{
                            // 如果允许记录,则进行标记
                            if (record){
                                gridGraph.addPoint(rx,rz,pair.getSecond().getKey().get().getValue().toString());
                            }
                        }
                    }

                    // TODO (默认):如果skipRegions集合不为Null,并且搜索结构的结果不为Null,则记录此regionID
                    if (skipRegions != null && pair!= null){
                        skipRegions.add(regionId);
                    }

                    // 返回
                    if (pair != null) {
                        return pair;
                    }

                }
            }
        }

        return null;
    }

    public static boolean checkNotReferenced(StructureAccessor structureAccessor, StructureStart start) {
        if (start.isNeverReferenced()) {
            structureAccessor.incrementReferences(start);
            return true;
        } else {
            return false;
        }
    }

    @Unique
    public static Pair<BlockPos, RegistryEntry<Structure>> locateStructure_custom(
            Set<RegistryEntry<Structure>> structures,
            ServerWorld world,
            StructureAccessor structureAccessor,
            boolean skipReferencedStructures,
            StructurePlacement placement,
            ChunkPos pos,
            ChunkGenerator chunkGenerator,
            int chunkX,
            int chunkZ,
            ChunkPos initChunkPos
    ) {
        // 直接预测的坐标
        ChunkPos startPos = pos;

        ChunkPos checkChunkPos = new ChunkPos(chunkX, chunkZ);

        ObjectArrayList<StructureSet.WeightedEntry> weightedEntries = new ObjectArrayList<>(structures.size());
        for (RegistryEntry<Structure> registryEntry : structures) {
            weightedEntries.add(new StructureSet.WeightedEntry(registryEntry,1));
        }

        // 检查
        for (RegistryEntry<Structure> registryEntry : structures) {
            Structure structure = registryEntry.value();
            // 检查是否是已经记录的地理语义结构
            if (structure instanceof JigsawStructure jigsawStructure){
                // 如果是拼图类型结构
                if (GeoSemanticStructureManager.geoJigsawStructures.containsKey(jigsawStructure)){
                    // 如果是地理语义生成的结构
                    if (GeoSemanticStructureManager.geoJigsawStructures.get(jigsawStructure).containsKey(placement)){
                        // 获取结构集
                        StructureSet structureSet = GeoSemanticStructureManager.geoJigsawStructures.get(jigsawStructure).get(placement);
                        // 获取地理语义配置
                        GeoSemanticConfig geoSemanticConfig = WorldGenPro.geoSemanticStructureManager.geoSemanticConfigs.get(structureSet);
                        // 获取真正的生成区块坐标
                        startPos = WorldGenPro.geoSemanticStructureManager.getStructureInCurrentRegion(
                                world.getRegistryManager(),
                                world.getChunkManager().getStructurePlacementCalculator(),
                                structureAccessor,
                                world.getStructureTemplateManager(),
                                chunkGenerator,
                                world,
                                structureSet,
                                geoSemanticConfig,
                                geoSemanticConfig.getGeoExpr(),
                                placement,
                                weightedEntries,
                                checkChunkPos,
                                null,
                                world.getChunkManager().getNoiseConfig(),
                                jigsawStructure
                        );

                        if (startPos == null){
                            return null;
                        }
                    }
                }
            }

            if (startPos == null){
                return null;
            }

            // 原版后续流程
            StructurePresence structurePresence = structureAccessor.getStructurePresence(startPos, registryEntry.value(), skipReferencedStructures);
            if (structurePresence != StructurePresence.START_NOT_PRESENT) {
                if (!skipReferencedStructures && structurePresence == StructurePresence.START_PRESENT) {
                    return Pair.of(placement.getLocatePos(startPos), registryEntry);
                }

                // TODO 如果初始区块不为Null,则进行判断
                if (initChunkPos != null){
                    if (pos.x == initChunkPos.x && pos.z == initChunkPos.z) {
                        return null;
                    }
                }

                Chunk chunk = world.getChunk(startPos.x, startPos.z, ChunkStatus.STRUCTURE_STARTS);

                StructureStart structureStart = structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), registryEntry.value(), chunk);
                if (structureStart != null && structureStart.hasChildren() && (!skipReferencedStructures || ChunkGenerator.checkNotReferenced(structureAccessor, structureStart))) {
                    return Pair.of(placement.getLocatePos(structureStart.getPos()), registryEntry);
                }
            }
        }

        return null;
    }

}
