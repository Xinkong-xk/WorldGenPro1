package io.xk.worldgenpro.vanilla.enhance.structure.generator;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.vanilla.enhance.structure.CustomStructurePoolGenerator;
import io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.JigsawStructureReflect;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructurePoolMixinReflect;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.*;
import net.minecraft.structure.pool.*;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

import java.util.*;

public class VanillaStructurePoolBasedGenerator {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static BlockRotation rotationFromFacing(Direction dir) {
        return switch (dir) {
            case SOUTH -> BlockRotation.NONE;
            case WEST  -> BlockRotation.CLOCKWISE_90;
            case NORTH -> BlockRotation.CLOCKWISE_180;
            case EAST  -> BlockRotation.COUNTERCLOCKWISE_90;
            default ->
                // Y 轴方向无法决定水平面旋转，按照原版保持 NONE
                    BlockRotation.NONE;
        };
    }

    public static Direction[] dirs = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    private static BlockRotation getRotation(
            JigsawStructure jigsawStructure,
            ChunkRandom chunkRandom,
            BlockPos pos,
            Structure.Context context
    ) {
        if (jigsawStructure == null) {
            return BlockRotation.random(chunkRandom);
        }

        // 1. pool-facing: explicit direction
        Direction direction = JigsawStructureReflect.getDirection(jigsawStructure);
        if (direction != null) {
            return rotationFromFacing(direction);
        }

        // 2. biome-facing
        Biome targetBiome = JigsawStructureReflect.getFacingBiome(jigsawStructure);
        if (targetBiome != null) {
            ChunkPos center = new ChunkPos(pos);
            BiomeSource biomeSource = context.biomeSource();
            // 扫描半径
            int radius = 7;
            int y = pos.getY();
            // 成功扫描到的方向
            ObjectArrayList<Direction> hitDirs = new ObjectArrayList<>(4);

            for (Direction dir : dirs) {

                ChunkPos neighbor = new ChunkPos(
                        center.x + dir.getOffsetX(),
                        center.z + dir.getOffsetZ()
                );

                int scanX = neighbor.getCenterX();
                int scanZ = neighbor.getCenterZ();

                Set<RegistryEntry<Biome>> biomes = biomeSource.getBiomesInArea(
                        scanX,
                        y,
                        scanZ,
                        radius,
                        context.noiseConfig().getMultiNoiseSampler()
                );
                // 直接比对 biome.value()，更快，不需要 HashSet
                for (RegistryEntry<Biome> entry : biomes) {
                    if (entry.value() == targetBiome) {
                        hitDirs.add(dir);
                        break;
                    }
                }
            }

            if (!hitDirs.isEmpty()) {
                Direction chosen = hitDirs.get(chunkRandom.nextInt(hitDirs.size()));
                return rotationFromFacing(chosen);
            }
        }

        return BlockRotation.random(chunkRandom);
    }



    public static Optional<Structure.StructurePosition> generate(
            Structure.Context context,
            RegistryEntry<StructurePool> structurePool,
            Optional<Identifier> id,
            int size,
            BlockPos pos,
            boolean useExpansionHack,
            Optional<Heightmap.Type> projectStartToHeightmap,
            int maxDistanceFromCenter,
            StructurePoolAliasLookup aliasLookup,
            JigsawStructure jigsawStructure
    ) {
        // TODO 创建新的结构上下文
        io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext structureContext = new io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext();

        // 获取各个管理器
        DynamicRegistryManager dynamicRegistryManager = context.dynamicRegistryManager();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
        HeightLimitView heightLimitView = context.world();
        ChunkRandom chunkRandom = context.random();
        Registry<StructurePool> registry = dynamicRegistryManager.get(RegistryKeys.TEMPLATE_POOL);

        // 动态计算旋转
        BlockRotation blockRotation = getRotation(jigsawStructure,chunkRandom,pos,context);

        StructurePool structurePool2 = (StructurePool)structurePool.getKey()
                .flatMap(registryKey -> registry.getOrEmpty(aliasLookup.lookup(registryKey)))
                .orElse(structurePool.value());

        StructurePoolElement structurePoolElement = EmptyPoolElement.INSTANCE;
        try {
            structurePoolElement = StructurePoolMixinReflect.customRandomElement(structurePool2,context,id.orElse(null),size,pos,useExpansionHack,maxDistanceFromCenter,chunkRandom);
        }catch (Exception e){
            e.printStackTrace();
        }

        if (structurePoolElement == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        } else {
            BlockPos blockPos;
            if (id.isPresent()) {
                Identifier identifier = (Identifier)id.get();
                Optional<BlockPos> optional = findStartingJigsawPos(structurePoolElement, identifier, pos, blockRotation, structureTemplateManager, chunkRandom);
                if (optional.isEmpty()) {
                    LOGGER.error(
                            "No starting jigsaw {} found in start pool {}", identifier, structurePool.getKey().map(key -> key.getValue().toString()).orElse("<unregistered>")
                    );
                    return Optional.empty();
                }

                blockPos = (BlockPos)optional.get();
            } else {
                blockPos = pos;
            }

            Vec3i vec3i = blockPos.subtract(pos);
            BlockPos blockPos2 = pos.subtract(vec3i);
            PoolStructurePiece poolStructurePiece = new PoolStructurePiece(
                    structureTemplateManager,
                    structurePoolElement,
                    blockPos2,
                    structurePoolElement.getGroundLevelDelta(),
                    blockRotation,
                    structurePoolElement.getBoundingBox(structureTemplateManager, blockPos2, blockRotation)
            );

            BlockBox blockBox = poolStructurePiece.getBoundingBox();
            int i = (blockBox.getMaxX() + blockBox.getMinX()) / 2;
            int j = (blockBox.getMaxZ() + blockBox.getMinZ()) / 2;
            int k;
            if (projectStartToHeightmap.isPresent()) {
                k = pos.getY() + chunkGenerator.getHeightOnGround(i, j, (Heightmap.Type)projectStartToHeightmap.get(), heightLimitView, context.noiseConfig());
            } else {
                k = blockPos2.getY();
            }

            int l = blockBox.getMinY() + poolStructurePiece.getGroundLevelDelta();
            poolStructurePiece.translate(0, k - l, 0);

            // TODO 添加起始结构的element和piece
            StructureContext:{
                // 添加计数
                structureContext.addPlacedElement(structurePoolElement);
                structureContext.addPiece(poolStructurePiece);
                structureContext.addElementCount(structurePool2);
            }

            // 真正的起始点坐标
            BlockPos startPos = blockPos2.add(0, k - l, 0);

            int m = k + vec3i.getY();
            return Optional.of(
                    new Structure.StructurePosition(
                            new BlockPos(i, m, j),
                            collector -> {
                                List<PoolStructurePiece> list = Lists.<PoolStructurePiece>newArrayList();
                                list.add(poolStructurePiece);
                                if (size > 0) {
                                    Box box = new Box(
                                            i - maxDistanceFromCenter,
                                            m - maxDistanceFromCenter,
                                            j - maxDistanceFromCenter,
                                            i + maxDistanceFromCenter + 1,
                                            m + maxDistanceFromCenter + 1,
                                            j + maxDistanceFromCenter + 1
                                    );
                                    VoxelShape voxelShape = VoxelShapes.combineAndSimplify(VoxelShapes.cuboid(box), VoxelShapes.cuboid(Box.from(blockBox)), BooleanBiFunction.ONLY_FIRST);
                                    generate(
                                            context.noiseConfig(),
                                            size,
                                            useExpansionHack,
                                            chunkGenerator,
                                            structureTemplateManager,
                                            heightLimitView,
                                            chunkRandom,
                                            registry,
                                            poolStructurePiece,
                                            list,
                                            voxelShape,
                                            aliasLookup,
                                            // 新参数传入
                                            maxDistanceFromCenter,
                                            startPos,
                                            id.orElseGet(() -> new Identifier("","")),
                                            structureContext,
                                            new BlockPos(i, m, j),
                                            context
                                    );
                                    list.forEach(collector::addPiece);
                                }
                            }
                    )
            );
        }
    }

    private static Optional<BlockPos> findStartingJigsawPos(
            StructurePoolElement pool, Identifier id, BlockPos pos, BlockRotation rotation, StructureTemplateManager structureManager, ChunkRandom random
    ) {
        List<StructureTemplate.StructureBlockInfo> list = pool.getStructureBlockInfos(structureManager, pos, rotation, random);
        Optional<BlockPos> optional = Optional.empty();

        for (StructureTemplate.StructureBlockInfo structureBlockInfo : list) {
            Identifier identifier = Identifier.tryParse(
                    ((NbtCompound) Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null")).getString("name")
            );
            if (id.equals(identifier)) {
                optional = Optional.of(structureBlockInfo.pos());
                break;
            }
        }

        return optional;
    }

    // 修改,添加了许多参数
    private static void generate(
            NoiseConfig noiseConfig,
            int maxSize,
            boolean modifyBoundingBox,
            ChunkGenerator chunkGenerator,
            StructureTemplateManager structureTemplateManager,
            HeightLimitView heightLimitView,
            Random random,
            Registry<StructurePool> structurePoolRegistry,
            PoolStructurePiece firstPiece,
            List<PoolStructurePiece> pieces,
            VoxelShape pieceShape,
            StructurePoolAliasLookup aliasLookup,
            // 添加的新参数
            int maxDistanceFromCenter,
            BlockPos startPos,
            Identifier id,
            StructureContext context,
            BlockPos centerPos,
            Structure.Context vanillaContext
    ) {
        CustomStructurePoolGenerator customStructurePoolGenerator = new CustomStructurePoolGenerator(
                structurePoolRegistry, maxSize, chunkGenerator, structureTemplateManager, pieces, random,maxDistanceFromCenter,startPos,id,context,centerPos,vanillaContext
        );
        try{
            customStructurePoolGenerator.generatePiece(firstPiece,
                    new MutableObject<>(pieceShape),
                    0,
                    modifyBoundingBox,
                    heightLimitView,
                    noiseConfig,
                    aliasLookup,
                    maxSize
            );
        }catch (Exception e){
            e.printStackTrace();
        }

        while (customStructurePoolGenerator.structurePieces.hasNext()) {
            StructurePoolBasedGenerator.ShapedPoolStructurePiece shapedPoolStructurePiece = customStructurePoolGenerator.structurePieces.next();
            try {
                customStructurePoolGenerator.generatePiece(
                        shapedPoolStructurePiece.piece(),
                        shapedPoolStructurePiece.pieceShape(),
                        shapedPoolStructurePiece.currentSize(),
                        modifyBoundingBox,
                        heightLimitView,
                        noiseConfig,
                        aliasLookup,
                        maxSize
                );
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 修改，传入的maxDistanceFromCenter为动态值
     * @param world 维度
     * @param structurePool 结构池
     * @param id 标识符
     * @param size 尺寸
     * @param pos 坐标
     * @param keepJigsaws 是否保留拼图方块
     * @return
     */
    public static boolean generate(ServerWorld world,
                                   RegistryEntry<StructurePool> structurePool,
                                   Identifier id,
                                   int size,
                                   BlockPos pos,
                                   boolean keepJigsaws
    ) {
        ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();
        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();
        StructureAccessor structureAccessor = world.getStructureAccessor();
        Random random = world.getRandom();
        Structure.Context context = new Structure.Context(
                world.getRegistryManager(),
                chunkGenerator,
                chunkGenerator.getBiomeSource(),
                world.getChunkManager().getNoiseConfig(),
                structureTemplateManager,
                world.getSeed(),
                new ChunkPos(pos),
                world,
                biome -> true
        );
        Optional<Structure.StructurePosition> optional = generate(
                context, structurePool, Optional.of(id), size, pos, false, Optional.empty(), WorldGenPro.configFile.maxDistanceFromCenter, StructurePoolAliasLookup.EMPTY,null
        );
        if (optional.isPresent()) {
            StructurePiecesCollector structurePiecesCollector = ((Structure.StructurePosition)optional.get()).generate();
            for (StructurePiece structurePiece : structurePiecesCollector.toList().pieces()) {
                if (structurePiece instanceof PoolStructurePiece poolStructurePiece) {
                    poolStructurePiece.generate(world, structureAccessor, chunkGenerator, random, BlockBox.infinite(), pos, keepJigsaws);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
