package io.xk.worldgenpro.gx.expression.test;


import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * 一个完整的 LuaJ + Java 互操作测试类。
 * 目的：验证 Lua 中调用 Java 静态方法是否可行。
 */
public class LuaStaticCallTest {

    // ====== 被 Lua 调用的静态方法 ======

    public static void sayHello(Object name) {
        System.out.println("[Java] Hello, " + name + "!");
    }

    public static void main(String[] args) {
        // 创建 Lua 执行环境
        Globals globals = JsePlatform.standardGlobals();

        // Lua 脚本（可直接内嵌）
        String luaScript = """
            local luajava = require('luajava')
                                            local TestClass = luajava.bindClass('io.xk.worldgenpro.gx.expression.test.LuaStaticCallTest')
                                            TestClass:sayHello("xk")
                
        """;

        // 执行 Lua 脚本
        LuaValue chunk = globals.load(luaScript);
        chunk.call();
    }
}