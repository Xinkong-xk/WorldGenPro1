package io.xk.worldgenpro.configfile;

import com.electronwill.nightconfig.core.file.FileConfig;
import io.xk.worldgenpro.WorldGenPro;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigFile {

    // 文件配置对象
    private FileConfig config;

    // ========需要的配置字段========
    // 最大距离
    public int maxDistanceFromCenter;

    // DataMixin是否使用严格模式
    public boolean dataMixinStrictMode;

    /**
     * 为当前存档加载或创建配置文件
     */
    public static ConfigFile loadFor(MinecraftServer server) {
        WorldGenPro.LOGGER.info("Loading ConfigFile");
        Path path = server
                .getSavePath(WorldSavePath.ROOT)
                .resolve("worldgenpro_config.toml");

        FileConfig fc = FileConfig.builder(path)
                .autosave()
                .build();

        ConfigFile cfg = new ConfigFile();
        cfg.config = fc;

        // 加载
        fc.load();

        cfg.maxDistanceFromCenter =
                fc.getOrElse("pool.max_distance_from_center", 512);

        cfg.dataMixinStrictMode =
                fc.getOrElse("data_mixin.strict_mode", true);

        // 确保字段存在 / 修复老配置
        fc.set("pool.max_distance_from_center", cfg.maxDistanceFromCenter);
        fc.set("data_mixin.strict_mode", cfg.dataMixinStrictMode);

        fc.save();
        return cfg;
    }


    /**
     * 手动保存一次（可选）
     */
    public void save() {
        config.set("pool.maxDistanceFromCenter", maxDistanceFromCenter);
        config.set("data_mixin.strictMode", dataMixinStrictMode);
        config.save();
    }
}
