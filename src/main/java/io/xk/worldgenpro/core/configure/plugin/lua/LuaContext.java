package io.xk.worldgenpro.core.configure.plugin.lua;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaContext {
    // 源代码
    private Map<String, String> sources = new HashMap<>();

    Globals globals;
    // require 缓存表
    Map<String, LuaTable> loaded = new HashMap<>();

    public LuaContext(ObjectArrayList<LuaPluginLoader.LuaFile> allLuaFile) {

        for (LuaPluginLoader.LuaFile file : allLuaFile) {
            sources.put(file.filePath.replace(".lua",""),file.source);
        }

        globals = JsePlatform.standardGlobals();

        // 自定义 require
        globals.set("require", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String name = arg.checkjstring();
                if (loaded.containsKey(name)) {
                    return loaded.get(name); // 返回占位表
                }
                LuaTable moduleTable = new LuaTable();
                loaded.put(name, moduleTable); // 占位表提前注册
                String code = sources.get(name);
                if (code == null) throw new LuaError("module not found: " + name);

                LuaValue chunk = globals.load(new StringReader(code), name);
                LuaValue result = chunk.call();

                // 如果模块返回 table，把内容拷贝到占位表里
                if (result.istable()) {
                    LuaTable t = (LuaTable) result;
                    LuaValue[] keys = t.keys();
                    for (int i = 0; i < keys.length; i++) {
                        LuaValue k = keys[i];
                        moduleTable.set(k, t.get(k));
                    }
                }
                return moduleTable;
            }
        });
    }

    // 匹配 @plugin 注释，带可选括号参数
    private static final Pattern PLUGIN_PATTERN = Pattern.compile("^\\s*--\\s*@plugin\\s*(\\((.*)\\))?", Pattern.MULTILINE);

    /**
     * 检查 Lua 文件是否是插件入口，并返回括号参数。
     *
     * @param file Lua 文件
     * @return 如果是插件入口返回参数 map，否则返回 null
     */
    public Map<String, String> parsePluginEntry(LuaPluginLoader.LuaFile file) {
        if (file == null || file.source == null) {
            return null;
        }

        Matcher matcher = PLUGIN_PATTERN.matcher(file.source);
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

    public boolean checkPluginEntry(LuaPluginLoader.LuaFile file) {
        if (file == null || file.source == null) return false;
        var result = parsePluginEntry(file);
        if (result != null)
            return true; // 没有匹配到或者文件为null


        return false;
    }


    public static LuaContext getNewContext(ObjectArrayList<LuaPluginLoader.LuaFile> allLuaFile) {
        return new LuaContext(allLuaFile);
    }
}
