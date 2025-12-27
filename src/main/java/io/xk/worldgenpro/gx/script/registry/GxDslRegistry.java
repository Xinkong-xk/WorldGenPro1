package io.xk.worldgenpro.gx.script.registry;

import io.xk.worldgenpro.gx.expression.compilers.JSExpressionCompiler;
import io.xk.worldgenpro.gx.expression.compilers.JavaExpressionCompiler;
import io.xk.worldgenpro.gx.expression.compilers.LuaExpressionCompiler;
import io.xk.worldgenpro.gx.script.GxLang;
import io.xk.worldgenpro.gx.script.GeneralExtensibleScriptEngine;
import io.xk.worldgenpro.gx.script.GxScript;
import org.graalvm.polyglot.Value;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class GxDslRegistry {

    /** 存放模板翻译型 DSL 指令 */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<GxLang, String>> DSL_MAP = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    /** 存放逻辑执行型 DSL 指令 */
    private static final ConcurrentHashMap<String, BiConsumer<GeneralExtensibleScriptEngine, List<String>>> EXEC_MAP = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    public static String getClassName(){
        return GxDslRegistry.class.getName();
    }

    /**
     * 注册 DSL 指令的模板实现。
     * 适用于“纯翻译”的指令，如 #debug "text"
     */
    public static void register(String name, ConcurrentHashMap<GxLang, String> langTranslations) {
        DSL_MAP.put(name.toLowerCase(), langTranslations);
    }

    /**
     * 注册 DSL 指令的逻辑实现。
     * 适用于“动态行为”的指令，比如 #export、#include 等。
     */
    public static void register(String name, BiConsumer<GeneralExtensibleScriptEngine, List<String>> handler) {
        EXEC_MAP.put(name.toLowerCase(), handler);
    }

    /** 获取模板翻译 */
    public static String getTranslation(String name, GxLang lang) {
        Map<GxLang, String> map = DSL_MAP.get(name.toLowerCase());
        if (map == null) return null;
        return map.get(lang);
    }

    /** 获取逻辑实现 */
    public static BiConsumer<GeneralExtensibleScriptEngine, List<String>> getExecutor(String name) {
        return EXEC_MAP.get(name.toLowerCase());
    }

    /** 判断是否存在 */
    public static boolean contains(String name) {
        return DSL_MAP.containsKey(name.toLowerCase()) || EXEC_MAP.containsKey(name.toLowerCase());
    }

    /** 示例注册方法，可在启动时调用 */
    public static void registerDefaults() {
        register("debug", new ConcurrentHashMap<>(Map.of(
                GxLang.JAVA, "System.out.println(<?>);",
                GxLang.JavaScript, "console.log(<?>);",
                GxLang.LUA, "print(<?>)"
        )));

        // lang 指令: 语言项
        register("lang", (engine, args) -> {
            engine.currentLang = GxLang.fromString(args.getFirst());
        });

        // export 指令: 导出
        register("export", (engine, args) -> {
            engine.exportMethods.addAll(args);
        });

        // script 指令: 执行脚本
        register("script",(engine, args) -> {
            switch (engine.currentLang) {
                case JAVA->{
                    // JAVA 代码
                    String scriptPath = args.getFirst();
                    List<String> rawArgs = args.subList(1, args.size());
                    String argString = String.join(", ", rawArgs);

                    engine.appendCode(getClassName()+".scriptCommandInvoke("+
                            "\""+ scriptPath +"\""+
                                    argString+
                            ")"
                            );
                }
                case LUA -> {
                    engine.appendCodeLuaImport();
                    String scriptPath = args.getFirst();
                    List<String> rawArgs = args.subList(1, args.size());
                    String argString = String.join(", ", rawArgs);

                    // 生成调用行：__Gx_GxDslRegistry__:scriptCommandInvoke("path", arg1, arg2)
                    engine.appendCode("__Gx_GxDslRegistry__:scriptCommandInvoke(" +
                            "\"" + scriptPath + "\"" +
                            (!argString.isEmpty() ? ", {" + argString +"}" : "") +
                            ")\n"
                    );
                }
                case JavaScript -> {
                    engine.appendCodeJsImport();
                    String scriptPath = args.getFirst();
                    List<String> rawArgs = args.subList(1, args.size());
                    String argString = String.join(", ", rawArgs);

                    engine.appendCode("__Gx_GxDslRegistry__.scriptCommandInvoke("+
                            "\""+ scriptPath +"\""+
                            argString+
                            ")"
                    );

                }
            }
        });
    }

    public static void scriptCommandInvoke(String scriptPath, Object... args){
        GeneralExtensibleScriptEngine.INSTANCE.execute(scriptPath, args);
    }

    public static void scriptExecute(GeneralExtensibleScriptEngine engine, List<String> args) {
        String scriptPath = args.getFirst();
        Object[] rawArgs = args.subList(1, args.size()).toArray();

        // 获取被调用脚本的宿主语言
        String[] names = scriptPath.split(":");
        String scriptName = names[0].trim();
        GxScript targetScript = engine.scripts.get(scriptName);
        GxLang targetLang = targetScript.lang;

        // 这里做参数类型还原/表达式求值
        Object[] preparedArgs = Arrays.stream(rawArgs)
                .map(Object::toString)
                .map(s -> eval(targetLang,s))
                .toArray();

        engine.execute(scriptPath, preparedArgs);
    }

    public static Object eval(GxLang lang, String value) {
        return switch (lang) {
            case JAVA -> {
                // TODO: 调用 Java 表达式求值器
                yield JavaExpressionCompiler.eval(value);
            }
            case JavaScript -> {
                // 使用 GraalVM JS 执行表达式
                Value result = JSExpressionCompiler.THREAD_CONTEXT.eval("js", value);
                yield result.isNull() ? null : result.as(Object.class);
            }
            case LUA -> {
                // 使用 Lua 执行表达式
                LuaValue luaResult = LuaExpressionCompiler.GLOBALS.load(value).call();
                yield luaResult.isnil() ? null : luaResult;
            }
            default -> value; // 如果语言未知，直接返回字符串
        };
    }

}
