package io.xk.worldgenpro.addon.semantic;

import io.xk.worldgenpro.WorldGenPro;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

public class GeoSemanticStructureServerListener {
    public static void  registerListeners() {
        ServerLifecycleEvents.SERVER_STOPPING.register(new Identifier("worldgenpro:super_large_structure_prediction_save"),
                server -> {
                    if (WorldGenPro.geoSemanticStructureManager != null) {
                        WorldGenPro.geoSemanticStructureManager.onServerStopping(server);
                    }
                }
        );

        // 服务器启动时加载超大型结构生成预测器
        ServerLifecycleEvents.SERVER_STARTING.register(new Identifier("worldgenpro:super_large_structure_prediction_load"),
                server -> {
                    if (WorldGenPro.geoSemanticStructureManager != null) {
                        WorldGenPro.geoSemanticStructureManager.onServerStarting(server);
                    }
                }
        );
    }
}
