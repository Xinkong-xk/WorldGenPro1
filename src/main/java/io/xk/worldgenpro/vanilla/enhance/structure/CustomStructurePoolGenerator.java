package io.xk.worldgenpro.vanilla.enhance.structure;

import com.google.common.collect.Lists;
import io.xk.worldgenpro.mixin.vanilla.enhance.structure.ShapedPoolStructurePieceAccessor;
import io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructurePoolMixinReflect;
import net.minecraft.block.JigsawBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.*;
import net.minecraft.structure.pool.*;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.PriorityIterator;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableObject;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CustomStructurePoolGenerator {
    private final Registry<StructurePool> registry;
    private final int maxSize;
    private final ChunkGenerator chunkGenerator;
    private final StructureTemplateManager structureTemplateManager;
    private final List<? super PoolStructurePiece> children;
    private final Random random;
    public final PriorityIterator<StructurePoolBasedGenerator.ShapedPoolStructurePiece> structurePieces = new PriorityIterator<>();

    // 添加参数
    private final int maxDistanceFromCenter;
    private final Identifier id;
    private final BlockPos startPos;
    private final BlockPos centerPos;

    private final Structure.Context vanillaContext;

    // 结构上下文
    public StructureContext structureContext;

    public CustomStructurePoolGenerator(
            Registry<StructurePool> registry,
            int maxSize,
            ChunkGenerator chunkGenerator,
            StructureTemplateManager structureTemplateManager,
            List<? super PoolStructurePiece> children,
            Random random,
            int maxDistanceFromCenter,
            BlockPos startPos,
            Identifier id,
            StructureContext context,
            BlockPos centerPos, Structure.Context vanillaContext) {

        this.structureContext = context;
        this.registry = registry;
        this.maxSize = maxSize;
        this.chunkGenerator = chunkGenerator;
        this.structureTemplateManager = structureTemplateManager;
        this.children = children;
        this.random = random;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.startPos = startPos;
        this.id = id;
        this.centerPos = centerPos;
        this.vanillaContext = vanillaContext;
    }

    public void generatePiece(
            PoolStructurePiece piece,
            MutableObject<VoxelShape> pieceShape,
            int currentSize,
            boolean modifyBoundingBox,
            HeightLimitView world,
            NoiseConfig noiseConfig,
            StructurePoolAliasLookup aliasLookup,
            int maxSize
    ) {
        StructurePoolElement structurePoolElement = piece.getPoolElement();
        BlockPos blockPos = piece.getPos();
        BlockRotation blockRotation = piece.getRotation();
        StructurePool.Projection projection = structurePoolElement.getProjection();
        boolean bl = projection == StructurePool.Projection.RIGID;
        MutableObject<VoxelShape> mutableObject = new MutableObject<>();
        BlockBox blockBox = piece.getBoundingBox();
        int i = blockBox.getMinY();

        label134:
        for (StructureTemplate.StructureBlockInfo structureBlockInfo : structurePoolElement.getStructureBlockInfos(this.structureTemplateManager, blockPos, blockRotation, this.random)) {
            Direction direction = JigsawBlock.getFacing(structureBlockInfo.state());
            BlockPos blockPos2 = structureBlockInfo.pos();
            BlockPos blockPos3 = blockPos2.offset(direction);
            int j = blockPos2.getY() - i;
            int k = -1;
            RegistryKey<StructurePool> registryKey = lookupPool(structureBlockInfo, aliasLookup);
            Optional<? extends RegistryEntry<StructurePool>> optional = this.registry.getEntry(registryKey);
            if (optional.isEmpty()) {
                StructurePoolBasedGenerator.LOGGER.warn("Empty or non-existent pool: {}", registryKey.getValue());
            } else {
                RegistryEntry<StructurePool> registryEntry = (RegistryEntry<StructurePool>)optional.get();
                if (registryEntry.value().getElementCount() == 0 && !registryEntry.matchesKey(StructurePools.EMPTY)) {
                    StructurePoolBasedGenerator.LOGGER.warn("Empty or non-existent pool: {}", registryKey.getValue());
                } else {
                    RegistryEntry<StructurePool> registryEntry2 = registryEntry.value().getFallback();
                    if (registryEntry2.value().getElementCount() == 0 && !registryEntry2.matchesKey(StructurePools.EMPTY)) {
                        StructurePoolBasedGenerator.LOGGER
                                .warn("Empty or non-existent fallback pool: {}", registryEntry2.getKey().map(key -> key.getValue().toString()).orElse("<unregistered>"));
                    } else {
                        boolean bl2 = blockBox.contains(blockPos3);
                        MutableObject<VoxelShape> mutableObject2;
                        if (bl2) {
                            mutableObject2 = mutableObject;
                            if (mutableObject.getValue() == null) {
                                mutableObject.setValue(VoxelShapes.cuboid(Box.from(blockBox)));
                            }
                        } else {
                            mutableObject2 = pieceShape;
                        }

                        List<StructurePoolElement> list = Lists.newArrayList();
                        if (currentSize != this.maxSize) {
                            // [1]
                            List<StructurePoolElement> list_1 = StructurePoolMixinReflect.getCustomElementIndicesInRandomOrder(
                                    registryEntry.value(),
                                    this,
                                    piece,
                                    pieceShape,
                                    currentSize,
                                    modifyBoundingBox,
                                    blockPos3,
                                    this.random,
                                    this.startPos,
                                    this.id,
                                    this.maxDistanceFromCenter,
                                    this.centerPos,
                                    maxSize,
                                    this.vanillaContext,
                                    this.structureContext
                            );
                            list.addAll(list_1);
                        }
                        // [2]
                        List<StructurePoolElement> list_2 = StructurePoolMixinReflect.getCustomElementIndicesInRandomOrder(
                                registryEntry2.value(),
                                this,
                                piece,
                                pieceShape,
                                currentSize,
                                modifyBoundingBox,
                                blockPos3,
                                this.random,
                                this.startPos,
                                this.id,
                                this.maxDistanceFromCenter,
                                this.centerPos,
                                maxSize,
                                this.vanillaContext,
                                this.structureContext
                        );

                        list.addAll(list_2);
                        int l = structureBlockInfo.nbt() != null ? structureBlockInfo.nbt().getInt("placement_priority") : 0;

                        for (StructurePoolElement structurePoolElement2 : list) {

                            if (structurePoolElement2 == EmptyPoolElement.INSTANCE) {
                                break;
                            }

                            for (BlockRotation blockRotation2 : BlockRotation.randomRotationOrder(this.random)) {
                                List<StructureTemplate.StructureBlockInfo> list2 = structurePoolElement2.getStructureBlockInfos(this.structureTemplateManager, BlockPos.ORIGIN, blockRotation2,random);

                                BlockBox blockBox2 = structurePoolElement2.getBoundingBox(this.structureTemplateManager, BlockPos.ORIGIN, blockRotation2 );

                                int m;
                                if (modifyBoundingBox && blockBox2.getBlockCountY() <= 16) {
                                    m = list2.stream().mapToInt(structureBlockInfox -> {
                                        if (!blockBox2.contains(structureBlockInfox.pos().offset(JigsawBlock.getFacing(structureBlockInfox.state())))) {
                                            return 0;
                                        } else {
                                            RegistryKey<StructurePool> registryKeyx = lookupPool(structureBlockInfox, aliasLookup);
                                            Optional<? extends RegistryEntry<StructurePool>> optionalx = this.registry.getEntry(registryKeyx);
                                            Optional<RegistryEntry<StructurePool>> optional2 = optionalx.map(entry -> ((StructurePool)entry.value()).getFallback());
                                            int ix = (Integer)optionalx.map(entry -> ((StructurePool)entry.value()).getHighestY(this.structureTemplateManager)).orElse(0);
                                            int jx = (Integer)optional2.map(entry -> ((StructurePool)entry.value()).getHighestY(this.structureTemplateManager)).orElse(0);
                                            return Math.max(ix, jx);
                                        }
                                    }).max().orElse(0);
                                } else {
                                    m = 0;
                                }

                                for (StructureTemplate.StructureBlockInfo structureBlockInfo2 : list2) {
                                    if (JigsawBlock.attachmentMatches(structureBlockInfo, structureBlockInfo2)) {
                                        BlockPos blockPos4 = structureBlockInfo2.pos();
                                        BlockPos blockPos5 = blockPos3.subtract(blockPos4);

                                        BlockBox blockBox3 = structurePoolElement2.getBoundingBox(this.structureTemplateManager, blockPos5, blockRotation2);

                                        int n = blockBox3.getMinY();
                                        StructurePool.Projection projection2 = structurePoolElement2.getProjection();
                                        boolean bl3 = projection2 == StructurePool.Projection.RIGID;
                                        int o = blockPos4.getY();
                                        int p = j - o + JigsawBlock.getFacing(structureBlockInfo.state()).getOffsetY();
                                        int q;
                                        if (bl && bl3) {
                                            q = i + p;
                                        } else {
                                            if (k == -1) {
                                                k = this.chunkGenerator.getHeightOnGround(blockPos2.getX(), blockPos2.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
                                            }

                                            q = k - o;
                                        }

                                        int r = q - n;
                                        BlockBox blockBox4 = blockBox3.offset(0, r, 0);
                                        BlockPos blockPos6 = blockPos5.add(0, r, 0);
                                        if (m > 0) {
                                            int s = Math.max(m + 1, blockBox4.getMaxY() - blockBox4.getMinY());
                                            blockBox4.encompass(new BlockPos(blockBox4.getMinX(), blockBox4.getMinY() + s, blockBox4.getMinZ()));
                                        }

                                        if (!VoxelShapes.matchesAnywhere(mutableObject2.getValue(), VoxelShapes.cuboid(Box.from(blockBox4).contract(0.25)), BooleanBiFunction.ONLY_SECOND)) {
                                            mutableObject2.setValue(VoxelShapes.combine(mutableObject2.getValue(), VoxelShapes.cuboid(Box.from(blockBox4)), BooleanBiFunction.ONLY_FIRST));
                                            int s = piece.getGroundLevelDelta();
                                            int t;
                                            if (bl3) {
                                                t = s - p;
                                            } else {
                                                t = structurePoolElement2.getGroundLevelDelta();
                                            }

                                            PoolStructurePiece poolStructurePiece = new PoolStructurePiece(
                                                    this.structureTemplateManager, structurePoolElement2, blockPos6, t, blockRotation2, blockBox4
                                            );
                                            int u;
                                            if (bl) {
                                                u = i + j;
                                            } else if (bl3) {
                                                u = q + o;
                                            } else {
                                                if (k == -1) {
                                                    k = this.chunkGenerator.getHeightOnGround(blockPos2.getX(), blockPos2.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
                                                }

                                                u = k + p / 2;
                                            }

                                            piece.addJunction(new JigsawJunction(blockPos3.getX(), u - j + s, blockPos3.getZ(), p, projection2));
                                            poolStructurePiece.addJunction(new JigsawJunction(blockPos2.getX(), u - o + t, blockPos2.getZ(), -p, projection));

                                            // TODO 记录element和piece
                                            recordElementAndPiece(poolStructurePiece,registryEntry.value());
                                            // TODO 添加进子片段列表
                                            this.children.add(poolStructurePiece);

                                            if (currentSize + 1 <= this.maxSize) {
                                                StructurePoolBasedGenerator.ShapedPoolStructurePiece shapedPoolStructurePiece = ShapedPoolStructurePieceAccessor.invokeNew(
                                                        poolStructurePiece, mutableObject2, currentSize + 1
                                                );
                                                this.structurePieces.enqueue(shapedPoolStructurePiece, l);
                                            }
                                            continue label134;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void recordElementAndPiece(PoolStructurePiece poolStructurePiece,StructurePool structurePool) {
        StructurePoolElement element = poolStructurePiece.getPoolElement();
        this.structureContext.addPiece(poolStructurePiece);
        this.structureContext.addPlacedElement(element);
        this.structureContext.addElementCount(structurePool);
    }

    private static RegistryKey<StructurePool> lookupPool(StructureTemplate.StructureBlockInfo structureBlockInfo, StructurePoolAliasLookup aliasLookup) {
        NbtCompound nbtCompound = (NbtCompound) Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null");
        RegistryKey<StructurePool> registryKey = StructurePools.of(nbtCompound.getString("pool"));
        return aliasLookup.lookup(registryKey);
    }
}