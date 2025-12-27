package io.xk.worldgenpro.gx.expression.test;

import io.xk.worldgenpro.gx.expression.ExpressionManager;

import java.util.*;

public class ExpressionCompilerTest {

    public static void main(String[] args) {
        ExpressionManager manager = new ExpressionManager();

        // 公共参数（每个表达式都会收到这些参数）
        LinkedHashMap<String, Class<?>> argsType = new LinkedHashMap<>();
        argsType.put("a", Integer.class);
        argsType.put("b", Integer.class);

        List<Object> values = Arrays.asList(10, 5);

        // =============== 1. Java 单行表达式（无 return） ===============
        manager.registerExpressionAuto(
                "java_single",
                List.of("a * b + 3"),
                argsType,
                Number.class
        );
        System.out.println("java_single => " + manager.runExpression("java_single", values));

        // =============== 2. Java 多行表达式（最后一行自动 return） ===============
        manager.registerExpressionAuto(
                "java_multi",
                List.of(
                        "int c = a + b;",
                        "int d = c * 2;",
                        "return d + 1;"
                ),
                argsType,
                Number.class
        );
        System.out.println("java_multi  => " + manager.runExpression("java_multi", values));

        // =============== 3. Lua 单行表达式（无 return） ===============
        manager.registerExpressionAuto(
                "lua_single",
                List.of("@Lua", "a * b + 7"),
                argsType,
                Number.class
        );
        System.out.println("lua_single => " + manager.runExpression("lua_single", values));

        // =============== 4. Lua 多行表达式（有显式 return） ===============
        manager.registerExpressionAuto(
                "lua_multi",
                List.of(
                        "@Lua",
                        "c = a + b",
                        "d = c * 3",
                        "return d - 4"
                ),
                argsType,
                Number.class
        );
        System.out.println("lua_multi => " + manager.runExpression("lua_multi", values));

        // =============== 5. JavaScript 单行表达式（无 return） ===============
        manager.registerExpressionAuto(
                "js_single",
                List.of("@JavaScript", "a * b + 11"),
                argsType,
                Number.class
        );
        System.out.println("js_single => " + manager.runExpression("js_single", values));

        // =============== 6. JavaScript 多行表达式（最后一行自动 return） ===============
        manager.registerExpressionAuto(
                "js_multi",
                List.of(
                        "@JavaScript",
                        "let c = a - b;",
                        "let d = c * 4;",
                        "return d + 2"
                ),
                argsType,
                Number.class
        );
        System.out.println("js_multi => " + manager.runExpression("js_multi", values));

        // =============== 验证缓存复用 ===============
        System.out.println("cache size :" + manager.cacheSize());
        System.out.println("again java_multi => " + manager.runExpression("java_multi", values));
    }
}
