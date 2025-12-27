//package io.xk.worldgenpro.mixin.vanilla.enhance.pool;
//
//import com.google.common.collect.Lists;
//import io.xk.worldgenpro.WorldGenPro;
//import io.xk.worldgenpro.vanilla.enhance.pool.CustomStructurePoolGenerator;
//import io.xk.worldgenpro.vanilla.enhance.pool.context.StructureContext;
//import io.xk.worldgenpro.vanilla.enhance.pool.reflect.StructurePoolMixinReflect;
//import net.minecraft.registry.DynamicRegistryManager;
//import net.minecraft.registry.Registry;
//import net.minecraft.registry.RegistryKeys;
//import net.minecraft.registry.entry.RegistryEntry;
//import net.minecraft.server.world.ServerWorld;
//import net.minecraft.pool.*;
//import net.minecraft.pool.pool.*;
//import net.minecraft.pool.pool.alias.StructurePoolAliasLookup;
//import net.minecraft.util.BlockRotation;
//import net.minecraft.util.Identifier;
//import net.minecraft.util.function.BooleanBiFunction;
//import net.minecraft.util.math.*;
//import net.minecraft.util.math.random.ChunkRandom;
//import net.minecraft.util.math.random.Random;
//import net.minecraft.util.shape.VoxelShape;
//import net.minecraft.util.shape.VoxelShapes;
//import net.minecraft.world.HeightLimitView;
//import net.minecraft.world.Heightmap;
//import net.minecraft.world.gen.StructureAccessor;
//import net.minecraft.world.gen.chunk.ChunkGenerator;
//import net.minecraft.world.gen.noise.NoiseConfig;
//import net.minecraft.world.gen.pool.Structure;
//import org.apache.commons.lang3.mutable.MutableObject;
//import org.slf4j.Logger;
//import org.spongepowered.asm.mixin.*;
//
//import java.util.List;
//import java.util.Optional;
//
//@Mixin(StructurePoolBasedGenerator.class)
//public class StructurePoolBasedGeneratorMixin {
//    /**
//     * @author XK
//     * @reason
//     */
//    @Overwrite
//    public static boolean generate(
//            ServerWorld world,
//            RegistryEntry<StructurePool> structurePool,
//            Identifier id,
//            int size,
//            BlockPos pos,
//            boolean keepJigsaws
//    ) {
//        ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();
//        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();
//        StructureAccessor structureAccessor = world.getStructureAccessor();
//        Random random = world.getRandom();
//        Structure.Context context = new Structure.Context(
//                world.getRegistryManager(),
//                chunkGenerator,
//                chunkGenerator.getBiomeSource(),
//                world.getChunkManager().getNoiseConfig(),
//                structureTemplateManager,
//                world.getSeed(),
//                new ChunkPos(pos),
//                world,
//                biome -> true
//        );
//        Optional<Structure.StructurePosition> optional = generate(
//                context, structurePool, Optional.of(id), size, pos, false, Optional.empty(), WorldGenPro.worldConfig.maxDistanceFromCenter, StructurePoolAliasLookup.EMPTY
//        );
//        if (optional.isPresent()) {
//            StructurePiecesCollector structurePiecesCollector = ((Structure.StructurePosition)optional.get()).generate();
//            for (StructurePiece structurePiece : structurePiecesCollector.toList().pieces()) {
//                if (structurePiece instanceof PoolStructurePiece poolStructurePiece) {
//                    poolStructurePiece.generate(world, structureAccessor, chunkGenerator, random, BlockBox.infinite(), pos, keepJigsaws);
//                }
//            }
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//
//    // ========== 修改原版随机模板池方法 ==========
//
//    @Shadow
//    @Final
//    static Logger LOGGER;
//
//    @Shadow
//    private static Optional<BlockPos> findStartingJigsawPos(
//            StructurePoolElement pool, Identifier id, BlockPos pos, BlockRotation rotation, StructureTemplateManager structureManager, ChunkRandom random
//    ) {return null;}
//
//
//    private static BlockRotation getRotation(Structure.Context context){
//        return null;
//    }
//
//
//    /**
//     * @author xk
//     * @reason
//     */
//    @Overwrite
//    public static Optional<Structure.StructurePosition> generate(
//            Structure.Context context,
//            RegistryEntry<StructurePool> structurePool,
//            Optional<Identifier> id,
//            int size,
//            BlockPos pos,
//            boolean useExpansionHack,
//            Optional<Heightmap.Type> projectStartToHeightmap,
//            int maxDistanceFromCenter,
//            StructurePoolAliasLookup aliasLookup
//    ) {
//        // TODO 创建新的结构上下文
//        io.xk.worldgenpro.vanilla.enhance.pool.context.StructureContext structureContext = new io.xk.worldgenpro.vanilla.enhance.pool.context.StructureContext();
//
//        // 获取各个管理器
//        DynamicRegistryManager dynamicRegistryManager = context.dynamicRegistryManager();
//        ChunkGenerator chunkGenerator = context.chunkGenerator();
//        StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
//        HeightLimitView heightLimitView = context.world();
//        ChunkRandom chunkRandom = context.random();
//        Registry<StructurePool> registry = dynamicRegistryManager.get(RegistryKeys.TEMPLATE_POOL);
//
//        BlockRotation blockRotation = BlockRotation.random(chunkRandom);
//
//        blockRotation = getRotation(context);
//
//        StructurePool structurePool2 = (StructurePool)structurePool.getKey()
//                .flatMap(registryKey -> registry.getOrEmpty(aliasLookup.lookup(registryKey)))
//                .orElse(structurePool.value());
//
//        StructurePoolElement structurePoolElement = EmptyPoolElement.INSTANCE;
//        try {
//            structurePoolElement = StructurePoolMixinReflect.customRandomElement(structurePool2,context,id.orElse(null),size,pos,useExpansionHack,maxDistanceFromCenter,chunkRandom);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        if (structurePoolElement == EmptyPoolElement.INSTANCE) {
//            return Optional.empty();
//        } else {
//            BlockPos blockPos;
//            if (id.isPresent()) {
//                Identifier identifier = (Identifier)id.get();
//                Optional<BlockPos> optional = findStartingJigsawPos(structurePoolElement, identifier, pos, blockRotation, structureTemplateManager, chunkRandom);
//                if (optional.isEmpty()) {
//                    LOGGER.error(
//                            "No starting jigsaw {} found in start pool {}", identifier, structurePool.getKey().map(key -> key.getValue().toString()).orElse("<unregistered>")
//                    );
//                    return Optional.empty();
//                }
//
//                blockPos = (BlockPos)optional.get();
//            } else {
//                blockPos = pos;
//            }
//
//            Vec3i vec3i = blockPos.subtract(pos);
//            BlockPos blockPos2 = pos.subtract(vec3i);
//            PoolStructurePiece poolStructurePiece = new PoolStructurePiece(
//                    structureTemplateManager,
//                    structurePoolElement,
//                    blockPos2,
//                    structurePoolElement.getGroundLevelDelta(),
//                    blockRotation,
//                    structurePoolElement.getBoundingBox(structureTemplateManager, blockPos2, blockRotation)
//            );
//
//            BlockBox blockBox = poolStructurePiece.getBoundingBox();
//            int i = (blockBox.getMaxX() + blockBox.getMinX()) / 2;
//            int j = (blockBox.getMaxZ() + blockBox.getMinZ()) / 2;
//            int k;
//            if (projectStartToHeightmap.isPresent()) {
//                k = pos.getY() + chunkGenerator.getHeightOnGround(i, j, (Heightmap.Type)projectStartToHeightmap.get(), heightLimitView, context.noiseConfig());
//            } else {
//                k = blockPos2.getY();
//            }
//
//            int l = blockBox.getMinY() + poolStructurePiece.getGroundLevelDelta();
//            poolStructurePiece.translate(0, k - l, 0);
//
//            // TODO 添加起始结构的element和piece
//            StructureContext:{
//                structureContext.addPlacedElement(structurePoolElement);
//                structureContext.addPiece(poolStructurePiece);
//            }
//
//            // 真正的起始点坐标
//            BlockPos startPos = blockPos2.add(0, k - l, 0);
//
//            int m = k + vec3i.getY();
//            return Optional.of(
//                    new Structure.StructurePosition(
//                            new BlockPos(i, m, j),
//                            collector -> {
//                                List<PoolStructurePiece> list = Lists.<PoolStructurePiece>newArrayList();
//                                list.add(poolStructurePiece);
//                                if (size > 0) {
//                                    Box box = new Box(
//                                            i - maxDistanceFromCenter,
//                                            m - maxDistanceFromCenter,
//                                            j - maxDistanceFromCenter,
//                                            i + maxDistanceFromCenter + 1,
//                                            m + maxDistanceFromCenter + 1,
//                                            j + maxDistanceFromCenter + 1
//                                    );
//                                    VoxelShape voxelShape = VoxelShapes.combineAndSimplify(VoxelShapes.cuboid(box), VoxelShapes.cuboid(Box.from(blockBox)), BooleanBiFunction.ONLY_FIRST);
//                                    generate(
//                                            context.noiseConfig(),
//                                            size,
//                                            useExpansionHack,
//                                            chunkGenerator,
//                                            structureTemplateManager,
//                                            heightLimitView,
//                                            chunkRandom,
//                                            registry,
//                                            poolStructurePiece,
//                                            list,
//                                            voxelShape,
//                                            aliasLookup,
//                                            // 新参数传入
//                                            maxDistanceFromCenter,
//                                            startPos,
//                                            id.orElseGet(() -> new Identifier("","")),
//                                            structureContext,
//                                            new BlockPos(i, m, j)
//                                    );
//                                    list.forEach(collector::addPiece);
//                                }
//                            }
//                    )
//            );
//        }
//    }
//
//
//    private static void generate(
//            NoiseConfig noiseConfig,
//            int maxSize,
//            boolean modifyBoundingBox,
//            ChunkGenerator chunkGenerator,
//            StructureTemplateManager structureTemplateManager,
//            HeightLimitView heightLimitView,
//            Random random,
//            Registry<StructurePool> structurePoolRegistry,
//            PoolStructurePiece firstPiece,
//            List<PoolStructurePiece> pieces,
//            VoxelShape pieceShape,
//            StructurePoolAliasLookup aliasLookup,
//            // 添加的新参数
//            int maxDistanceFromCenter,
//            BlockPos startPos,
//            Identifier id,
//            StructureContext context,
//            BlockPos centerPos
//    ) {
//        CustomStructurePoolGenerator customStructurePoolGenerator = new CustomStructurePoolGenerator(
//                structurePoolRegistry, maxSize, chunkGenerator, structureTemplateManager, pieces, random,maxDistanceFromCenter,startPos,id,context,centerPos
//        );
//        try{
//            customStructurePoolGenerator.generatePiece(firstPiece, new MutableObject<>(pieceShape), 0, modifyBoundingBox, heightLimitView, noiseConfig, aliasLookup);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        while (customStructurePoolGenerator.structurePieces.hasNext()) {
//            StructurePoolBasedGenerator.ShapedPoolStructurePiece shapedPoolStructurePiece = customStructurePoolGenerator.structurePieces.next();
//            try {
//                customStructurePoolGenerator.generatePiece(
//                        shapedPoolStructurePiece.piece(),
//                        shapedPoolStructurePiece.pieceShape(),
//                        shapedPoolStructurePiece.currentSize(),
//                        modifyBoundingBox,
//                        heightLimitView,
//                        noiseConfig,
//                        aliasLookup
//                );
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//    }
//}
