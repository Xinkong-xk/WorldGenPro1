package io.xk.worldgenpro.core.configure.plugin.lua;


import io.xk.worldgenpro.core.configure.plugin.PluginContext;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class LuaPlugin {
    public final String identifier;
    public final LuaValue runFunc; // luaj 的 LuaValue，可能是函数

    public LuaPlugin(String identifier, LuaValue runFunc) {
        this.identifier = identifier;
        this.runFunc = runFunc;
        if (runFunc == null || !runFunc.isfunction()) {
            throw new IllegalArgumentException("Lua plugin '" + identifier + "' does not provide a valid run function!");
        }
    }

    public void run(PluginContext pluginContext) {
        try {
            // 把 Java 对象转为 LuaValue
            LuaValue luaContextArg = CoerceJavaToLua.coerce(pluginContext);
            runFunc.call(luaContextArg); // 调用 Lua 里的 run(context)
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute <Lua> plugin: " + identifier, e);
        }
    }
}