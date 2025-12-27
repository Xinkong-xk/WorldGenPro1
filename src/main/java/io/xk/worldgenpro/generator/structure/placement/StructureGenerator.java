package io.xk.worldgenpro.generator.structure.placement;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.*;
import net.minecraft.structure.pool.*;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static io.xk.worldgenpro.WorldGenPro.minecraftServer;

public class StructureGenerator {

    public static RegistryEntry<StructurePool> fallback = null;
    public static Object2ObjectOpenHashMap<String, RegistryEntry<StructurePool>> structurePoolCache = new Object2ObjectOpenHashMap<>();
    private static final RegistryEntry<StructureProcessorList> EMPTY_PROCESSORS = RegistryEntry.of(new StructureProcessorList(List.of()));

    public static void GenerateNbt(ServerWorld world,
                                   String nbtId,
                                   BlockPos pos,
                                   net.minecraft.structure.pool.StructurePool.Projection projection,
                                   int size,
                                   int maxDistanceFromCenter,
                                   boolean useExpansionHack,
                                   boolean keepJigsaw,
                                   net.minecraft.registry.entry.RegistryEntry<net.minecraft.structure.processor.StructureProcessorList> processor) {


        RegistryEntry<StructurePool> poolEntry;
        if (!structurePoolCache.containsKey(nbtId)) {
            if (processor == null) processor = EMPTY_PROCESSORS;

            CustomSinglePoolElement element = new CustomSinglePoolElement(Either.left(new Identifier(nbtId)), processor, projection);

            if (fallback == null) {
                fallback = minecraftServer.getRegistryManager().getWrapperOrThrow(RegistryKeys.TEMPLATE_POOL)
                        .getOrThrow(StructurePools.of("minecraft:empty"));
            }

            StructurePool startPool = new StructurePool(
                    fallback,
                    List.of(Pair.of(element, 1))
            );

            poolEntry = RegistryEntry.of(startPool);
            structurePoolCache.put(nbtId, poolEntry);
        } else {
            poolEntry = structurePoolCache.get(nbtId);
        }


        Random random = world.getRandom();
        Structure.Context structure_context = getContext(world, pos);

        // --- generate 调用 ---
        Optional<Structure.StructurePosition> result =
                ForkStructurePoolBasedGenerator.generate(
                        structure_context,
                        poolEntry,
                        Optional.empty(),
                        size,
                        pos,
                        useExpansionHack,
                        Optional.empty(),
                        maxDistanceFromCenter,
                        StructurePoolAliasLookup.EMPTY
                );

        // --- pieces generate 计时 ---
        if (result.isPresent()) {
            StructurePiecesCollector collector = result.get().generate();
            java.util.List<net.minecraft.structure.StructurePiece> piecesList = collector.toList().pieces();

            for (StructurePiece piece : piecesList) {
                if (piece instanceof PoolStructurePiece poolPiece) {

                                poolPiece.generate(
                                        world,
                                        world.getStructureAccessor(),
                                        world.getChunkManager().getChunkGenerator(),
                                        random,
                                        BlockBox.infinite(),
                                        pos,
                                        keepJigsaw
                                );

                } else {
                    System.out.println("[Warning] : " + piece + " : unknown ");
                }
            }

        } else {
            System.out.println("[Error] : No pool found!");
        }

    }

    private static Structure.@NotNull Context getContext(ServerWorld world, BlockPos pos) {
        ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();
        BiomeSource biomeSource = chunkGenerator.getBiomeSource();
        NoiseConfig noiseConfig = world.getChunkManager().getNoiseConfig();
        StructureTemplateManager templateManager = world.getServer().getStructureTemplateManager();

        return new Structure.Context(
                world.getRegistryManager(),
                chunkGenerator,
                biomeSource,
                noiseConfig,
                templateManager,
                world.getSeed(),
                new ChunkPos(pos),
                world,
                biome -> true
        );
    }
}
