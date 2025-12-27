package io.xk.worldgenpro.vanilla.enhance.config;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.xk.worldgenpro.gx.expression.ExpressionManager;
import net.minecraft.util.math.random.Random;

import java.util.*;

/**
 *  Input_Str_StrList_To_Str 支持:
 *  <br/>- 单个字符串: "expr"
 *  <br/>- 字符串列表: ["expr1","expr2"]
 *  <br/> 返回:String
 */
public final class Input_Str_StrList_To_Str {
    private final List<String> expressionLines;
    private String expressionName; // 编译缓存名称

    public Input_Str_StrList_To_Str(List<String> exprs) {
        if (exprs == null) {
            this.expressionLines = null;
        } else {
            // 过滤空行（全空白也认为空）
            List<String> cleaned = exprs.stream()
                    .map(s -> s == null ? "" : s.trim())
                    .filter(s -> !s.isEmpty())
                    .toList();

            this.expressionLines = cleaned.isEmpty() ? null : cleaned;
        }
    }

    public static Input_Str_StrList_To_Str ofExpr(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return new Input_Str_StrList_To_Str(null);
        }
        return new Input_Str_StrList_To_Str(List.of(expr));
    }

    public static Input_Str_StrList_To_Str ofExprList(List<String> exprs) {
        return new Input_Str_StrList_To_Str(List.copyOf(exprs));
    }

    public String evaluate(Random random, ExpressionManager exprManager, LinkedHashMap<String, Object> contextVars) {
        // 表达式逻辑
        if (expressionLines != null && !expressionLines.isEmpty()) {
            // 若没有命名，则用哈希代替随机 UUID，使内容相同的表达式可重用
            if (expressionName == null) {
                expressionName = "Input_Str_StrList_To_Str_" + UUID.randomUUID();
            }

            // 检查表达式是否已存在
            if (!exprManager.hasExpression(expressionName)) {
                // 参数类型映射（保持一致）
                LinkedHashMap<String, Class<?>> argTypes = new LinkedHashMap<>(contextVars.size()*2);
                for (var e : contextVars.entrySet()) {
                    argTypes.put(e.getKey(), e.getValue().getClass());
                }

                exprManager.registerExpressionAuto(
                        expressionName,
                        expressionLines,
                        argTypes,
                        String.class
                );
            }

            // 执行已缓存或新编译的表达式
            List<Object> args = new ArrayList<>(contextVars.values());
            Object result = exprManager.runExpression(expressionName, args);

            if (result instanceof String n) {
                return n;
            } else {
                throw new IllegalStateException("表达式未返回 String 类型: " + result);
            }
        }

        throw new IllegalStateException("Input_Str_StrList_To_Str 未配置有效值");
    }

    /**Codec 实现（修正版）**/
    public static final Codec<Input_Str_StrList_To_Str> CODEC = new Codec<>() {

        @Override
        public <T> DataResult<Pair<Input_Str_StrList_To_Str, T>> decode(DynamicOps<T> ops, T input) {

            // 1 尝试 string
            DataResult<String> maybeStr = ops.getStringValue(input);
            if (maybeStr.result().isPresent()) {
                return maybeStr.map(s -> Pair.of(Input_Str_StrList_To_Str.ofExpr(s), input));
            }

            // --- 2. 尝试 list ---
            // 尝试 string 列表
            var maybeList = ops.getList(input);
            if (maybeList.result().isPresent()) {

                List<T> raw = new ArrayList<>();
                maybeList.result().get().accept(raw::add);

                List<String> lines = new ArrayList<>();
                for (T t : raw) {
                    var s = ops.getStringValue(t).result();
                    if (s.isEmpty()) {
                        return DataResult.error(() -> "Input_Str_StrList_To_Str 要求 string 列表，但存在非字符串元素");
                    }
                    lines.add(s.get());
                }

                return DataResult.success(Pair.of(Input_Str_StrList_To_Str.ofExprList(lines), input));
            }

            // 如果以上全部失败，返回通用错误
            return DataResult.error(() -> "Invalid Input_Str_StrList_To_Str Config format: expected string or [string,...]");
        }

        @Override
        public <T> DataResult<T> encode(Input_Str_StrList_To_Str input, DynamicOps<T> ops, T prefix) {
            if (input.expressionLines != null) {
                if (input.expressionLines.size() == 1) {
                    return DataResult.success(ops.createString(input.expressionLines.get(0)));
                } else {
                    int size = input.expressionLines.size();
                    List<T> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(ops.createString(input.expressionLines.get(i)));
                    }
                    return DataResult.success(ops.createList(list.stream()));
                }
            }

            return DataResult.error(() -> "Cannot encode empty Input_Str_StrList_To_Str Config");
        }

        @Override
        public String toString() {
            return "Input_Str_StrList_To_Str:Codec";
        }
    };
}
