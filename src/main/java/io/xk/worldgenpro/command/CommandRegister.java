package io.xk.worldgenpro.command;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.api.CommonRegister;
import io.xk.worldgenpro.addon.roadgen.api.data.RegionStructureMap;
import io.xk.worldgenpro.addon.semantic.GeoSemanticStructureManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.heightprovider.HeightProviderType;
import net.minecraft.world.gen.structure.Structure;
// getString(ctx, "string")
// word()

import static net.minecraft.server.command.CommandManager.*;

import static net.minecraft.server.command.CommandManager.literal;

public class CommandRegister implements CommonRegister {

    @Override
    public void registerServer() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("config.set")
                                        .then(literal("maxDistanceFromCenter")
                                                .then(argument("value", IntegerArgumentType.integer(1))
                                                        .executes(context -> setMaxDistance(context, IntegerArgumentType.getInteger(context, "value")))
                                                )
                                        )
                                )
                )
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("config.value")
                                        .then(literal("maxDistanceFromCenter")
                                                .executes(context -> printMaxDistance(context))
                                        )
                                )
                )
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("RandomSpreadStructurePlacement")
                                                .executes(context -> test1(context))
                                        )
                                )
                )
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("HeightMap")
                                                .executes(context -> {
                                                    try {
                                                        Chunk chunk = context.getSource().getWorld().getChunk(context.getSource().getPlayer().getBlockPos());

                                                        BlockPos blockPos = context.getSource().getPlayer().getBlockPos();
                                                        context.getSource().sendFeedback(
                                                                () -> Text.literal("此处高度: "+ chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(blockPos.getX()&15,blockPos.getZ()&15)),
                                                                true
                                                        );
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }


                                                    return 1;
                                                })
                                        )
                                )
                )
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("topY")
                                                .executes(context -> {
                                                    try {
                                                        Chunk chunk = context.getSource().getWorld().getChunk(context.getSource().getPlayer().getBlockPos());

                                                        BlockPos blockPos = context.getSource().getPlayer().getBlockPos();
                                                        context.getSource().sendFeedback(
                                                                () -> Text.literal("此处高度: "+ context.getSource().getWorld().getTopY(Heightmap.Type.WORLD_SURFACE,blockPos.getX(),blockPos.getZ())),
                                                                true
                                                        );
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }


                                                    return 1;
                                                })
                                        )
                                )
                )
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("getChunk")
                                                .executes(context -> {
                                                    try {
                                                        Chunk chunk = context.getSource().getWorld().getChunk(100000,100000, ChunkStatus.STRUCTURE_REFERENCES,false);

                                                        context.getSource().sendFeedback(
                                                                () -> {
                                                                    if (chunk != null) {
                                                                        return Text.literal("区块: " + chunk.toString());
                                                                    }else{
                                                                        return Text.literal("区块: " + "null");
                                                                    }
                                                                },
                                                                true
                                                        );
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }


                                                    return 1;
                                                })
                                        )
                                )
                )
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("biome")
                                                .executes(context -> {
                                                    try {
                                                        int test = 10000;
                                                        HeightProvider heightProvider = new HeightProvider() {
                                                            @Override
                                                            public int get(Random random, HeightContext context) {
                                                                return random.nextInt(128);
                                                            }

                                                            @Override
                                                            public HeightProviderType<?> getType() {
                                                                return null;
                                                            }
                                                        };

                                                        java.util.Optional<Heightmap.Type> typeOptional = java.util.Optional.of(Heightmap.Type.WORLD_SURFACE);
                                                        ChunkGenerator chunkGenerator = context.getSource().getWorld().getChunkManager().getChunkGenerator();
                                                        ServerWorld world = context.getSource().getWorld();
                                                        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
                                                        for (int i = 0; i < test; i++){
                                                            GeoSemanticStructureManager.computeStructureY_LogicOnly(
                                                                    heightProvider,
                                                                    typeOptional,
                                                                    chunkGenerator,
                                                                    i*8,i*8,
                                                                    world
                                                            );
                                                        }
                                                        stopwatch.stop();
                                                        context.getSource().sendFeedback(
                                                                () -> Text.literal("采样高度图: "+ test+"次,耗时: "+stopwatch.elapsed()),
                                                                true
                                                        );

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                )
        );


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("heightMap_test_2")
                                                .executes(context -> {
                                                    try {
                                                        BiomeSource biomeSource = context.getSource().getWorld().getChunkManager().getChunkGenerator().getBiomeSource();
                                                        int test = 10000;
                                                        var m = context.getSource().getWorld().getChunkManager().getNoiseConfig().getMultiNoiseSampler();
                                                        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
                                                        for (int i = 0; i < test; i++){
                                                            biomeSource.getBiome(1+i,1,1+i,m);
                                                        }
                                                        stopwatch.stop();
                                                        context.getSource().sendFeedback(
                                                                () -> Text.literal("采样: "+ test+"次,耗时: "+stopwatch.elapsed()),
                                                                true
                                                        );

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                )
        );

        // ============== GridGraph测试 ==============
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("RegionStructureMap")
                                                .executes(context -> {
                                                    try {
                                                        RegionStructureMap regionStructureMap = WorldGenPro.roadConnectionConfigManager.getRegionStructureMap(context.getSource().getWorld().getRegistryKey());
                                                        context.getSource().sendFeedback(
                                                                () -> {
                                                                    return Text.literal("RegionStructureMap: " + regionStructureMap.toString());
                                                                },
                                                                true
                                                        );
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }


                                                    return 1;
                                                })
                                        )
                                )
                )
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("wgp")
                                .then(literal("test")
                                        .then(literal("Add.RegionStructureMap")
                                                .executes(context -> {
                                                    try {
                                                        RegionStructureMap regionStructureMap = WorldGenPro.roadConnectionConfigManager.getRegionStructureMap(context.getSource().getWorld().getRegistryKey());
                                                        BlockPos pos = BlockPos.ofFloored(context.getSource().getPosition());
                                                        regionStructureMap.regionMap.get("minecraft:villages").addPoint(pos.getX(),pos.getZ(),"villages_"+pos.toString());
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                    return 1;
                                                })
                                        )
                                )
                )
        );

    }


    private int test1(CommandContext<ServerCommandSource> context) {
        ChunkPos this_pos = context.getSource().getPlayer().getChunkPos();
        ServerWorld world = (ServerWorld) context.getSource().getWorld();
        int y = context.getSource().getPlayer().getBlockY();

        BlockPos pos = getStructurePlacement(
                getStructure(new Identifier("minecraft:village_plains"),world),world)
                .getStartChunk(world.getSeed(),this_pos.x,this_pos.z)
                .getCenterAtY(y);

        context.getSource().sendFeedback(
                () -> Text.literal("平原村庄在: "+ this_pos.toString() + "区块坐标上在: " + pos.toString()),
                true
        );

        System.out.println("平原村庄在: "+ this_pos.toString() + "区块坐标上在: " + pos.toString());

        return 1;
    }

    public Structure getStructure(Identifier identifier,ServerWorld world){
        return world.getServer().getRegistryManager().get(RegistryKeys.STRUCTURE).get(identifier);
    }

    public RandomSpreadStructurePlacement getStructurePlacement(Structure structure, ServerWorld world) {
        Registry<StructureSet> structureSetRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE_SET);
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
    }

    private int setMaxDistance(CommandContext<ServerCommandSource> context, int value) {
        WorldGenPro.configFile.maxDistanceFromCenter = value;
        WorldGenPro.configFile.save();
        context.getSource().sendFeedback(
                () -> Text.literal("pool.maxDistanceFromCenter 已修改为: " + value),
                true
        );
        return value;
    }

    private int printMaxDistance(CommandContext<ServerCommandSource> context) {
        System.out.println(WorldGenPro.configFile.maxDistanceFromCenter);
        context.getSource().sendFeedback(
                () -> Text.literal("pool.maxDistanceFromCenter 的值为: " + WorldGenPro.configFile.maxDistanceFromCenter),
                true
        );
        return 1;
    }
}
