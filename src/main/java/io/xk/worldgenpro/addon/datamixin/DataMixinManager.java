package io.xk.worldgenpro.addon.datamixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.datamixin.api.DataMixinOp;
import io.xk.worldgenpro.addon.datamixin.config.DataMixinElement;
import io.xk.worldgenpro.addon.datamixin.config.DataMixinFile;
import io.xk.worldgenpro.addon.datamixin.mixintype.MixinTypeManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_MIDDLE_CAPACITY;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_SMALL_CAPACITY;

public class DataMixinManager {

    public static Logger LOGGER = LoggerFactory.getLogger("DataMixin");

    public ConcurrentMap<String, DataMixinFile> mixinFiles = new ConcurrentHashMap<>(BASE_SMALL_CAPACITY);

    public void addMixinFile(String fileName, DataMixinFile mixinFile) {
        mixinFiles.putIfAbsent(fileName, mixinFile);
    }

    public ConcurrentMap<String, DataMixinFile> getMixinFiles() {
        return mixinFiles;
    }

    public void onServerStarting(MinecraftServer server) {
        // 加载所有DataMixin文件
        WorldGenPro.dataMixinLoader.reload(server.getResourceManager(),server);
        // 构建所有DataMixin
        buildAll(server);
    }

    public void reset(){
        mixinFiles.clear();
    }

    public void onServerStopping(MinecraftServer ignoredServer) {
        reset();
    }

    /**
     * 构建所有DataMixin
     * @param server Minecraft服务器对象
     */
    public void buildAll(MinecraftServer server) {
        LOGGER.info("Building Data Mixins...");

        // 记录所有元素
        Object2ObjectLinkedOpenHashMap<Integer,ObjectArrayList<DataMixinElement>> allElementMap = new Object2ObjectLinkedOpenHashMap<>(BASE_MIDDLE_CAPACITY);

        // 1. 收集并解析所有 element
        for (Map.Entry<String, DataMixinFile> entry : mixinFiles.entrySet()) {
            String fileName = entry.getKey();
            DataMixinFile file = entry.getValue();
            // 设置文件名
            file.setFileName(fileName);

            // 遍历所有DataMixin文件里的元素
            for (int i = 0; i < file.elements().size(); i++) {
                // 原始json元素
                JsonElement raw = file.elements().get(i);
                // 如果不是一个json对象则报错
                if (!raw.isJsonObject()) {
                    LOGGER.error("Skipping non-object element in {} at index [{}]: {}", fileName, i,raw);
                    if (WorldGenPro.configFile.dataMixinStrictMode){
                        throw new RuntimeException("non-object element in "+fileName+" at index ["+i+"]: "+raw);
                    }
                    continue;
                }
                // 获取为json对象
                JsonObject obj = raw.getAsJsonObject();

                try {
                    // 解析op操作符字段
                    String opText = obj.has("op") ? obj.get("op").getAsString() : null;
                    int priority = obj.has("priority")
                            ? obj.get("priority").getAsInt()
                            : DataMixinElement.DEFAULT_PRIORITY;
                    // 如果没有操作符字段则报错
                    if (opText == null) {
                        LOGGER.error("Missing 'op' field in {} at index [{}]: {}", fileName, i, obj);
                        if (WorldGenPro.configFile.dataMixinStrictMode){
                            throw new RuntimeException("missing 'op' field in "+fileName+" at index ["+i+"]: "+obj);
                        }
                        continue;
                    }
                    // 创建DataMixinElement对象
                    DataMixinElement element = new DataMixinElement(opText, priority, obj);
                    element.setParentFile(file);
                    element.setElementIndex(i);
                    allElementMap.computeIfAbsent(priority, key -> new ObjectArrayList<>()).add(element);
                } catch (Exception e) {
                    LOGGER.error(
                            "Failed to parse DataMixinElement in {} at index [{}]: {}",
                            fileName, i, e
                    );
                    if (WorldGenPro.configFile.dataMixinStrictMode){
                        throw new RuntimeException("Failed to parse DataMixinElement in "+fileName+" at index ["+i+"]: "+e);
                    }
                }
            }
        }

        // 2. 按 priority 优先级排序(小 → 大)
        ObjectArrayList<Integer> sortedPriorities =
                new ObjectArrayList<>(allElementMap.keySet());
        sortedPriorities.sort(Integer::compare);

        // 3. 检验所有 priority 上的 DataMixin 项是否存在冲突和其他错误
        this.validatePriorityMixins(sortedPriorities,allElementMap);

        // 4. 依 priority 顺序执行 mixin 操作
        for (int priority : sortedPriorities) {
            ObjectArrayList<DataMixinElement> elements = allElementMap.get(priority);

            for (DataMixinElement element : elements) {
                DataMixinOp op = element.getOp();

                try {
                    // 开始进行mixin操作
                    MixinTypeManager.applyMixin(server,element,op,element.parentFile.mixinType);
                } catch (Exception e) {
                    LOGGER.error("Error executing DataMixinElement {}", element, e);
                    if (WorldGenPro.configFile.dataMixinStrictMode) {
                        throw new RuntimeException("Error executing DataMixinElement " + element, e);
                    }
                }
            }
        }

        // 总数
        int total = allElementMap.values().stream()
                .mapToInt(ObjectArrayList::size)
                .sum();

        LOGGER.info("Finished building Data Mixins. Total elements: {}", total);
    }

    public void validatePriorityMixins(ObjectArrayList<Integer> sortedPriorities,
                                       Object2ObjectLinkedOpenHashMap<Integer,ObjectArrayList<DataMixinElement>> allElementMap){
        for (int priority : sortedPriorities){
            ObjectArrayList<DataMixinElement> elements = allElementMap.get(priority);
            if (elements.size() > 1){
                // 如果在此优先级内存在大于1个mixin项,则需要进行检验
                Pair<Boolean,String> result = DataMixinValidate.validateMixins(elements);
                if (!result.getLeft()){
                    LOGGER.error("Error in validating DataMixinElement");
                    throw new RuntimeException("DataMixin conflict Error \n"+result.getRight());
                }
            }
        }
    }
}
