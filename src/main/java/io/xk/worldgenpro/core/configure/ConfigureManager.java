package io.xk.worldgenpro.core.configure;


import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.builtin.datapack.BuiltinPlugins;
import io.xk.worldgenpro.core.configure.loader.DataPackLoader;
import io.xk.worldgenpro.core.configure.plugin.PluginContext;
import io.xk.worldgenpro.core.configure.plugin.java.JavaPlugin;
import io.xk.worldgenpro.core.configure.plugin.java.JavaPluginLoader;
import io.xk.worldgenpro.core.configure.plugin.javascript.JavaScriptPlugin;
import io.xk.worldgenpro.core.configure.plugin.javascript.JavaScriptPluginLoader;
import io.xk.worldgenpro.core.configure.plugin.lua.LuaPlugin;
import io.xk.worldgenpro.core.configure.plugin.lua.LuaPluginLoader;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

public class ConfigureManager {

    public static final Logger LOGGER = LoggerFactory.getLogger(WorldGenPro.MOD_ID);

    public DataPackLoader registerLoader = new DataPackLoader();

    public JavaPluginLoader javaPluginLoader = new JavaPluginLoader();

    public JavaScriptPluginLoader jsPluginLoader = new JavaScriptPluginLoader();

    public LuaPluginLoader luaPluginLoader = new LuaPluginLoader();

    public ConfigureManager() {
        BuiltinPlugins.register(this);
    }

    public void loadResources(ResourceManager manager) {
        // 加载注册项
        loadRegister(manager);
        // 加载所有插件
        loadPlugins(manager);
        // 测试运行插件
        executePlugins(manager);
    }

    public void loadRegister(ResourceManager manager){

        // 加载 通用注册项 json文件
        LOGGER.info("[Configure] Loading Common Register File...");
        java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> CommonRegisterMap = manager.findResources("develop/register/common", path -> path.toString().endsWith(".json"));
        for(Identifier id : CommonRegisterMap.keySet()) {
            try(BufferedReader reader = CommonRegisterMap.get(id).getReader()) {
                registerLoader.loadCommon(reader, id.toString(), manager, this);
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred while loading resource json " + id.toString()+ e.toString());
            }
        }
        LOGGER.info("[Configure] Load {} Common Register Files", CommonRegisterMap.size());

        // 加载 事件注册项 json文件
        LOGGER.info("[Configure] Loading Event Register Files...");
        java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> EventRegisterMap = manager.findResources("develop/register/event", path -> path.toString().endsWith(".json"));
        for(Identifier id : EventRegisterMap.keySet()) {
            try(BufferedReader reader = EventRegisterMap.get(id).getReader()) {
                registerLoader.loadEvent(reader, id.toString(), manager, this);
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred while loading resource json " + id.toString()+ e.toString());
            }
        }
        LOGGER.info("[Configure] Load {} Event Register Files", EventRegisterMap.size());
    }

    public void loadPlugins(ResourceManager manager) {
        LOGGER.info("[Configure] Load Plugin (Java,JavaScript,Lua) Files");

        // 加载 Java 插件
        LOGGER.info("[Configure] Loading <Java> Plugin...");
        java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> JavaPluginFiles = manager.findResources("develop/plugin", path -> path.toString().endsWith(".java"));
        for(Identifier id : JavaPluginFiles.keySet()) {
            try(BufferedReader reader = JavaPluginFiles.get(id).getReader()) {
                // 添加Java插件文件
                javaPluginLoader.addJavaFile(reader,id.toString());
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred while loading Java File " + id.toString()+ e.toString());
            }
        }
        LOGGER.info("[Configure] Load {} Java Plugins", JavaPluginFiles.size());

        // 加载 JavaScript 插件
        LOGGER.info("[Configure] Loading <JavaScript> Plugin...");
        // 加载mjs文件
        java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> JavaScriptPluginFiles = manager.findResources("develop/plugin", path -> path.toString().endsWith(".mjs"));
        for(Identifier id : JavaScriptPluginFiles.keySet()) {
            try(BufferedReader reader = JavaScriptPluginFiles.get(id).getReader()) {
                // 添加Java插件文件
                jsPluginLoader.addJavaScriptFile(reader,id.toString());
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred while loading JavaScript File " + id.toString()+ e.toString());
            }
        }
        // 加载普通js文件,但是当做是mjs模块化文件
        java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> JavaScriptPluginFiles_2 = manager.findResources("develop/plugin", path -> path.toString().endsWith(".js"));
        for(Identifier id : JavaScriptPluginFiles_2.keySet()) {
            try(BufferedReader reader = JavaScriptPluginFiles_2.get(id).getReader()) {
                // 添加Java插件文件
                jsPluginLoader.addJavaScriptFile(reader,id.toString());
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred while loading JavaScript File " + id.toString()+ e.toString());
            }
        }
        LOGGER.info("[Configure] Load {} JavaScript Plugins", JavaScriptPluginFiles.size()+JavaScriptPluginFiles_2.size());

        // 加载 Lua 插件
        LOGGER.info("[Configure] Loading <Lua> Plugin...");
        java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> LuaPluginFiles = manager.findResources("develop/plugin", path -> path.toString().endsWith(".lua"));
        for(Identifier id : LuaPluginFiles.keySet()) {
            try(BufferedReader reader = LuaPluginFiles.get(id).getReader()) {
                // 添加Java插件文件
                luaPluginLoader.addLuaFile(reader,id.toString());
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred while loading Lua File " + id.toString()+ e.toString());
            }
        }
        LOGGER.info("[Configure] Load {} Lua Plugins", LuaPluginFiles.size());
    }

    public void executePlugins(ResourceManager manager) {
        LOGGER.info("[Configure] Execute Plugin Files...");

        // 创建插件上下文
        PluginContext context = new PluginContext();

        // 编译并执行 Java 插件
        ObjectArrayList<JavaPlugin> javaPlugins = javaPluginLoader.build();
        LOGGER.info("[Configure] Execute Java Plugins:");
        for (JavaPlugin plugin : javaPlugins) {
            System.out.println(" - " + plugin.identifier + " -> " + plugin.clazz.getName());
            plugin.run(context);
        }
        LOGGER.info("[Configure] Execute {} Java Plugins Successfully!", javaPlugins.size());

        // 解析并执行 JS 插件
        ObjectArrayList<JavaScriptPlugin> jsPlugins = jsPluginLoader.build();
        LOGGER.info("[Configure] Execute JavaScript Plugins:");
        for (JavaScriptPlugin plugin : jsPlugins) {
            System.out.println(" - " + plugin.identifier);
            plugin.run(context);
        }
        LOGGER.info("[Configure] Execute {} JavaScript Plugins Successfully!",jsPlugins.size());

        // 解析并执行 Lua 插件
        ObjectArrayList<LuaPlugin> luaPlugins = luaPluginLoader.build();
        LOGGER.info("[Configure] Execute Lua Plugins:");
        for (LuaPlugin plugin : luaPlugins) {
            System.out.println(" - " + plugin.identifier);
            plugin.run(context);
        }
        LOGGER.info("[Configure] Execute {} Lua Plugins Successfully!", luaPlugins.size());
    }
}
