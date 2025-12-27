package io.xk.worldgenpro.test.api;

public interface TestGenerator {
    void generate(com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> context);

    TestGenerator getInstance();
}
