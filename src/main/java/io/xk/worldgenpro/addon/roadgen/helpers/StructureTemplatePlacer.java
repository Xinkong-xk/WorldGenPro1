package io.xk.worldgenpro.addon.roadgen.helpers;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.FeaturePoolElement;
import net.minecraft.structure.pool.LegacySinglePoolElement;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.JigsawReplacementStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.ServerWorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static net.minecraft.structure.StructureTemplate.*;

public class StructureTemplatePlacer {
    public static void placeStructure(StructurePoolElement structurePoolElement,
                                      StructureTemplateManager structureTemplateManager,
                                      BlockPos placePos,
                                      ServerWorld world
    ) {
        if (structurePoolElement instanceof LegacySinglePoolElement legacySinglePoolElement){
            StructureTemplate structureTemplate = getStructure(structureTemplateManager,legacySinglePoolElement.location);
            placeStructureTemplate(
                    world,
                    placePos,
                    placePos,
                    new StructurePlacementData()
                            .addProcessor(JigsawReplacementStructureProcessor.INSTANCE)
                            .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR),
                    world.random,
                    3,
                    structureTemplate.blockInfoLists,
                    structureTemplate.getSize(),
                    structureTemplate.entities);
        }
        else if (structurePoolElement instanceof SinglePoolElement singlePoolElement){
            StructureTemplate structureTemplate = getStructure(structureTemplateManager,singlePoolElement.location);
            placeStructureTemplate(
                    world,
                    placePos,
                    placePos,
                    new StructurePlacementData()
                            .addProcessor(JigsawReplacementStructureProcessor.INSTANCE)
                            .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR),
                    world.random,
                    3,
                    structureTemplate.blockInfoLists,
                    structureTemplate.getSize(),
                    structureTemplate.entities);
        }
        else if (structurePoolElement instanceof FeaturePoolElement featurePoolElement){

        }
    }

    private static StructureTemplate getStructure(StructureTemplateManager structureTemplateManager,
                                           com.mojang.datafixers.util.Either<net.minecraft.util.Identifier, net.minecraft.structure.StructureTemplate> location) {
        return location.map(structureTemplateManager::getTemplateOrBlank, Function.identity());
    }

    private static boolean placeStructureTemplate(
            ServerWorldAccess world,
            BlockPos pos,
            BlockPos pivot,
            StructurePlacementData placementData,
            Random random,
            int flags,
            List<StructureTemplate.PalettedBlockInfoList> blockInfoLists,
            Vec3i size,
            List<StructureTemplate.StructureEntityInfo> entities
    ) {
        if (blockInfoLists.isEmpty()) {
            return false;
        } else {
            List<StructureTemplate.StructureBlockInfo> list = placementData.getRandomBlockInfos(blockInfoLists, pos).getAll();
            if ((!list.isEmpty() || !placementData.shouldIgnoreEntities() && !entities.isEmpty())
                    && size.getX() >= 1
                    && size.getY() >= 1
                    && size.getZ() >= 1) {
                BlockBox blockBox = placementData.getBoundingBox();
                List<BlockPos> list2 = Lists.<BlockPos>newArrayListWithCapacity(placementData.shouldPlaceFluids() ? list.size() : 0);
                List<BlockPos> list3 = Lists.<BlockPos>newArrayListWithCapacity(placementData.shouldPlaceFluids() ? list.size() : 0);
                List<Pair<BlockPos, NbtCompound>> list4 = Lists.<Pair<BlockPos, NbtCompound>>newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int m = Integer.MIN_VALUE;
                int n = Integer.MIN_VALUE;
                for (StructureTemplate.StructureBlockInfo structureBlockInfo : process(world, pos, pivot, placementData, list)) {
                    BlockPos blockPos = structureBlockInfo.pos();
                    if (blockBox == null || blockBox.contains(blockPos)) {
                        FluidState fluidState = placementData.shouldPlaceFluids() ? world.getFluidState(blockPos) : null;
                        BlockState blockState = structureBlockInfo.state().mirror(placementData.getMirror()).rotate(placementData.getRotation());
                        if (structureBlockInfo.nbt() != null) {
                            BlockEntity blockEntity = world.getBlockEntity(blockPos);
                            Clearable.clear(blockEntity);
                            world.setBlockState(blockPos, Blocks.BARRIER.getDefaultState(), Block.FORCE_STATE | Block.NO_REDRAW);
                        }

                        if (world.setBlockState(blockPos, blockState, flags)) {
                            i = Math.min(i, blockPos.getX());
                            j = Math.min(j, blockPos.getY());
                            k = Math.min(k, blockPos.getZ());
                            l = Math.max(l, blockPos.getX());
                            m = Math.max(m, blockPos.getY());
                            n = Math.max(n, blockPos.getZ());
                            list4.add(Pair.of(blockPos, structureBlockInfo.nbt()));
                            if (structureBlockInfo.nbt() != null) {
                                BlockEntity blockEntity = world.getBlockEntity(blockPos);
                                if (blockEntity != null) {
                                    if (blockEntity instanceof LootableInventory) {
                                        structureBlockInfo.nbt().putLong("LootTableSeed", random.nextLong());
                                    }

                                    blockEntity.readNbt(structureBlockInfo.nbt());
                                }
                            }

                            if (fluidState != null) {
                                if (blockState.getFluidState().isStill()) {
                                    list3.add(blockPos);
                                } else if (blockState.getBlock() instanceof FluidFillable) {
                                    ((FluidFillable)blockState.getBlock()).tryFillWithFluid(world, blockPos, blockState, fluidState);
                                    if (!fluidState.isStill()) {
                                        list2.add(blockPos);
                                    }
                                }
                            }
                        }
                    }
                }

                boolean bl = true;
                Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                while (bl && !list2.isEmpty()) {
                    bl = false;
                    Iterator<BlockPos> iterator = list2.iterator();

                    while (iterator.hasNext()) {
                        BlockPos blockPos2 = (BlockPos)iterator.next();
                        FluidState fluidState2 = world.getFluidState(blockPos2);

                        for (int o = 0; o < directions.length && !fluidState2.isStill(); o++) {
                            BlockPos blockPos3 = blockPos2.offset(directions[o]);
                            FluidState fluidState3 = world.getFluidState(blockPos3);
                            if (fluidState3.isStill() && !list3.contains(blockPos3)) {
                                fluidState2 = fluidState3;
                            }
                        }

                        if (fluidState2.isStill()) {
                            BlockState blockState2 = world.getBlockState(blockPos2);
                            Block block = blockState2.getBlock();
                            if (block instanceof FluidFillable) {
                                ((FluidFillable)block).tryFillWithFluid(world, blockPos2, blockState2, fluidState2);
                                bl = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (i <= l) {
                    if (!placementData.shouldUpdateNeighbors()) {
                        VoxelSet voxelSet = new BitSetVoxelSet(l - i + 1, m - j + 1, n - k + 1);
                        int p = i;
                        int q = j;
                        int ox = k;

                        for (Pair<BlockPos, NbtCompound> pair : list4) {
                            BlockPos blockPos4 = pair.getFirst();
                            voxelSet.set(blockPos4.getX() - p, blockPos4.getY() - q, blockPos4.getZ() - ox);
                        }

                        // NOTE 可能在这里
                        updateCorner(world, flags, voxelSet, p, q, ox);
                    }

                    for (Pair<BlockPos, NbtCompound> pair2 : list4) {
                        BlockPos blockPos5 = pair2.getFirst();
                        if (!placementData.shouldUpdateNeighbors()) {
                            BlockState blockState2 = world.getBlockState(blockPos5);
                            BlockState blockState3 = Block.postProcessState(blockState2, world, blockPos5);
                            if (blockState2 != blockState3) {
                                world.setBlockState(blockPos5, blockState3, flags & ~Block.NOTIFY_NEIGHBORS | Block.FORCE_STATE);
                            }

                            world.updateNeighbors(blockPos5, blockState3.getBlock());
                        }

                        if (pair2.getSecond() != null) {
                            BlockEntity blockEntity = world.getBlockEntity(blockPos5);
                            if (blockEntity != null) {
                                blockEntity.markDirty();
                            }
                        }
                    }
                }

                if (!placementData.shouldIgnoreEntities()) {
                    spawnEntities(
                            world, pos, placementData.getMirror(), placementData.getRotation(), placementData.getPosition(), blockBox, placementData.shouldInitializeMobs(),entities
                    );
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public static void spawnEntities(
            ServerWorldAccess world,
            BlockPos pos,
            BlockMirror mirror,
            BlockRotation rotation,
            BlockPos pivot,
            @Nullable BlockBox area,
            boolean initializeMobs,
            List<StructureTemplate.StructureEntityInfo> entities
    ) {
        for (StructureTemplate.StructureEntityInfo structureEntityInfo : entities) {
            BlockPos blockPos = transformAround(structureEntityInfo.blockPos, mirror, rotation, pivot).add(pos);
            if (area == null || area.contains(blockPos)) {
                NbtCompound nbtCompound = structureEntityInfo.nbt.copy();
                Vec3d vec3d = transformAround(structureEntityInfo.pos, mirror, rotation, pivot);
                Vec3d vec3d2 = vec3d.add(pos.getX(), pos.getY(), pos.getZ());
                NbtList nbtList = new NbtList();
                nbtList.add(NbtDouble.of(vec3d2.x));
                nbtList.add(NbtDouble.of(vec3d2.y));
                nbtList.add(NbtDouble.of(vec3d2.z));
                nbtCompound.put("Pos", nbtList);
                nbtCompound.remove("UUID");
                getEntity(world, nbtCompound).ifPresent(entity -> {
                    float f = entity.applyRotation(rotation);
                    f += entity.applyMirror(mirror) - entity.getYaw();
                    entity.refreshPositionAndAngles(vec3d2.x, vec3d2.y, vec3d2.z, f, entity.getPitch());
                    if (initializeMobs && entity instanceof MobEntity) {
                        ((MobEntity)entity).initialize(world, world.getLocalDifficulty(BlockPos.ofFloored(vec3d2)), SpawnReason.STRUCTURE, null, nbtCompound);
                    }

                    world.spawnEntityAndPassengers(entity);
                });
            }
        }
    }

}
