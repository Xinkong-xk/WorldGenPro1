package io.xk.worldgenpro.gx.expression.compilers;

import io.xk.worldgenpro.gx.expression.api.ExpressionCompiler;
import io.xk.worldgenpro.utils.ClassInitializer;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class LuaExpressionCompiler extends ExpressionCompiler {

    public static final Globals GLOBALS = JsePlatform.standardGlobals();

    @Override
    public <T> Function<List<Object>, T> compileExpression(List<String> scriptTexts,
                                                           LinkedHashMap<String, Class<?>> args,
                                                           Class<T> resultType) {
        try {
            List<String> paramNames = new ArrayList<>(args.keySet());
            StringBuilder lua = new StringBuilder();
            lua.append("return function(").append(String.join(",", paramNames)).append(")\n");

            if (scriptTexts.size() == 1) {
                String line = scriptTexts.getFirst().trim();
                if (!line.startsWith("return")) lua.append("  return ").append(line).append("\n");
                else lua.append("  ").append(line).append("\n");
            } else {
                // 多行不干预 return，由用户负责
                for (String line : scriptTexts) {
                    lua.append("  ").append(line).append("\n");
                }
            }

            lua.append("end");

            LuaValue chunk = GLOBALS.load(lua.toString(), "expr_chunk_" + UUID.randomUUID());
            LuaValue func = chunk.call();

            return argsList -> {
                try {
                    LuaValue[] luArgs = new LuaValue[paramNames.size()];
                    for (int i = 0; i < paramNames.size(); i++) {
                        luArgs[i] = CoerceJavaToLua.coerce(argsList.get(i));
                    }
                    LuaValue result = func.invoke(LuaValue.varargsOf(luArgs)).arg1();
                    if (result == null || result.isnil()) return null;
                    Object javaValue = CoerceLuaToJava.coerce(result, resultType);
                    return resultType.cast(javaValue);
                } catch (Exception e) {
                    throw new RuntimeException("Lua expression execution failed: " + scriptTexts, e);
                }
            };

        } catch (Exception e) {
            throw new RuntimeException("Failed to compile Lua expression: " + scriptTexts, e);
        }
    }

    @Override
    public Consumer<List<Object>> compileProcess(List<String> scriptTexts,
                                                 LinkedHashMap<String, Class<?>> args) {
        try {
            // 参数名保持顺序
            List<String> paramNames = new ArrayList<>(args.keySet());

            // 构建 Lua 函数字符串
            StringBuilder lua = new StringBuilder();
            lua.append("return function(")
                    .append(String.join(",", paramNames))
                    .append(")\n");

            for (String line : scriptTexts) {
                lua.append("  ").append(line).append("\n");
            }

            lua.append("end");

            // 编译
            LuaValue chunk = GLOBALS.load(lua.toString(), "proc_chunk_" + UUID.randomUUID());
            LuaValue func = chunk.call();

            return argsList -> {
                try {
                    LuaValue[] luArgs = new LuaValue[paramNames.size()];
                    for (int i = 0; i < paramNames.size(); i++) {
                        luArgs[i] = CoerceJavaToLua.coerce(argsList.get(i));
                    }

                    // 执行，不关心返回值
                    func.invoke(LuaValue.varargsOf(luArgs));
                } catch (Exception e) {
                    throw new RuntimeException("Lua process execution failed: " + scriptTexts, e);
                }
            };

        } catch (Exception e) {
            throw new RuntimeException("Failed to compile Lua process: " + scriptTexts, e);
        }
    }


    public boolean isInitialized = false;

    @Override
    public void initialize() {
        if (!this.isInitialized) {
            ClassInitializer.forceInitialize(this.getClass());
            this.isInitialized = true;
        }
    }

    @Override
    public boolean isInitialized() {
        return this.isInitialized;
    }
}
