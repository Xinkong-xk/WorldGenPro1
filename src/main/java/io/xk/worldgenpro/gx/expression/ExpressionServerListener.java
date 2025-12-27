package io.xk.worldgenpro.gx.expression;


import io.xk.worldgenpro.WorldGenPro;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

public class ExpressionServerListener {
    public static void registerListeners()
    {
        ServerLifecycleEvents.SERVER_STARTING.register(new Identifier("worldgenpro:script_engine_initialize"),
                server -> {
                    // 在服务器初始化时,检查所有脚本引擎是否需要初始化和启动
                    WorldGenPro.expressionManager.initialize();
                });
    }
}
