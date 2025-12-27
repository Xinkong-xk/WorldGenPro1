package io.xk.worldgenpro.test;

import com.mojang.brigadier.context.CommandContext;
import io.xk.worldgenpro.api.CommonRegister;
import io.xk.worldgenpro.generator.structure.placement.StructureGenerator;
import io.xk.worldgenpro.test.city.large.LargeCityTest;
import io.xk.worldgenpro.test.city.middle.MiddleCityTest;
import io.xk.worldgenpro.utils.Timer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;

public class TestCommandRegister implements CommonRegister {

    public void registerServer(){

        register_large_city();

        register_middle_city();

        register_small_city();
    }

    public void register_large_city() {
        CommandRegistrationCallback.EVENT.register((dispatcher,
                                                    registryAccess,
                                                    environment) ->
                dispatcher.register(literal("test.generator.city.large").executes(context -> {
            try {
                LargeCityTest.instance.generate(context);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return 1;
        })));
    }


    public void register_middle_city() {
        CommandRegistrationCallback.EVENT.register((dispatcher,
                                                    registryAccess,
                                                    environment) ->
                dispatcher.register(literal("test.generator.city.middle").executes(context -> {
                    try {
                        MiddleCityTest.instance.generate(context);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return 1;
                })));

        CommandRegistrationCallback.EVENT.register((dispatcher,
                                                    registryAccess,
                                                    environment) ->
                dispatcher.register(literal("test.generator.city.middle._test").executes(context -> execute(context))));

        CommandRegistrationCallback.EVENT.register((dispatcher,
                                                    registryAccess,
                                                    environment) ->
                dispatcher.register(literal("test.generator.city.middle._test_2").executes(context -> execute2(context))));

    }

    public void register_small_city() {

    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        Timer.start("test for block");

        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos start = BlockPos.ofFloored(source.getPosition());

        int width = 17 * 30;  // 510
        int depth = 17 * 30;  // 510
        int height = 2;

        // 尽量避免触发更新（如果Fabric/Forge版本允许）
        for (int y = 0; y < height; y++) {
            int yy = start.getY() + y;
            for (int x = 0; x < width; x++) {
                int xx = start.getX() + x;
                for (int z = 0; z < depth; z++) {
                    int zz = start.getZ() + z;
                    world.setBlockState(new BlockPos(xx, yy, zz), Blocks.STONE.getDefaultState(), 2);
                }
            }
        }

        source.sendFeedback(() -> net.minecraft.text.Text.literal(
                "已生成 " + width + "x" + depth + "x" + height + " 的石头立方体。"
        ), true);

        return 1;
    }

    public static int execute2(CommandContext<ServerCommandSource> context) {

        ServerWorld world = context.getSource().getWorld();

        int start_x = (int) context.getSource().getPosition().getX();
        int start_z = (int) context.getSource().getPosition().getZ();
        int y = (int) context.getSource().getPosition().getY();

        for (int x = 0; x < 30; x++) {
            for (int z = 0; z < 30; z++) {
                BlockPos blockPos = new BlockPos(start_x+x*17,y,start_z+z*17);
                StructureGenerator.GenerateNbt(
                        world,
                        "worldgenpro:develop/structures/test/city/middle/empty.nbt",
                        blockPos,
                        StructurePool.Projection.RIGID,
                        20,
                        80,
                        true,
                        false,
                        null);
            }
        }

        return 1;
    }

}
