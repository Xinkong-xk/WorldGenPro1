package io.xk.worldgenpro.core.configure.plugin.java;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.util.ResourceFinderClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.CompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_SMALL_CAPACITY;

public class JavaPluginLoader {

    public static final Logger LOGGER = LoggerFactory.getLogger("Java Plugin");

    public class JavaFile {
        public final String identifier;
        public String source;
        public final String filePath;
        public String virtualPackage;

        public JavaFile(String identifier, String source) {
            this.identifier = identifier;
            this.source = source;
            this.filePath = getVirtualFilePath();
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
            return "JavaFile{" +
                    "identifier='" + identifier + '\'' +
                    ", source='" + source + '\'' +
                    ", filePath='" + filePath + '\'' +
                    ", virtualPackage='" + virtualPackage + '\'' +
                    '}';
        }
    }

    // key: namespace, value: map<虚拟包名, 文件列表>
    private final Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, ObjectArrayList<JavaFile>>> javaFiles = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);

    public void addJavaFile(BufferedReader reader, String identifier) {
        try {
            String content = String.join("\n", reader.lines().toList());
            addJavaFile(identifier, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Java source from Reader for: " + identifier, e);
        } finally {
            try { reader.close(); } catch (Exception ignored) {}
        }
    }

    public void addJavaFile(String identifier, String source) {
        String namespace = getNamespaceFromIdentifier(identifier);
        String virtualPackage = computeVirtualPackage(identifier, namespace);

        // debug
        LOGGER.info("Adding Java file: {} to {}", identifier, virtualPackage);

        javaFiles
                .computeIfAbsent(namespace, k -> new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY))
                .computeIfAbsent(virtualPackage, k -> new ObjectArrayList<>(BASE_SMALL_CAPACITY))
                .add(new JavaFile(identifier, source));
    }

    /**
     * 计算虚拟包，自动去掉 develop 前缀，并把 namespace 拼到最前面
     * 示例:
     * identifier = test:develop/plugin/test.java
     * namespace = test
     * 返回 test.plugin
     */
    private String computeVirtualPackage(String identifier, String namespace) {
        int colonIndex = identifier.indexOf(':');
        String path = colonIndex >= 0 ? identifier.substring(colonIndex + 1) : identifier;

        // 去掉文件名和 develop 前缀
        int lastSlash = path.lastIndexOf('/');
        String packagePath = lastSlash > 0 ? path.substring(0, lastSlash) : "plugin";
        if (packagePath.startsWith("develop/")) {
            packagePath = packagePath.substring("develop/".length());
        }

        // 转换斜杠为点
        packagePath = packagePath.replace('/', '.');

        // 拼接 namespace
        return namespace + "." + packagePath;
    }


    private String getNamespaceFromIdentifier(String identifier) {
        int colonIndex = identifier.indexOf(':');
        return colonIndex >= 0 ? identifier.substring(0, colonIndex) : "default";
    }


    public ObjectArrayList<JavaPlugin> build() {
        ObjectArrayList<JavaPlugin> plugins = new ObjectArrayList<>();

        for (Map.Entry<String, Object2ObjectOpenHashMap<String, ObjectArrayList<JavaFile>>> nsEntry : javaFiles.entrySet()) {
            // 命名空间
            String namespace = nsEntry.getKey();
            // java资源
            ObjectArrayList<Resource> javaResources = new ObjectArrayList<>();
            // 此命名空间下所有java文件
            ObjectArrayList<JavaFile> allJavaFile = new ObjectArrayList<>();

            Object2ObjectOpenHashMap<String, ObjectArrayList<JavaFile>> packageMap = nsEntry.getValue();
            for (Map.Entry<String, ObjectArrayList<JavaFile>> pkgEntry : packageMap.entrySet()) {
                String virtualPackage = pkgEntry.getKey();
                ObjectArrayList<JavaFile> files = pkgEntry.getValue();

                for (JavaFile file : files) {
                    javaResources.add(new StringResource(
                            file.filePath,
                            file.source
                    ));
                    file.virtualPackage = virtualPackage;

                    // 记录所有java文件
                    allJavaFile.add(file);
                }
            }

            // 加载完成此命名空间下所有java插件的资源
            ICompiler compiler = new CompilerFactory().newCompiler();

            // Store generated .class files in a Map:
            Map<String, byte[]> classes = new HashMap<>();
            compiler.setClassFileCreator(new MapResourceCreator(classes));

            JavaFile currentJavaFile = null;
            try {
                // Now compile two units from strings:
                compiler.compile(javaResources.toArray(new Resource[]{}));

                // Set up a class loader that uses the generated classes.
                ClassLoader cl = new ResourceFinderClassLoader(
                        new MapResourceFinder(classes),    // resourceFinder
                        this.getClass().getClassLoader() // parent
                );

                for (JavaFile file : allJavaFile) {
                    currentJavaFile = file;

                    String className = getFullClassName(file.filePath) ;
                    Class<?> clazz = cl.loadClass(className);
                    if (clazz.isAnnotationPresent(plugin.class)) {

                        plugins.add(new JavaPlugin(file.identifier, clazz));
                    }
                }
            }catch (ClassNotFoundException e) {
                LOGGER.error("Error while [building] Java plugin: {}", currentJavaFile.identifier);
                e.printStackTrace();
            }catch (org.codehaus.commons.compiler.CompileException e){
                LOGGER.error("Error while [compiling] Java plugin: {}", namespace);
                e.printStackTrace();
            }catch (IOException e){
                LOGGER.error("Error while [compiling] Java plugin: {}", namespace);
                e.printStackTrace();
            }

        }

        return plugins;
    }

    private String getFullClassName(String filePath) {
        return filePath.replace(".java","").replace("/",".");
    }
}