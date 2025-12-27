package io.xk.worldgenpro.core.configure.plugin.lua;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.Map;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_SMALL_CAPACITY;

public class LuaPluginLoader {
    public static final Logger LOGGER = LoggerFactory.getLogger("Lua Plugin");

    public static class LuaFile {
        public final String identifier;
        public String source;
        public String filePath;
        public String fileName;

        public LuaFile(String identifier, String source) {
            this.identifier = identifier;
            this.source = source;
            this.filePath = getVirtualFilePath();
            this.fileName = new File(this.filePath).getName();
        }

        public String getVirtualFilePath() {
            int colonIndex = this.identifier.indexOf(':');
            String path = colonIndex >= 0 ? identifier.substring(colonIndex + 1) : identifier;
            if (path.startsWith("develop/")) {
                path = path.substring("develop/".length());
            }
            return path;
        }

        @Override
        public String toString() {
            return "JavaScriptFile{" +
                    "identifier='" + identifier + '\'' +
                    ", source='" + source + '\'' +
                    ", filePath='" + filePath + '\'' +
                    ", fileName='" + fileName + '\'' +
                    '}';
        }
    }

    private final Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, ObjectArrayList<LuaFile>>> jsFiles = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);


    public void addLuaFile(BufferedReader reader, String identifier) {
        try {
            String content = String.join("\n", reader.lines().toList());
            addLuaFile(identifier, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Lua source from Reader for: " + identifier, e);
        } finally {
            try { reader.close(); } catch (Exception ignored) {}
        }

    }

    public void addLuaFile(String identifier, String source) {
        String namespace = getNamespaceFromIdentifier(identifier);
        String virtualPackage = computeVirtualPackage(identifier, namespace);

        // debug
        LOGGER.info("Adding Lua file: {} to {}", identifier, virtualPackage);

        jsFiles.computeIfAbsent(namespace, k -> new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY))
                .computeIfAbsent(virtualPackage, k -> new ObjectArrayList<>(BASE_SMALL_CAPACITY))
                .add(new LuaFile(identifier, source));
    }

    private String getNamespaceFromIdentifier(String identifier) {
        int colonIndex = identifier.indexOf(':');
        return colonIndex >= 0 ? identifier.substring(0, colonIndex) : "default";
    }

    private String computeVirtualPackage(String identifier, String namespace) {
        int colonIndex = identifier.indexOf(':');
        String path = colonIndex >= 0 ? identifier.substring(colonIndex + 1) : identifier;

        int lastSlash = path.lastIndexOf('/');
        String packagePath = lastSlash > 0 ? path.substring(0, lastSlash) : "plugin";
        if (packagePath.startsWith("develop/")) packagePath = packagePath.substring("develop/".length());
        packagePath = packagePath.replace('/', '.');

        return namespace + "." + packagePath;
    }

    public ObjectArrayList<LuaPlugin> build() {
        ObjectArrayList<LuaPlugin> plugins = new ObjectArrayList<>();

        for (Map.Entry<String, Object2ObjectOpenHashMap<String, ObjectArrayList<LuaFile>>> nsEntry : jsFiles.entrySet()) {
            // 命名空间
            String namespace = nsEntry.getKey();
            // 所有Lua文件
            ObjectArrayList<LuaFile> allLuaFile = new ObjectArrayList<>();

            Object2ObjectOpenHashMap<String, ObjectArrayList<LuaFile>> packageMap = nsEntry.getValue();

            for (Map.Entry<String, ObjectArrayList<LuaFile>> pkgEntry : packageMap.entrySet()) {
                ObjectArrayList<LuaFile> files = pkgEntry.getValue();

                for (LuaFile file : files) {
                    allLuaFile.add(file);
                }
            }

            // 获取上下文
            LuaContext luaContext = LuaContext.getNewContext(allLuaFile);

            for (LuaFile file : allLuaFile) {
                try {
                    if (!luaContext.checkPluginEntry(file)) {
                        continue; // 不是插件入口，跳过
                    }

                    // 1. 取源码
                    String code = file.source;
                    if (code == null) {
                        throw new RuntimeException("Lua file not found: " + file.filePath);
                    }

                    LuaValue chunk = luaContext.globals.load(new StringReader(code), file.filePath.replace(".lua",""));
                    chunk.call(); // 执行脚本，但不关心返回值

                    // 直接从 globals 里查找 run
                    LuaValue runFunc = luaContext.globals.get("run");
                    if (runFunc.isnil() || !runFunc.isfunction()) {
                        throw new RuntimeException("Lua Plugin missing run(context) function: " + file.fileName);
                    }

                    // 添加Lua插件
                    plugins.add(new LuaPlugin(file.fileName, runFunc));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        return plugins;
    }
}
