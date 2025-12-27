package io.xk.worldgenpro.addon.roadgen.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.codec.config.RoadEntry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RoadConfigLoader {

    public void reload(ResourceManager manager, MinecraftServer server) {
        RoadGen.LOGGER.info("[RoadGen:Resource] Loading road connection configs...");
        // 加载来自以下两个文件夹下的两个目录里的RoadGen配置文件
        loadFrom("worldgen/roadgen",manager,server);
        loadFrom("worldgen/road",manager,server);
        loadFrom("develop/roadgen",manager,server);
        loadFrom("develop/road",manager,server);
        RoadGen.LOGGER.info("[RoadGen:Resource.successful] Finished loading {} road config files", WorldGenPro.roadConnectionConfigManager.getAll().size());
    }

    public void loadFrom(String folder,ResourceManager manager,MinecraftServer server) {
        Map<Identifier, Resource> resources = manager.findResources(folder, path -> path.getPath().endsWith(".json"));

        for (Identifier id : resources.keySet()) {
            try {
                Resource resource = resources.get(id);
                JsonElement json = JsonParser.parseReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

                // 顶级是数组: [ { from:... , ... }, ... ]
                Codec<RoadEntry> Codec = RoadEntry.CODEC.stable();

                var result = Codec.parse(JsonOps.INSTANCE, json);

                result.resultOrPartial(error -> {
                    RoadGen.LOGGER.error("[RoadGen:Resource] Failed parsing {}: {}", id, error);
                }).ifPresent(configRoadEntry -> {
                    // 使用 <namespace:path> 去掉前缀
                    String key = id.toString();

                    WorldGenPro.roadConnectionConfigManager.put(key, configRoadEntry,server);
                    RoadGen.LOGGER.info("[RoadGen:Resource] Loaded road config: {}", key);
                });

            } catch (Exception e) {
                RoadGen.LOGGER.error("[RoadGen:Resource] Exception loading road config {}: {}", id, e.toString());
            }
        }
    }
}
