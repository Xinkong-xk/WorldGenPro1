package io.xk.worldgenpro.core.listener;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.core.configure.ConfigureManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.xk.worldgenpro.WorldGenPro.*;

// 基础配置加载监听器
public class BaseConfigureListeners {
    public static final Logger LOGGER = LoggerFactory.getLogger("Main Listener");

    public static void registerListeners() {

        // 新的存档加载完成后后
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[Server] : Server Started...");

            StructureTemplateManager structureTemplateManager = minecraftServer.getStructureTemplateManager();
            for (Identifier id : structuresNbtMap.keySet()){
                StructureTemplate structureTemplate = structureTemplateManager.createTemplate(structuresNbtMap.get(id));
                structureTemplateManager.templates.put(Identifier.of(id.getNamespace(),id.getPath()), Optional.of(structureTemplate));
            }
            structuresNbtMap.clear();
        });

        // 新的存档加载时
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("[Server] : Server Starting...");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[Server] : Server Stopping...");
            // 创建一个新的基础配置管理器,方便下一次使用
            WorldGenPro.configureManager = new ConfigureManager();
        });

    }
}
