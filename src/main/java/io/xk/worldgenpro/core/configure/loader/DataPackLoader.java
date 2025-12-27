package io.xk.worldgenpro.core.configure.loader;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.xk.worldgenpro.core.configure.ConfigureManager;
import io.xk.worldgenpro.core.configure.api.EventManager;
import io.xk.worldgenpro.core.configure.loader.event.EventEntry;
import io.xk.worldgenpro.core.configure.loader.event.EventLoader;
import io.xk.worldgenpro.core.configure.loader.common.CommonEntry;
import io.xk.worldgenpro.core.configure.loader.common.CommonLoader;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resource.ResourceManager;

import java.io.BufferedReader;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

/**
 * 数据包加载器
 * 负责加载 RegisterEntry 和 EventEntry（原 ScriptEntry）
 */
public class DataPackLoader {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 注册项：文件路径标识 + 注册条目
    public final Object2ObjectOpenHashMap<String, CommonEntry> registerEntries = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);

    // 事件项：文件路径标识 + 事件条目
    public final Object2ObjectOpenHashMap<String, EventEntry> eventEntries = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);

    public EventManager eventManager = new EventManager();

    /**
     * 加载注册项 JSON 文件
     */
    public void loadCommon(BufferedReader reader, String identifier, ResourceManager resourceManager, ConfigureManager configureManager) {
        var entries = CommonLoader.load(gson, reader, identifier);

        for (CommonEntry entry : entries) {
            // key: 文件路径 + 条目名，保证唯一
            registerEntries.put(identifier + "/" + entry.name, entry);

            // 处理
            eventManager.processCommonEntry(entry,resourceManager,configureManager);
        }
    }

    /**
     * 加载事件项 JSON 文件
     */
    public void loadEvent(BufferedReader reader, String identifier, ResourceManager resourceManager, ConfigureManager configureManager) {
        var entries = EventLoader.load(gson, reader, identifier);

        for (EventEntry entry : entries) {
            // key: 文件路径 + 条目名，保证唯一
            eventEntries.put(identifier + "/" + entry.name, entry);

            // 处理
            eventManager.processEventEntry(entry,resourceManager,configureManager);
        }
    }

}
