package io.xk.worldgenpro.gx.expression;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.api.Initializable;
import io.xk.worldgenpro.gx.expression.api.ExpressionCompiler;
import io.xk.worldgenpro.gx.expression.compilers.JavaExpressionCompiler;
import io.xk.worldgenpro.gx.expression.compilers.LuaExpressionCompiler;
import io.xk.worldgenpro.gx.expression.compilers.JSExpressionCompiler;
import io.xk.worldgenpro.gx.script.GeneralExtensibleScriptEngine;
import io.xk.worldgenpro.gx.script.GxLang;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class ExpressionManager implements Initializable {

    private boolean isInitialized = false;

    // Java 编译器
    public final ExpressionCompiler javaCompiler = new JavaExpressionCompiler();

    // Lua 编译器
    public final ExpressionCompiler luaCompiler = new LuaExpressionCompiler();

    // JavaScript 编译器
    public final ExpressionCompiler jsCompiler = new JSExpressionCompiler();

    // 表达式缓存表
    public final Map<String, Function<List<Object>, ?>> expressionCache = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    // 过程缓存表
    public final Map<String, Consumer<List<Object>>> processCache = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    @Override
    public void initialize() {
        if (! this.isInitialized) {
            // 初始化三个编译器
            javaCompiler.initialize();
            jsCompiler.initialize();
            luaCompiler.initialize();
            this.isInitialized = true;
        }
    }

    @Override
    public boolean isInitialized() {
        return this.isInitialized;
    }

    /**
     * 表达式是否存在
     * @param expressionName 表达式名称
     * @return 是否存在
     */
    public boolean hasExpression(String expressionName) {
        return expressionCache.containsKey(expressionName);
    }

    /**
     * 过程是否存在
     * @param processName 过程名
     * @return 是否存在
     */
    public boolean hasProcess(String processName) {return processCache.containsKey(processName);}


    // 正则匹配：支持 @Lua> 或 #!lua，忽略大小写，并允许多个空格或制表符
    public static final Pattern INLINE_LANG_PATTERN =
            Pattern.compile("^(?:@([A-Za-z]+)\\s*>?\\s*|#!([A-Za-z]+)\\s+)\\s*(.*)$");


    /**
     *
     * @param name 表达式名称
     * @param scriptTexts 脚本文本列表
     * @param args 参数名称类型列表
     * @param resultType 返回值类型
     * @param <T> 返回值类型
     */
    @SuppressWarnings("unchecked")
    public <T> void registerExpressionAuto(
            String name,
            List<String> scriptTexts,
            LinkedHashMap<String, Class<?>> args,
            Class<T> resultType
    ) {
        if (scriptTexts == null || scriptTexts.isEmpty()) {
            throw new IllegalArgumentException("Script texts cannot be null or empty");
        }

        GxLang lang = GxLang.JAVA; // 默认语言
        List<String> cleaned = new ArrayList<>(scriptTexts);

        // --- 单行表达式检测 @Lang> 或 #!lang ---
        if (scriptTexts.size() == 1) {
            String line = scriptTexts.get(0).trim();
            Matcher m = INLINE_LANG_PATTERN.matcher(line);
            if (m.matches()) {
                // 捕获语言名（第1或第2捕获组）
                String langName = (m.group(1) != null ? m.group(1) : m.group(2)).toLowerCase(Locale.ROOT);
                if (langName == null || langName.isEmpty()) {
                    // 用户没写语言标记，使用默认语言
                    lang = GxLang.JAVA;
                } else {
                    switch (langName.toLowerCase()) {
                        case "lua" -> lang = GxLang.LUA;
                        case "javascript", "js" -> lang = GxLang.JavaScript;
                        case "java" -> lang = GxLang.JAVA;
                        default -> throw new IllegalArgumentException("未知的脚本语言标识符: @" + langName);
                    }
                }

                // 捕获表达式主体部分
                String code = m.group(3).trim();
                if (code.contains("#")) {
                    // 如果使用了gx指令
                    GeneralExtensibleScriptEngine.INSTANCE.currentLang = lang;
                    code = GeneralExtensibleScriptEngine.INSTANCE.parse(List.of(new String[]{m.group(3).trim()}));
                }

                cleaned = List.of(code);
            }
        }

        // --- 多行表达式检测旧语法 @Lua / @JavaScript ---
        else {
            String firstLine = scriptTexts.get(0).trim();
            if (firstLine.matches("^@?\\s*(?i)(java|javascript|lua)\\b.*")) {
                String lower = firstLine.replace("@", "").trim().toLowerCase(Locale.ROOT);
                if (lower.startsWith("lua")) lang = GxLang.LUA;
                else if (lower.startsWith("javascript")) lang = GxLang.JavaScript;
                else lang = GxLang.JAVA;

                cleaned.remove(0);
            }

            // 清理输出缓冲区
            GeneralExtensibleScriptEngine.INSTANCE.clearOutput();
            // 设置宿主语言
            GeneralExtensibleScriptEngine.INSTANCE.currentLang = lang;
            cleaned = List.of(GeneralExtensibleScriptEngine.INSTANCE.parse(cleaned).split("\n"));
        }

        registerExpressionInternal(name, lang, cleaned, args, resultType);
    }

    /**
     * 表达式内部注册
     * @param name 表达式名称
     * @param lang 表达式宿主语言
     * @param scriptTexts 脚本文本列表
     * @param args 参数名称类型列表
     * @param resultType 返回值类型
     * @param <T> 返回值类型
     */
    private <T> void registerExpressionInternal(
            String name,
            GxLang lang,
            List<String> scriptTexts,
            LinkedHashMap<String, Class<?>> args,
            Class<T> resultType
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Expression name cannot be null or empty");
        }

        ExpressionCompiler compiler = switch (lang) {
            case LUA -> luaCompiler;
            case JavaScript -> jsCompiler;
            default -> javaCompiler;
        };

        Function<List<Object>, T> func = compiler.compileExpression(scriptTexts, args, resultType);
        expressionCache.put(name, func);
    }

    /**
     * 自动注册过程
     * @param name 过程名
     * @param scriptTexts 脚本文本列表
     * @param args 参数名称类型列表
     */
    public void registerProcessAuto(
            String name,
            List<String> scriptTexts,
            LinkedHashMap<String, Class<?>> args
    ) {
        if (scriptTexts == null || scriptTexts.isEmpty()) {
            throw new IllegalArgumentException("Script texts cannot be null or empty");
        }

        GxLang lang = GxLang.JAVA; // 默认语言
        List<String> cleaned = new ArrayList<>(scriptTexts);

        // --- 单行语法解析 ---
        if (scriptTexts.size() == 1) {
            String line = scriptTexts.get(0).trim();
            Matcher m = INLINE_LANG_PATTERN.matcher(line);
            if (m.matches()) {
                String langName = (m.group(1) != null ? m.group(1) : m.group(2)).toLowerCase(Locale.ROOT);
                if (langName != null && !langName.isEmpty()) {
                    switch (langName) {
                        case "lua" -> lang = GxLang.LUA;
                        case "javascript", "js" -> lang = GxLang.JavaScript;
                        case "java" -> lang = GxLang.JAVA;
                        default -> throw new IllegalArgumentException("未知的脚本语言标识符: @" + langName);
                    }
                }

                String code = m.group(3).trim();
                if (code.contains("#")) {
                    GeneralExtensibleScriptEngine.INSTANCE.currentLang = lang;
                    code = GeneralExtensibleScriptEngine.INSTANCE.parse(List.of(new String[]{m.group(3).trim()}));
                }

                cleaned = List.of(code);
            }
        }
        // --- 多行旧风格 ---
        else {
            String firstLine = scriptTexts.get(0).trim();
            if (firstLine.matches("^@?\\s*(?i)(java|javascript|lua)\\b.*")) {
                String lower = firstLine.replace("@", "").trim().toLowerCase(Locale.ROOT);
                if (lower.startsWith("lua")) lang = GxLang.LUA;
                else if (lower.startsWith("javascript")) lang = GxLang.JavaScript;
                else lang = GxLang.JAVA;

                cleaned.remove(0);
            }

            // 清理输出缓冲区
            GeneralExtensibleScriptEngine.INSTANCE.clearOutput();
            // 设置宿主语言
            GeneralExtensibleScriptEngine.INSTANCE.currentLang = lang;
            cleaned = List.of(GeneralExtensibleScriptEngine.INSTANCE.parse(cleaned).split("\n"));
        }

        registerProcessInternal(name, lang, cleaned, args);
    }

    /**
     * 过程内部注册
     * @param name 过程名
     * @param lang 过程宿主语言
     * @param scriptTexts 脚本文本列表
     * @param args 参数名称类型列表
     */
    private void registerProcessInternal(
            String name,
            GxLang lang,
            List<String> scriptTexts,
            LinkedHashMap<String, Class<?>> args
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Process name cannot be null or empty");
        }

        ExpressionCompiler compiler = switch (lang) {
            case LUA -> luaCompiler;
            case JavaScript -> jsCompiler;
            default -> javaCompiler;
        };

        // 使用过程编译方法来编译 compileProcess
        Consumer<List<Object>> proc = compiler.compileProcess(scriptTexts, args);

        processCache.put(name, proc);
    }


    /**
     * 执行一个表达式
     * @param name 表达式名称
     * @param args 参数列表
     * @return 返回值
     * @param <T> 返回值类型
     */
    @SuppressWarnings("unchecked")
    public <T> T runExpression(String name, List<Object> args) {
        Function<List<Object>, ?> func = expressionCache.get(name);
        if (func == null) {
            throw new IllegalStateException("No expression registered with name: " + name);
        }
        return (T) func.apply(args);
    }

    /**
     * 执行一个过程
     * @param name 过程名称
     * @param args 参数列表
     */
    @SuppressWarnings("unchecked")
    public void runProcess(String name, List<Object> args) {
        Consumer<List<Object>> proc = processCache.get(name);
        if (proc == null) {
            throw new IllegalStateException("No process registered with name: " + name);
        }
        proc.accept(args);
    }


    public void clearCache() {
        expressionCache.clear();
    }

    public int cacheSize() {
        return expressionCache.size();
    }

    public boolean contains(String name) {
        return expressionCache.containsKey(name);
    }

    public Set<String> getAllNames() {
        return expressionCache.keySet();
    }

    public static void main(String[] args) {

        // ------------------ 原本已有的测试 ------------------
        GeneralExtensibleScriptEngine.INSTANCE.compile(
                """
                    @js
                    #export b
                    function b(text,value) {
                        #debug text value
                    }
                """,
                "test"
        );

        WorldGenPro.expressionManager.registerExpressionAuto(
                "test",
                List.of(new String[]{
                        "@lua",
                        "#script test:b 1234567 value_1",
                        "return value_1"
                }),
                new LinkedHashMap<>() {{
                    put("value_1", Integer.class);
                }},
                Integer.class
        );

        int value = WorldGenPro.expressionManager.runExpression(
                "test",
                new ObjectArrayList<>() {{
                    add(1);
                }}
        );

        System.out.println("value is: " + value);


        // ============================
        // 新增：过程编译测试
        // ============================

        // 注册一个过程 test_proc
        // 脚本内容模拟一个简单的 lua 过程：输出 args + 修改局部值
        WorldGenPro.expressionManager.registerProcessAuto(
                "test_proc",
                List.of(
                        "@lua",
                        "print('process start, a =', a)",
                        "a = a + 10",
                        "print('process done, new a =', a)"
                ),
                new LinkedHashMap<>() {{
                    put("a", Integer.class);
                }}
        );

        // 调用过程
        System.out.println("----- running process test_proc -----");

        WorldGenPro.expressionManager.runProcess(
                "test_proc",
                new ObjectArrayList<>() {{
                    add(5);
                }}
        );

        System.out.println("----- process test complete -----");
    }

}
