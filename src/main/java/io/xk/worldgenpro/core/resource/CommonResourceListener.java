package io.xk.worldgenpro.core.resource;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.core.resource.api.ResourceLoader;
import io.xk.worldgenpro.core.resource.structures.NbtFileLoader;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resource.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xk.worldgenpro.WorldGenPro.structuresNbtMap;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class CommonResourceListener {

    public static final Logger LOGGER = LoggerFactory.getLogger("Resource");

    public static ObjectArrayList<ResourceLoader> loaders = new ObjectArrayList<>(BASE_BIG_CAPACITY);

    static {
        // 注册nbt文件加载器
        registerLoader(new NbtFileLoader());
    }

    /**
     * 注册加载器
     * @param loader 加载器实例
     */
    public static void registerLoader(ResourceLoader loader) {
        loaders.add(loader);
    }

    public void reload(ResourceManager manager) {
        // 清空结构表
        structuresNbtMap.clear();

        // 遍历调用所有加载器
        for (ResourceLoader loader : loaders) {
            java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> resourcs = manager.findResources(loader.getPath(), loader.getAllowedPathPredicate());
            loader.load(resourcs);
        }

        // 加载所有其他配置
        WorldGenPro.configureManager.loadResources(manager);
    }
}
