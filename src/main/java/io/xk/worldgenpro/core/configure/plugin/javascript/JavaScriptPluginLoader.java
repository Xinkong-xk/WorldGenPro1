package io.xk.worldgenpro.core.configure.plugin.javascript;

import org.graalvm.polyglot.*;
import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_SMALL_CAPACITY;

public class JavaScriptPluginLoader {

    public static final Logger LOGGER = LoggerFactory.getLogger("JS Plugin");

    public static class JavaScriptFile {
        public final String identifier;
        public String source;
        public String filePath;
        public String fileName;

        public JavaScriptFile(String identifier, String source) {
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

    private final Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, ObjectArrayList<JavaScriptFile>>> jsFiles = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);


    public void addJavaScriptFile(BufferedReader reader, String identifier) {
        try {
            String content = String.join("\n", reader.lines().toList());
            addJavaScriptFile(identifier, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JS source from Reader for: " + identifier, e);
        } finally {
            try { reader.close(); } catch (Exception ignored) {}
        }

    }

    public void addJavaScriptFile(String identifier, String source) {
        String namespace = getNamespaceFromIdentifier(identifier);
        String virtualPackage = computeVirtualPackage(identifier, namespace);

        // debug
        LOGGER.info("Adding JavaScript file: {} to {}", identifier, virtualPackage);

        jsFiles
                .computeIfAbsent(namespace, k -> new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY))
                .computeIfAbsent(virtualPackage, k -> new ObjectArrayList<>(BASE_SMALL_CAPACITY))
                .add(new JavaScriptFile(identifier, source));
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

    public ObjectArrayList<JavaScriptPlugin> build() {
        ObjectArrayList<JavaScriptPlugin> plugins = new ObjectArrayList<>();

        for (Map.Entry<String, Object2ObjectOpenHashMap<String, ObjectArrayList<JavaScriptFile>>> nsEntry : jsFiles.entrySet()) {
            // 命名空间
            String namespace = nsEntry.getKey();
            // 所有JavaScript文件
            ObjectArrayList<JavaScriptFile> allJSFile = new ObjectArrayList<>();

            Object2ObjectOpenHashMap<String, ObjectArrayList<JavaScriptFile>> packageMap = nsEntry.getValue();

            for (Map.Entry<String, ObjectArrayList<JavaScriptFile>> pkgEntry : packageMap.entrySet()) {
                ObjectArrayList<JavaScriptFile> files = pkgEntry.getValue();

                for (JavaScriptFile file : files) {
                    allJSFile.add(file);
                }
            }

            // 获取上下文
            Context cx = JavaScriptContextUtil.getNewContext(allJSFile,namespace);

            for (JavaScriptFile file : allJSFile) {
                try{
                    Map<String,String> pluginArgs = parsePluginEntry(file);
                    if (pluginArgs == null) {
                        continue; // 没有标记则跳过
                    } else {
                        // 删除顶层 "@Plugin" 或 '@Plugin' 标记
                        String newSource = file.source
                                .replaceFirst("\"@Plugin\"\\s*;?", "")
                                .replaceFirst("'@Plugin'\\s*;?", "");
                        file.source = newSource;
                    }

                    // 匹配顶层 export function run(...) { ... }
                    Matcher matcher = RUN_FUNCTION_PATTERN.matcher(file.source);
                    if (matcher.find()) {
                        // 自动拼接一条 globalThis.run = run;
                        file.source += "\nglobalThis.run = run;";
                    }

                    cx.eval(Source.newBuilder("js", file.source, file.fileName)
                            .mimeType("application/javascript+module")
                            .build());

                    Value runFunc = cx.getBindings("js").getMember("run");

                    if (runFunc == null || !runFunc.canExecute()) {
                        throw new RuntimeException("JavaScript Plugin missing run(context) function: " + file.fileName);
                    }
                    // 添加JS插件
                    plugins.add(new JavaScriptPlugin( file.fileName, runFunc));

                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }

        return plugins;
    }

    private static final Pattern RUN_FUNCTION_PATTERN = Pattern.compile(
            "^\\s*export\\s+function\\s+run\\s*\\(\\s*[a-zA-Z_$][a-zA-Z0-9_$]*\\s*\\)\\s*\\{",
            Pattern.MULTILINE
    );


    // 匹配 "@plugin" 指令，带可选括号参数
    // 解释：
    // ^\\s*              → 行首允许空白
    // "                 → 匹配双引号
    // \\s*@plugin\\s*    → 匹配 @plugin 和可选空白
    // (\\((.*)\\))?      → 可选的括号参数，第二组捕获括号内内容
    // "                 → 匹配闭合的双引号
    private static final Pattern PLUGIN_PATTERN_JS =
            Pattern.compile("^\\s*\"\\s*@plugin\\s*(\\((.*)\\))?\"\\s*;\\s*$", Pattern.MULTILINE);


    /**
     * 检查 JS 文件是否是插件入口，并返回括号参数。
     *
     * @param file JS 文件
     * @return 如果是插件入口返回参数 map，否则返回 null
     */
    public Map<String, String> parsePluginEntry(JavaScriptFile file) {
        if (file == null || file.source == null) {
            return null;
        }

        Matcher matcher = PLUGIN_PATTERN_JS.matcher(file.source);
        if (!matcher.find()) {
            return null; // 没有 @plugin 标记
        }

        Map<String, String> paramMap = new HashMap<>();

        String params = matcher.group(2); // 括号内内容
        if (params == null || params.isEmpty()) {
            return paramMap; // 没有参数，返回空 map
        }

        // 拆分 key=value，允许任意空白符
        String[] pairs = params.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                paramMap.put(key, value);
            }
        }

        return paramMap;
    }

}
