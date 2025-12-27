package io.xk.worldgenpro.addon.datamixin.config;

import io.xk.worldgenpro.WorldGenPro;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DataMixinLoader {
    public static org.slf4j.Logger LOGGER = LoggerFactory.getLogger("DataMixinLoader");

    public void reload(ResourceManager manager, MinecraftServer server) {
        LOGGER.info("Loading Data Mixin Files...");
        // 加载来自以下两个文件夹下的DataMixin文件
        loadFrom("worldgen/data_mixin",manager,server);
        loadFrom("develop/data_mixin",manager,server);
        LOGGER.info("Finished loading {} Data Mixin files", WorldGenPro.dataMixinManager.getMixinFiles().size());
    }

    public void loadFrom(String folder,ResourceManager manager,MinecraftServer server) {
        Map<Identifier, Resource> resources = manager.findResources(folder, path -> path.getPath().endsWith(".json"));

        for (Identifier id : resources.keySet()) {
            try {
                Resource resource = resources.get(id);
                String key = id.toString();
                DataMixinFile dataMixinFile = DataMixinFile.read(resource.getReader());
                WorldGenPro.dataMixinManager.addMixinFile(key, dataMixinFile);
                LOGGER.info("Loaded Data Mixin : {}", key);
            } catch (Exception e) {
                LOGGER.error("Exception loading Data Mixin {}: {}", id, e.toString());
            }
        }
    }
}
