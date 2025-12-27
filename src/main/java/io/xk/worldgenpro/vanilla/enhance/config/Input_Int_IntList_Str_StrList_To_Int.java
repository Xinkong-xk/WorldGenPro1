package io.xk.worldgenpro.vanilla.enhance.config;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.xk.worldgenpro.api.vanilla.enhance.structure.IntRange;
import io.xk.worldgenpro.gx.expression.ExpressionManager;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.function.Consumer;

/**
 * Input_Int_IntList_Str_StrList_To_Int 支持:
 *  <br/>- 单个整数: 80
 *  <br/>- 两整数列表: [64,128]
 *  <br/>- 单个字符串: "expr"
 *  <br/>- 字符串列表: ["expr1","expr2"]
 */
public final class Input_Int_IntList_Str_StrList_To_Int {
    private final IntRange intRange;
    private final List<String> expressionLines;
    private String expressionName; // 编译缓存名称

    public Input_Int_IntList_Str_StrList_To_Int(IntRange intRange, List<String> exprs) {
        this.intRange = intRange;
        this.expressionLines = exprs == null ? null : List.copyOf(exprs);
    }

    public static Input_Int_IntList_Str_StrList_To_Int ofRange(int min, int max) {
        return new Input_Int_IntList_Str_StrList_To_Int(IntRange.of(min, max), null);
    }

    public static Input_Int_IntList_Str_StrList_To_Int ofSingleInt(int value) {
        return ofRange(value, value);
    }

    public static Input_Int_IntList_Str_StrList_To_Int ofExpr(String expr) {
        return new Input_Int_IntList_Str_StrList_To_Int(null, List.of(expr));
    }

    public static Input_Int_IntList_Str_StrList_To_Int ofExprList(List<String> exprs) {
        return new Input_Int_IntList_Str_StrList_To_Int(null, List.copyOf(exprs));
    }

    public int evaluate(Random random, ExpressionManager exprManager, LinkedHashMap<String, Object> contextVars) {
        // 固定值或范围随机
        if (intRange != null) {
            return intRange.random(random);
        }

        // 表达式逻辑
        if (expressionLines != null && !expressionLines.isEmpty()) {
            // 若没有命名，则用哈希代替随机 UUID，使内容相同的表达式可重用
            if (expressionName == null) {
                expressionName = "Input_Int_IntList_Str_StrList_To_Int_" + UUID.randomUUID();
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
                        Number.class
                );
            }

            // 执行已缓存或新编译的表达式
            List<Object> args = new ArrayList<>(contextVars.values());
            Object result = exprManager.runExpression(expressionName, args);

            if (result instanceof Number n) {
                return n.intValue();
            } else {
                throw new IllegalStateException("表达式未返回 Number 类型: " + result);
            }
        }

        throw new IllegalStateException("Input_Int_IntList_Str_StrList_To_Int 未配置有效值");
    }

    /**Codec 实现（修正版）**/
    public static final Codec<Input_Int_IntList_Str_StrList_To_Int> CODEC = new Codec<>() {

        @Override
        public <T> DataResult<Pair<Input_Int_IntList_Str_StrList_To_Int, T>> decode(DynamicOps<T> ops, T input) {
            // 1) 尝试 number（单个整数）
            DataResult<Number> maybeNum = ops.getNumberValue(input);
            if (maybeNum.result().isPresent()) {
                return maybeNum.map(n -> Pair.of(Input_Int_IntList_Str_StrList_To_Int.ofSingleInt(n.intValue()), input));
            }

            // 2) 尝试 list of numbers (e.g. [min,max])
            // --- 2. 尝试 list ---
            DataResult<Consumer<Consumer<T>>> maybeList = ops.getList(input);
            if (maybeList.result().isPresent()) {
                List<T> rawList = new ArrayList<>();
                maybeList.result().get().accept(rawList::add);  // 把每个元素塞进 list

                if (!rawList.isEmpty()) {
                    // 尝试解析为数字列表
                    List<Number> nums = new ArrayList<>();
                    boolean allNums = true;
                    for (T elem : rawList) {
                        Optional<Number> n = ops.getNumberValue(elem).result();
                        if (n.isPresent()) nums.add(n.get());
                        else { allNums = false; break; }
                    }
                    if (allNums) {
                        if (nums.size() == 1) {
                            return DataResult.success(Pair.of(Input_Int_IntList_Str_StrList_To_Int.ofSingleInt(nums.get(0).intValue()), input));
                        }
                        if (nums.size() == 2) {
                            int min = nums.get(0).intValue();
                            int max = nums.get(1).intValue();
                            return DataResult.success(Pair.of(Input_Int_IntList_Str_StrList_To_Int.ofRange(min, max), input));
                        }
                        return DataResult.error(() -> "Expected list of 1 or 2 numbers, got " + nums.size());
                    }

                    // 尝试解析为字符串列表
                    List<String> strs = new ArrayList<>();
                    boolean allStrings = true;
                    for (T elem : rawList) {
                        Optional<String> s = ops.getStringValue(elem).result();
                        if (s.isPresent()) strs.add(s.get());
                        else { allStrings = false; break; }
                    }
                    if (allStrings && !strs.isEmpty()) {
                        return DataResult.success(Pair.of(Input_Int_IntList_Str_StrList_To_Int.ofExprList(strs), input));
                    }
                }
            }

            // 3) 尝试 string
            DataResult<String> maybeStr = ops.getStringValue(input);
            if (maybeStr.result().isPresent()) {
                return maybeStr.map(s -> Pair.of(Input_Int_IntList_Str_StrList_To_Int.ofExpr(s), input));
            }

            // 如果以上全部失败，返回通用错误
            return DataResult.error(() -> "Invalid Input_Int_IntList_Str_StrList_To_Int Config format: expected number, [number,number], string or [string,...]");
        }

        @Override
        public <T> DataResult<T> encode(Input_Int_IntList_Str_StrList_To_Int input, DynamicOps<T> ops, T prefix) {
            if (input.intRange != null) {
                int min = input.intRange.min();
                int max = input.intRange.max();
                if (min == max) {
                    return DataResult.success(ops.createNumeric(min));
                } else {
                    List<T> list = List.of(ops.createNumeric(min), ops.createNumeric(max));
                    return DataResult.success(ops.createList(list.stream()));
                }
            }

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

            return DataResult.error(() -> "Cannot encode empty Input_Int_IntList_Str_StrList_To_Int Config");
        }

        @Override
        public String toString() {
            return "Input_Int_IntList_Str_StrList_To_Int:Codec";
        }
    };
}
