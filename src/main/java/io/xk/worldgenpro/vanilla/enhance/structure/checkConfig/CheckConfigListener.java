package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig;

import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckDistanceConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.config.CheckSizeConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructurePoolMixinReflect;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Objects;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

public class CheckConfigListener {
    public static void registerListeners(){
        // 服务器启动时构建末尾配置
        ServerLifecycleEvents.SERVER_STARTING.register(new Identifier("worldgenpro:end_config_build"),
                server -> {
                    // 遍历注册表
                    LOGGER.info("[CheckConfig] Load Configs");
                    for(var pool : server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL)){
                        // 构建末尾配置项
                        CheckSizeConfig checkSizeConfig = Objects.requireNonNull(StructurePoolMixinReflect.getCheckSizeConfig(pool));
                        checkSizeConfig.parentPool = pool;
                        checkSizeConfig.buildConfig(server);

                        CheckDistanceConfig checkDistanceConfig = Objects.requireNonNull(StructurePoolMixinReflect.getCheckDistanceConfig(pool));
                        checkDistanceConfig.parentPool = pool;
                        checkDistanceConfig.buildConfig(server);
                    }
                }
        );

    }
}
