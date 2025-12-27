package io.xk.worldgenpro.gx.script;

import io.xk.worldgenpro.gx.expression.compilers.JSExpressionCompiler;
import io.xk.worldgenpro.gx.expression.compilers.LuaExpressionCompiler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.codehaus.janino.SimpleCompiler;
import org.luaj.vm2.LuaValue;
import org.graalvm.polyglot.*;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.xk.worldgenpro.gx.expression.compilers.JavaExpressionCompiler.defaultImports;

public class GxScript {
    public String name;
    public final GxLang lang;
    private ConcurrentHashMap<String, Method> javaMethods;
    private ConcurrentHashMap<String, LuaValue> luaFunctions;
    private ConcurrentHashMap<String, Value> jsFunctions;

    private Object javaInstance; // 用于保存 Java 编译后类的实例

    public GxScript(GxLang lang,String name) {
        this.lang = lang;
        this.name = name;
    }

    /**
     * Java脚本编译器：动态编译并导出所有public方法
     */
    public ConcurrentHashMap<String, Method> fromJava(String code, ObjectArrayList<String> exportedNames) {
        try {
            SimpleCompiler compiler = new SimpleCompiler();

            String className = "GxScript_" + UUID.randomUUID().toString().replace("-", "");

            StringBuilder c_code = new StringBuilder();
            c_code.append(String.join("", defaultImports));
            c_code.append("public class ").append(className)
                    .append("{\n");
            c_code.append(code);
            c_code.append("};");

            compiler.cook(c_code.toString());

            Class<?> clazz = compiler.getClassLoader().loadClass(className);

            javaInstance = clazz.getDeclaredConstructor().newInstance();

            ConcurrentHashMap<String, Method> methods = new ConcurrentHashMap<>();
            for (Method m : clazz.getDeclaredMethods()) {
                if (exportedNames == null || exportedNames.isEmpty() || exportedNames.contains(m.getName())) {
                    methods.put(m.getName(), m);
                }
            }

            return methods;
        } catch (Exception e) {
            throw new RuntimeException("Java script compilation failed", e);
        }
    }

    /**
     * Lua脚本导出
     */
    private void fromLua(String code, ObjectArrayList<String> exportedNames) {
        LuaExpressionCompiler.GLOBALS.load(code, "script").call();

        for (String name : exportedNames) {
            LuaValue func = LuaExpressionCompiler.GLOBALS.get(name);
            if (!func.isnil()) {
                luaFunctions.put(name, func);
            }
        }
    }

    /**
     * JavaScript脚本导出 (GraalVM JS)
     */
    private void fromJS(String code, ObjectArrayList<String> exportedNames) {
        JSExpressionCompiler.THREAD_CONTEXT.eval("js", code);
        Value bindings = JSExpressionCompiler.THREAD_CONTEXT.getBindings("js");

        for (String name : exportedNames) {
            Value func = bindings.getMember(name);
            if (func != null && func.canExecute()) {
                jsFunctions.put(name, func);
            }
        }
    }

    /**
     * 总编译方法，根据语言调用不同编译逻辑
     */
    public void compile(String code, ObjectArrayList<String> exportedNames) {
        switch (lang) {
            case JAVA -> {
                javaMethods = new ConcurrentHashMap<>(4);
                javaMethods.putAll(fromJava(code, exportedNames));
            }
            case LUA -> {
                luaFunctions = new ConcurrentHashMap<>(4);
                fromLua(code, exportedNames);
            }
            case JavaScript -> {
                jsFunctions = new ConcurrentHashMap<>(4);
                fromJS(code, exportedNames);
            }
            default -> throw new IllegalArgumentException("Unsupported language: " + lang);
        }
    }

    /**
     * 执行函数（根据语言自动分派）
     */
    public Object execute(String methodName, Object... args) {
        try {
            switch (lang) {
                case JAVA -> {
                    Method m = javaMethods.get(methodName);
                    if (m == null) throw new IllegalArgumentException("No such Java method: " + methodName);
                    if (javaInstance == null)
                        javaInstance = m.getDeclaringClass().getDeclaredConstructor().newInstance();
                    return m.invoke(javaInstance, args);
                }

                case LUA -> {
                    LuaValue func = luaFunctions.get(methodName);
                    if (func == null) throw new IllegalArgumentException("No such Lua function: " + methodName);
                    LuaValue[] luaArgs = new LuaValue[args.length];
                    for (int i = 0; i < args.length; i++) {
                        luaArgs[i] = LuaValue.valueOf(args[i].toString());
                    }
                    return func.invoke(luaArgs).arg1().tojstring();
                }

                case JavaScript -> {
                    Value func = jsFunctions.get(methodName);
                    if (func == null) throw new IllegalArgumentException("No such JS function: " + methodName);
                    Value result = func.execute(args);
                    return result.isNull() ? null : result.as(Object.class);
                }

                default -> throw new IllegalArgumentException("Unsupported language: " + lang);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error executing method: " + methodName, e);
        }
    }

}
