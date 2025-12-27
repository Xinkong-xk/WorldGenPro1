package io.xk.worldgenpro.core.configure.plugin.java;


import io.xk.worldgenpro.core.configure.plugin.PluginContext;

import java.lang.reflect.Method;

/**
 * 封装已编译插件类
 */
public class JavaPlugin {
    public final String identifier;
    public final Class<?> clazz;

    public JavaPlugin(String identifier, Class<?> clazz) {
        this.identifier = identifier;
        this.clazz = clazz;
    }

    public void run(PluginContext pluginContext){
        // 找到 run(PluginContext) 方法
        try {
            Method m = clazz.getMethod("run", PluginContext.class);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            m.invoke(instance, pluginContext);
        }catch (Exception e){
            System.err.println("Error while running plugin: " + identifier);
            e.printStackTrace();
        }
    }
}
