package io.xk.worldgenpro.gx.script.registry;

import java.util.concurrent.ConcurrentHashMap;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class GxMetaRegistry {
    private static final ConcurrentHashMap<String, String> META_MAP = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    /** 注册元信息指令，比如 register("@java", "#lang java") */
    public static void register(String metaSymbol, String translatedDsl) {
        META_MAP.put(metaSymbol.toLowerCase(), translatedDsl);
    }

    public static String translate(String metaLine) {
        for (var entry : META_MAP.entrySet()) {
            if (metaLine.trim().toLowerCase().startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return metaLine; // 非元信息，原样返回
    }

    public static void registerDefaults() {
        register("@java", "#lang java");
        register("#!java", "#lang java");

        register("@js", "#lang js");
        register("#!js", "#lang js");

        register("@javascript", "#lang javascript");
        register("#!javascript", "#lang javascript");

        register("@lua", "#lang lua");
        register("#!lua", "#lang lua");
    }
}
