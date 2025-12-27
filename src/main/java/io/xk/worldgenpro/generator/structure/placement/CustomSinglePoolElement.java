package io.xk.worldgenpro.generator.structure.placement;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.JigsawReplacementStructureProcessor;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.minecraft.structure.StructureTemplate.process;

public class CustomSinglePoolElement extends StructurePoolElement {
    private static final Codec<Either<Identifier, StructureTemplate>> LOCATION_CODEC = Codec.of(CustomSinglePoolElement::encodeLocation, Identifier.CODEC.map(Either::left));
    public static final Codec<CustomSinglePoolElement> CODEC = RecordCodecBuilder.create(instance -> instance.group(locationGetter(), processorsGetter(), projectionGetter()).apply(instance, CustomSinglePoolElement::new));
    protected final Either<Identifier, StructureTemplate> location;
    protected final RegistryEntry<StructureProcessorList> processors;

    private static <T> DataResult<T> encodeLocation(Either<Identifier, StructureTemplate> location, DynamicOps<T> ops, T prefix) {
        Optional<Identifier> optional = location.left();
        return optional.isEmpty() ? DataResult.error(() -> "Can not serialize a runtime pool element") : Identifier.CODEC.encode((Identifier) optional.get(), ops, prefix);
    }

    protected static <E extends CustomSinglePoolElement> RecordCodecBuilder<E, RegistryEntry<StructureProcessorList>> processorsGetter() {
        return StructureProcessorType.REGISTRY_CODEC.fieldOf("processors").forGetter(pool -> pool.processors);
    }

    protected static <E extends CustomSinglePoolElement> RecordCodecBuilder<E, Either<Identifier, StructureTemplate>> locationGetter() {
        return LOCATION_CODEC.fieldOf("location").forGetter(pool -> pool.location);
    }

    public CustomSinglePoolElement(Either<Identifier, StructureTemplate> location, RegistryEntry<StructureProcessorList> processors, StructurePool.Projection projection) {
        super(projection);
        this.location = location;
        this.processors = processors;
    }

    @Override
    public Vec3i getStart(StructureTemplateManager structureTemplateManager, BlockRotation rotation) {
        StructureTemplate structureTemplate = this.getStructure(structureTemplateManager);
        return structureTemplate.getRotatedSize(rotation);
    }

    public StructureTemplate getStructure(StructureTemplateManager structureTemplateManager) {
        return this.location.map(structureTemplateManager::getTemplateOrBlank, Function.identity());
    }

    public List<StructureTemplate.StructureBlockInfo> getDataStructureBlocks(StructureTemplateManager structureTemplateManager, BlockPos pos, BlockRotation rotation, boolean mirroredAndRotated) {
        StructureTemplate structureTemplate = this.getStructure(structureTemplateManager);
        List<StructureTemplate.StructureBlockInfo> list = structureTemplate.getInfosForBlock(pos, new StructurePlacementData().setRotation(rotation), Blocks.STRUCTURE_BLOCK, mirroredAndRotated);
        List<StructureTemplate.StructureBlockInfo> list2 = Lists.<StructureTemplate.StructureBlockInfo>newArrayList();

        for (StructureTemplate.StructureBlockInfo structureBlockInfo : list) {
            NbtCompound nbtCompound = structureBlockInfo.nbt();
            if (nbtCompound != null) {
                StructureBlockMode structureBlockMode = StructureBlockMode.valueOf(nbtCompound.getString("mode"));
                if (structureBlockMode == StructureBlockMode.DATA) {
                    list2.add(structureBlockInfo);
                }
            }
        }

        return list2;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getStructureBlockInfos(StructureTemplateManager structureTemplateManager, BlockPos pos, BlockRotation rotation, Random random) {
        StructureTemplate structureTemplate = this.getStructure(structureTemplateManager);
        ObjectArrayList<StructureTemplate.StructureBlockInfo> objectArrayList = structureTemplate.getInfosForBlock(pos, new StructurePlacementData().setRotation(rotation), Blocks.JIGSAW, true);

        Util.shuffle(objectArrayList, random);
        sort(objectArrayList);
        return objectArrayList;
    }

    @VisibleForTesting
    static void sort(List<StructureTemplate.StructureBlockInfo> blocks) {
        blocks.sort(Comparator.comparingInt(block -> Nullables.mapOrElse(((StructureTemplate.StructureBlockInfo) block).nbt(), nbt -> nbt.getInt("selection_priority"), 0)).reversed());
    }

    @Override
    public BlockBox getBoundingBox(StructureTemplateManager structureTemplateManager, BlockPos pos, BlockRotation rotation) {
        StructureTemplate structureTemplate = this.getStructure(structureTemplateManager);
        return structureTemplate.calculateBoundingBox(new StructurePlacementData().setRotation(rotation), pos);
    }

    @Override
    public boolean generate(StructureTemplateManager structureTemplateManager, StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, BlockPos pos, BlockPos pivot, BlockRotation rotation, BlockBox box, Random random, boolean keepJigsaws) {
        StructureTemplate structureTemplate = this.getStructure(structureTemplateManager);
        StructurePlacementData structurePlacementData = this.createPlacementData(rotation, box, keepJigsaws);

        if (!place(structureTemplate, world, pos, pivot, structurePlacementData, random, Block.NO_REDRAW)) { // 18?
            // Block.FORCE_STATE | Block.REDRAW_ON_MAIN_THREAD
            return false;
        } else {

            for (StructureTemplate.StructureBlockInfo structureBlockInfo : StructureTemplate.process(world, pos, pivot, structurePlacementData, this.getDataStructureBlocks(structureTemplateManager, pos, rotation, false))) {
                this.method_16756(world, structureBlockInfo, pos, rotation, random, box);
            }

            return true;
        }
    }

    public boolean place(StructureTemplate template, ServerWorldAccess world, BlockPos pos, BlockPos pivot, StructurePlacementData placementData, Random random, int flags) {
        if (template.blockInfoLists.isEmpty()) {
            return false;
        } else {
            List<StructureTemplate.StructureBlockInfo> list = placementData.getRandomBlockInfos(template.blockInfoLists, pos).getAll();
            if ((!list.isEmpty() || !placementData.shouldIgnoreEntities() && !template.entities.isEmpty()) && template.getSize().getX() >= 1 && template.getSize().getY() >= 1 && template.getSize().getZ() >= 1) {
                BlockBox blockBox = placementData.getBoundingBox();
                List<BlockPos> list2 = Lists.<BlockPos>newArrayListWithCapacity(placementData.shouldPlaceFluids() ? list.size() : 0);
                List<BlockPos> list3 = Lists.<BlockPos>newArrayListWithCapacity(placementData.shouldPlaceFluids() ? list.size() : 0);

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
                                    ((FluidFillable) blockState.getBlock()).tryFillWithFluid(world, blockPos, blockState, fluidState);
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
                        BlockPos blockPos2 = (BlockPos) iterator.next();
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
                                ((FluidFillable) block).tryFillWithFluid(world, blockPos2, blockState2, fluidState2);
                                bl = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (!placementData.shouldIgnoreEntities()) {
                    template.spawnEntities(world, pos, placementData.getMirror(), placementData.getRotation(), placementData.getPosition(), blockBox, placementData.shouldInitializeMobs());
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public StructurePlacementData createPlacementData(BlockRotation rotation, BlockBox box, boolean keepJigsaws) {
        StructurePlacementData structurePlacementData = new StructurePlacementData();
        structurePlacementData.setBoundingBox(box);
        structurePlacementData.setRotation(rotation);
        structurePlacementData.setUpdateNeighbors(true);
        structurePlacementData.setIgnoreEntities(false);
        structurePlacementData.addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);
        structurePlacementData.setInitializeMobs(true);
        if (!keepJigsaws) {
            structurePlacementData.addProcessor(JigsawReplacementStructureProcessor.INSTANCE);
        }

        this.processors.value().getList().forEach(structurePlacementData::addProcessor);
        this.getProjection().getProcessors().forEach(structurePlacementData::addProcessor);
        return structurePlacementData;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return StructurePoolElementType.SINGLE_POOL_ELEMENT;
    }

    public String toString() {
        return "CustomSingle[" + this.location + "]";
    }
}
