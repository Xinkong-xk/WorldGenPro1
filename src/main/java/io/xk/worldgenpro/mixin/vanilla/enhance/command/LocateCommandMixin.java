package io.xk.worldgenpro.mixin.vanilla.enhance.command;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import io.xk.worldgenpro.addon.roadgen.helpers.WorldGenProStructureLocator;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.LocateCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.*;

import static net.minecraft.server.command.LocateCommand.sendCoordinates;

@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {

    @Shadow
    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureListForPredicate(
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> structureRegistry
    ){
        return null;
    }

    @Shadow
    @Final
    private static DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION;

    @Shadow
    @Final
    private static DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION;


    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author xk
     * @reason
     */
    @Overwrite
    private static int executeLocateStructure(ServerCommandSource source, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException {
        Registry<Structure> registry = source.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        RegistryEntryList<Structure> registryEntryList = (RegistryEntryList<Structure>)getStructureListForPredicate(predicate, registry)
                .orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asString()));
        BlockPos blockPos = BlockPos.ofFloored(source.getPosition());
        ServerWorld serverWorld = source.getWorld();
        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
        Pair<BlockPos, RegistryEntry<Structure>> pair = locateStructure_custom(serverWorld, registryEntryList, blockPos, 150, false,serverWorld.getChunkManager().getChunkGenerator());
        stopwatch.stop();
        if (pair == null) {
            throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.asString());
        } else {
            LOGGER.info(pair.toString());
            return sendCoordinates(source, predicate, blockPos, pair, "commands.locate.structure.success", false, stopwatch.elapsed());
        }
    }

    @Unique
    private static Pair<BlockPos, RegistryEntry<Structure>> locateStructure_custom(
            ServerWorld world,
            RegistryEntryList<Structure> structures,
            BlockPos center,
            int radius,
            boolean skipReferencedStructures,
            ChunkGenerator generator
            ) {
        StructurePlacementCalculator structurePlacementCalculator = world.getChunkManager().getStructurePlacementCalculator();
        Map<StructurePlacement, Set<RegistryEntry<Structure>>> map = new Object2ObjectArrayMap<>();

        for (RegistryEntry<Structure> registryEntry : structures) {
            for (StructurePlacement structurePlacement : structurePlacementCalculator.getPlacements(registryEntry)) {
                ((Set)map.computeIfAbsent(structurePlacement, placement -> new ObjectArraySet())).add(registryEntry);
            }
        }

        if (map.isEmpty()) {
            return null;
        } else {
            Pair<BlockPos, RegistryEntry<Structure>> pair = null;
            double d = Double.MAX_VALUE;
            StructureAccessor structureAccessor = world.getStructureAccessor();
            List<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> list = new ArrayList(map.size());

            for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : map.entrySet()) {
                StructurePlacement structurePlacement2 = (StructurePlacement)entry.getKey();
                if (structurePlacement2 instanceof ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
                    Pair<BlockPos, RegistryEntry<Structure>> pair2 = generator.locateConcentricRingsStructure(
                            (Set<RegistryEntry<Structure>>)entry.getValue(), world, structureAccessor, center, skipReferencedStructures, concentricRingsStructurePlacement
                    );
                    if (pair2 != null) {
                        BlockPos blockPos = pair2.getFirst();
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

                for (int k = 0; k <= radius; k++) {
                    boolean bl = false;

                    for (Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry2 : list) {
                        RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry2.getKey();
                        Pair<BlockPos, RegistryEntry<Structure>> pair3 = locateRandomSpreadStructure_Custom(
                                (Set<RegistryEntry<Structure>>)entry2.getValue(),
                                world,
                                structureAccessor,
                                i,
                                j,
                                k,
                                skipReferencedStructures,
                                structurePlacementCalculator.getStructureSeed(),
                                randomSpreadStructurePlacement,
                                generator
                        );
                        if (pair3 != null) {
                            bl = true;
                            double f = center.getSquaredDistance(pair3.getFirst());
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

    @Unique
    private static Pair<BlockPos, RegistryEntry<Structure>> locateRandomSpreadStructure_Custom(
            Set<RegistryEntry<Structure>> structures,
            ServerWorld world,
            StructureAccessor structureAccessor,
            int centerChunkX,
            int centerChunkZ,
            int radius,                     // 原版 k（搜索层）
            boolean skipReferencedStructures,
            long seed,
            RandomSpreadStructurePlacement placement,
            ChunkGenerator chunkGenerator
    ) {
        int spacing = placement.getSpacing(); // i

        // j = X方向步数（-radius 到 +radius）
        for (int j = -radius; j <= radius; j++) {

            boolean onVerticalEdge = (j == -radius || j == radius);

            // k = Z方向步数（-radius 到 +radius）
            for (int k = -radius; k <= radius; k++) {

                boolean onHorizontalEdge = (k == -radius || k == radius);

                // 原版：只处理边界 (j,k) 在外框上
                if (onVerticalEdge || onHorizontalEdge) {

                    int chunkX = centerChunkX + spacing * j;   // l
                    int chunkZ = centerChunkZ + spacing * k;   // m

                    // 先获取原版预测下的坐标
                    ChunkPos candidate;
                    // 原版调用 placement.getStartChunk，用此网格计算随机偏移
                    candidate = placement.getStartChunk(seed, chunkX, chunkZ);

                    //  如果是地理语义结构,则调用预测器
                    // -----------------------------------------
                    Pair<BlockPos, RegistryEntry<Structure>> result = null;
                    try{
                        result = WorldGenProStructureLocator.locateStructure_custom(structures,
                                        world,
                                        structureAccessor,
                                        skipReferencedStructures,
                                        placement,
                                        candidate
                                        ,chunkGenerator,
                                        chunkX,
                                        chunkZ,
                                null
                                );
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    // -----------------------------------------

                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }



}
