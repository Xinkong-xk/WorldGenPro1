package io.xk.worldgenpro.gx.script;

import io.xk.worldgenpro.gx.script.registry.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class GeneralExtensibleScriptEngine {

    public static GeneralExtensibleScriptEngine INSTANCE = new GeneralExtensibleScriptEngine();

    public GxLang currentLang = null; // 默认宿主语言

    private final StringBuilder output = new StringBuilder();

    public ObjectArrayList<String> exportMethods = new ObjectArrayList<>(BASE_BIG_CAPACITY);

    public ConcurrentHashMap<String,GxScript> scripts = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    public GeneralExtensibleScriptEngine() {
        GxMetaRegistry.registerDefaults();
        GxDslRegistry.registerDefaults();
    }

    /** 追加输出（供 DSL 执行器使用） */
    public void appendCode(String s) {
        output.append(s).append("\n");
    }

    public void appendCodeJsImport() {
        this.appendCode("const __Gx_GxDslRegistry__ = Java.type(\""+  GxDslRegistry.getClassName() +"\");");
    }

    public void appendCodeLuaImport() {
        this.appendCode("local luajava = require('luajava')\n");
        this.appendCode("local __Gx_GxDslRegistry__ = luajava.bindClass(\"" + GxDslRegistry.getClassName() + "\")\n");
    }

    /** 获取最终结果 */
    public String getOutput() {
        return output.toString();
    }

    /** 解析整个脚本文件 */
    public String parse(List<String> lines) {
        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("//")) {
                appendCode(line);
                continue;
            }

            // 元信息指令转换
            if (line.startsWith("@") || line.startsWith("#!")) {
                line = GxMetaRegistry.translate(line);
                if (dsl_parse(line)){
                    continue;
                }
            }

            if (dsl_parse(line)){
                continue;
            }

            // Minecraft 指令桥接
            if (GxMinecraftBridge.isMinecraftCommand(line)) {
                appendCode(GxMinecraftBridge.translateToHost(line, currentLang.name()));
                continue;
            }

            // 其余为宿主语言原文
            appendCode(line);
        }

        return getOutput();
    }

    public boolean dsl_parse(String line){
        // DSL 指令解析
        if (line.startsWith("#")) {
            String[] tokens = line.substring(1).trim().split("\\s+", 2);
            String dslName = tokens[0];
            String argsPart = tokens.length > 1 ? tokens[1] : "";
            List<String> args = parseArgs(argsPart);

            // 优先查找逻辑型 DSL
            BiConsumer<GeneralExtensibleScriptEngine, List<String>> executor = GxDslRegistry.getExecutor(dslName);
            if (executor != null) {
                executor.accept(this, args);
                return true;
            }

            String template = GxDslRegistry.getTranslation(dslName, currentLang);
            if (template != null) {
                // 拼接模板与所有参数
                String replaced = template.replace("<?>",String.join(",", args));
                appendCode(replaced);
                return true;
            }

            appendCode("// Unknown DSL: " + line);
            return true;
        }
        return false;
    }

    /** 参数解析器：简单按空格分隔，保留所有原始字符（包括引号） */
    private List<String> parseArgs(String argString) {
        if (argString == null || argString.isBlank()) return List.of();
        // 按空格分隔，连续空格视为一个分隔符，不移除引号
        return Arrays.asList(argString.trim().split("\\s+"));
    }

    public void compile(String script, String scriptName) {
        // 每次编译前需要清空输出缓冲区
        this.clearOutput();
        // 分割代码
        List<String> lines = List.of(script.split("\n"));
        // 解析语法和指令
        String code = this.parse(lines);
        // 实例化脚本对象
        GxScript gxScript = new GxScript(this.currentLang, scriptName);
        // 编译宿主语言
        gxScript.compile(code,exportMethods);
        // 缓存
        scripts.put(scriptName, gxScript);
        // 编译完成后也清理一次缓冲区
        this.clearOutput();
    }

    public void clearOutput() {
        this.output.setLength(0);
    }

    // 使用脚本路径执行
    public void execute(String methodPath, Object ... args) {
        String[] names = methodPath.split(":");
        String scriptName = names[0].trim();
        String methodName = names[1].trim();
        execute(scriptName, methodName, args);
    }

    // 分别使用脚本名和方法名执行
    public void execute(String scriptName, String methodName ,Object... args) {
        if (scripts.containsKey(scriptName)) {
            scripts.get(scriptName).execute(methodName,args);
        }else{
            System.out.println("[GxScript Error] : Script File <"+scriptName+"> not found");
        }
    }


    // ----------- 测试 -----------
    public static void main(String[] argv) {

        GeneralExtensibleScriptEngine engine = GeneralExtensibleScriptEngine.INSTANCE;

        java:{
            String script_java = """
                    @java
                    #export test a
                    public void test(String text) {
                        #debug text
                    }
                    
                    public void a(int a1,int a2,String text) {
                        #debug a1+1
                        #debug a2+2
                        #debug text
                    }
                    """;

            engine.compile(script_java, "test_java");
            engine.execute("test_java", "test", "test for java");
        }

        js:{
            String script_js = """
                    @js
                    #export test b
                    function test(text) {
                        #debug text
                    }
                    
                    function b(b1,b2) {
                        #debug b1+3
                        #debug b2+4
                    }
                    """;

            engine.compile(script_js, "test_js");
            engine.execute("test_js", "test", "test for js");
        }

        lua:{
            String script_lua = """
                    @lua
                    #export test test2
                    function test(text)
                        #debug text
                    end
                    
                    function test2(text)
                        #debug text
                        #script test_java:a 1 2 text
                        #script test_js:b 1 10
                    end
                    """;

            engine.compile(script_lua, "test_lua");
            engine.execute("test_lua", "test", "test for lua");
            System.out.println("_________");
            engine.execute("test_lua", "test2", "test for lua 2 ");
            System.out.println("_________");
            engine.execute("test_lua", "test2", "3 ");

        }

    }
}
