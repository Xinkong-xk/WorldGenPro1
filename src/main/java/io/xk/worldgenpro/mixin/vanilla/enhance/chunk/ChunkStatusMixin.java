package io.xk.worldgenpro.mixin.vanilla.enhance.chunk;

import com.mojang.datafixers.util.Either;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import io.xk.worldgenpro.addon.semantic.geo.GeoExpr;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructureSetReflect;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.*;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static io.xk.worldgenpro.addon.semantic.GeoSemanticStructureManager.recordGenerateResult;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin {


    @Inject(
            method = "register(Ljava/lang/String;Lnet/minecraft/world/chunk/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/chunk/ChunkStatus$LoadTask;)Lnet/minecraft/world/chunk/ChunkStatus;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void redirectAddStructureStart(
            String id,
            @Nullable ChunkStatus previous,
            int taskMargin,
            boolean shouldAlwaysUpgrade,
            EnumSet<Heightmap.Type> heightMapTypes,
            ChunkStatus.ChunkType chunkType,
            ChunkStatus.GenerationTask generationTask,
            ChunkStatus.LoadTask loadTask,
            CallbackInfoReturnable<ChunkStatus> cir
    ) {
        // 结构起始生成任务
        // STRUCTURE_STARTS
        if (id.equals("structure_starts")) {
            generationTask = (
                    ChunkStatus targetStatus,
                    Executor executor,
                    ServerWorld world,
                    ChunkGenerator generator,
                    StructureTemplateManager structureTemplateManager,
                    ServerLightingProvider lightingProvider,
                    Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> fullChunkConverter,
                    List<Chunk> chunks,
                    Chunk chunk
            ) -> {
                if (world.getServer().getSaveProperties().getGeneratorOptions().shouldGenerateStructures()) {
                    // TODO ADD:Geo地理语义搜索定位
                    setStructureStarts(
                            world.getRegistryManager(),
                            world.getChunkManager().getStructurePlacementCalculator(),
                            world.getStructureAccessor(),
                            chunk,
                            structureTemplateManager,
                            generator,
                            world
                    );
                }
                // 缓存此世界上的结构起始点
                world.cacheStructures(chunk);

                return CompletableFuture.completedFuture(Either.left(chunk));
            };

            // 设置返回值
            cir.setReturnValue(
                    Registry.register(
                            Registries.CHUNK_STATUS,
                            id,
                            ChunkStatusAccessor.invokeNew(previous, taskMargin, shouldAlwaysUpgrade, heightMapTypes, chunkType, generationTask, loadTask)
                    )
            );
            // 强制结束
            cir.cancel();
        }
//        // FEATURES
//        else if (id.equals("features")) {
//            generationTask = (
//                    ChunkStatus targetStatus,
//                    Executor executor,
//                    ServerWorld world,
//                    ChunkGenerator generator,
//                    StructureTemplateManager structureTemplateManager,
//                    ServerLightingProvider lightingProvider,
//                    Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> fullChunkConverter,
//                    List<Chunk> chunks,
//                    Chunk chunk
//            ) -> {
//                // 正常原版流程
//                Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE));
//                ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, 1);
//                generator.generateFeatures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion));
//                Blender.tickLeavesAndFluids(chunkRegion, chunk);
//                // 构造返回值
//                return CompletableFuture.completedFuture(Either.left(chunk));
//            };
//
//            cir.setReturnValue(
//                    Registry.register(
//                            Registries.CHUNK_STATUS,
//                            id,
//                            ChunkStatusAccessor.invokeNew(previous, taskMargin, shouldAlwaysUpgrade, heightMapTypes, chunkType, generationTask, loadTask)
//                    )
//            );
//        }
    }

    @Unique
    private static void setStructureStarts(
            DynamicRegistryManager registryManager,
            StructurePlacementCalculator placementCalculator,
            StructureAccessor structureAccessor,
            Chunk chunk,
            StructureTemplateManager structureTemplateManager,
            ChunkGenerator generator,
            ServerWorld world
    ) {
        ChunkPos chunkPos = chunk.getPos();
        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk);
        NoiseConfig noiseConfig = placementCalculator.getNoiseConfig();
        placementCalculator.getStructureSets()
                .forEach(
                        structureSetEntry -> {
                            // 获取结果集
                            StructureSet structureSet = structureSetEntry.value();
                            // 获取地理语义定位配置
                            GeoSemanticConfig geoSemanticConfig = StructureSetReflect.getGeoSemanticConfig(structureSet);

                            StructurePlacement structurePlacement = ((StructureSet) structureSetEntry.value()).placement();
                            List<StructureSet.WeightedEntry> list = ((StructureSet) structureSetEntry.value()).structures();

                            for (StructureSet.WeightedEntry weightedEntry : list) {
                                StructureStart structureStart = structureAccessor.getStructureStart(chunkSectionPos, weightedEntry.structure().value(), chunk);
                                if (structureStart != null && structureStart.hasChildren()) {
                                    return;
                                }
                            }

                            // 如果这个结果集在平均范围内,则进行检查
                            if (geoSemanticConfig.getGeoExpr() != null){
                                if (WorldGenPro.geoSemanticStructureManager.checkChunk(
                                       structureSet,
                                        chunkPos,
                                        placementCalculator.getStructureSeed()
                                )){
                                    geoGenerate(
                                            registryManager,
                                            placementCalculator,
                                            structureAccessor,
                                            chunk,
                                            structureTemplateManager,
                                            generator,
                                            world,
                                            structureSet,
                                            geoSemanticConfig,
                                            geoSemanticConfig.getGeoExpr(),
                                            structurePlacement,
                                            list,
                                            chunkPos,
                                            chunkSectionPos,
                                            noiseConfig
                                    );
                                }
                            } else{
                                // 如果这个结构集没有配置地理语义生成定位或者配置无效,则由原版生成器定位
                                if (structurePlacement.shouldGenerate(placementCalculator, chunkPos.x, chunkPos.z)) {
                                    if (list.size() == 1) {
                                        generator.trySetStructureStart(
                                                (StructureSet.WeightedEntry)list.getFirst(),
                                                structureAccessor,
                                                registryManager,
                                                noiseConfig,
                                                structureTemplateManager,
                                                placementCalculator.getStructureSeed(),
                                                chunk,
                                                chunkPos,
                                                chunkSectionPos
                                        );
                                    } else {
                                        ObjectArrayList<StructureSet.WeightedEntry> arrayList = new ObjectArrayList(list.size());
                                        arrayList.addAll(list);
                                        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(0L));
                                        chunkRandom.setCarverSeed(placementCalculator.getStructureSeed(), chunkPos.x, chunkPos.z);
                                        int i = 0;

                                        for (StructureSet.WeightedEntry weightedEntry2 : arrayList) {
                                            i += weightedEntry2.weight();
                                        }

                                        while (!arrayList.isEmpty()) {
                                            int j = chunkRandom.nextInt(i);
                                            int k = 0;
                                            for (StructureSet.WeightedEntry weightedEntry3 : arrayList) {
                                                j -= weightedEntry3.weight();
                                                if (j < 0) {
                                                    break;
                                                }
                                                k++;
                                            }

                                            StructureSet.WeightedEntry weightedEntry4 = arrayList.get(k);
                                            if (generator.trySetStructureStart(
                                                    weightedEntry4,
                                                    structureAccessor,
                                                    registryManager,
                                                    noiseConfig,
                                                    structureTemplateManager,
                                                    placementCalculator.getStructureSeed(),
                                                    chunk,
                                                    chunkPos,
                                                    chunkSectionPos
                                            )) {
                                                return;
                                            }

                                            arrayList.remove(k);
                                            i -= weightedEntry4.weight();
                                        }
                                    }
                                }
                            }
                        }
                );
    }


    @Unique
    private static void geoGenerate(
            DynamicRegistryManager registryManager,
            StructurePlacementCalculator placementCalculator,
            StructureAccessor structureAccessor,
            Chunk chunk,
            StructureTemplateManager structureTemplateManager,
            ChunkGenerator generator,
            ServerWorld world,
            StructureSet structureSet,
            GeoSemanticConfig geoSemanticConfig,
            GeoExpr geoExpr,
            StructurePlacement structurePlacement,
            List<StructureSet.WeightedEntry> list,
            ChunkPos chunkPos,
            ChunkSectionPos chunkSectionPos,
            NoiseConfig noiseConfig
    ) {
        if (list.size() == 1) {
            // 检查此结构是否需要生成在这个区块
            if (WorldGenPro.geoSemanticStructureManager.checkStructure(
                    registryManager,
                    placementCalculator,
                    structureAccessor,
                    structureTemplateManager,
                    generator,
                    world,
                    structureSet,
                    geoSemanticConfig,
                    geoSemanticConfig.getGeoExpr(),
                    structurePlacement,
                    list,
                    chunkPos,
                    chunkSectionPos,
                    noiseConfig,
                    list.getFirst()
            )){
                boolean success = generator.trySetStructureStart(
                        list.getFirst(),
                        structureAccessor,
                        registryManager,
                        noiseConfig,
                        structureTemplateManager,
                        placementCalculator.getStructureSeed(),
                        chunk,
                        chunkPos,
                        chunkSectionPos
                );
                recordGenerateResult(world.getRegistryKey(),structureSet,list.getFirst().structure().value(),chunkPos,structureAccessor,success);
            }
        } else {
            ObjectArrayList<StructureSet.WeightedEntry> arrayList = new ObjectArrayList<>(list.size());
            arrayList.addAll(list);
            ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(0L));
            chunkRandom.setCarverSeed(placementCalculator.getStructureSeed(), chunkPos.x, chunkPos.z);
            int i = 0;

            for (StructureSet.WeightedEntry weightedEntry2 : arrayList) {
                i += weightedEntry2.weight();
            }

            while (!arrayList.isEmpty()) {
                int j = chunkRandom.nextInt(i);
                int k = 0;
                for (StructureSet.WeightedEntry weightedEntry3 : arrayList) {
                    j -= weightedEntry3.weight();
                    if (j < 0) {
                        break;
                    }
                    k++;
                }

                StructureSet.WeightedEntry weightedEntry4 = arrayList.get(k);

                // 检查是否可以生成
                if (WorldGenPro.geoSemanticStructureManager.checkStructure(
                        registryManager,
                        placementCalculator,
                        structureAccessor,
                        structureTemplateManager,
                        generator,
                        world,
                        structureSet,
                        geoSemanticConfig,
                        geoSemanticConfig.getGeoExpr(),
                        structurePlacement,
                        list,
                        chunkPos,
                        chunkSectionPos,
                        noiseConfig,
                        weightedEntry4
                )){
                    if (generator.trySetStructureStart(
                            weightedEntry4,
                            structureAccessor,
                            registryManager,
                            noiseConfig,
                            structureTemplateManager,
                            placementCalculator.getStructureSeed(),
                            chunk,
                            chunkPos,
                            chunkSectionPos
                    )) {
                        recordGenerateResult(world.getRegistryKey(),structureSet,weightedEntry4.structure().value(),chunkPos,structureAccessor,true);

                        // 记录剩下未生成的标记为生成失败
                        ObjectArrayList<StructureSet.WeightedEntry> objectArrayList = new ObjectArrayList<>(arrayList);
                        objectArrayList.remove(weightedEntry4);
                        for (StructureSet.WeightedEntry weightedEntryFailed : objectArrayList) {
                            recordGenerateResult(world.getRegistryKey(),structureSet,weightedEntryFailed.structure().value(),chunkPos,structureAccessor,false);
                        }

                        return;
                    }else{
                        recordGenerateResult(world.getRegistryKey(),structureSet,weightedEntry4.structure().value(),chunkPos,structureAccessor,false);
                    }
                }

                arrayList.remove(k);
                i -= weightedEntry4.weight();
            }
            // 如果一个都没能成功生成
            // while 循环结束后（没有 return）
            for (StructureSet.WeightedEntry e : list) {
                recordGenerateResult(world.getRegistryKey(),structureSet, e.structure().value(), chunkPos, structureAccessor, false);
            }
        }
    }
}
