package io.xk.worldgenpro.vanilla.enhance.config;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.xk.worldgenpro.gx.expression.ExpressionManager;
import net.minecraft.util.math.random.Random;

import java.util.*;

/**
 *  Input_Str_StrList_To_Null 支持:
 *  <br/>- 单个字符串: "expr"
 *  <br/>- 字符串列表: ["expr1","expr2"]
 *  <br/> 返回:Null (不返回)
 */
public final class Input_Str_StrList_To_Null {

    private final List<String> processLines; // 缓存的过程
    private String processName; // 缓存名称(processName)

    public boolean isEmpty = false;

    public Input_Str_StrList_To_Null(List<String> exprs) {
        if (exprs == null) {
            this.processLines = null;
        } else {
            // 过滤空行（全空白也认为空）
            List<String> cleaned = exprs.stream()
                    .map(s -> s == null ? "" : s.trim())
                    .filter(s -> !s.isEmpty())
                    .toList();

            this.processLines = cleaned.isEmpty() ? null : cleaned;
        }
        if (this.processLines == null) {
            // 标记为空
            isEmpty = true;
        }
    }

    public static Input_Str_StrList_To_Null ofExpr(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return new Input_Str_StrList_To_Null(null);
        }
        return new Input_Str_StrList_To_Null(List.of(expr));
    }

    public static Input_Str_StrList_To_Null ofExprList(List<String> exprs) {
        return new Input_Str_StrList_To_Null(exprs);
    }

    private void runProcess(ExpressionManager exprManager, LinkedHashMap<String, Object> contextVars) {

        if (processLines == null || processLines.isEmpty()) {
            // 空 → 不执行、不报错
            return;
        }

        if (processName == null) {
            processName = "Input_Str_StrList_To_Null_" + Objects.hash(processLines);
        }

        if (!exprManager.hasProcess(processName)) {

            LinkedHashMap<String, Class<?>> argTypes = new LinkedHashMap<>(contextVars.size()*2);
            for (var e : contextVars.entrySet()) {
                argTypes.put(e.getKey(), e.getValue().getClass());
            }

            exprManager.registerProcessAuto(
                    processName,
                    processLines,
                    argTypes
            );
        }

        exprManager.runProcess(
                processName,
                new ArrayList<>(contextVars.values())
        );
    }

    public void run(Random random, ExpressionManager exprManager, LinkedHashMap<String, Object> contextVars) {
        contextVars.put("random", random);
        runProcess(exprManager, contextVars);
    }

    public void run(ExpressionManager exprManager, LinkedHashMap<String, Object> contextVars) {
        runProcess(exprManager, contextVars);
    }

    /**
     * 只支持 string 或 string[]
     */
    public static final Codec<Input_Str_StrList_To_Null> CODEC = new Codec<>() {

        @Override
        public <T> DataResult<Pair<Input_Str_StrList_To_Null, T>> decode(DynamicOps<T> ops, T input) {

            // 尝试 string
            var maybeStr = ops.getStringValue(input);
            if (maybeStr.result().isPresent()) {
                return maybeStr.map(s -> Pair.of(Input_Str_StrList_To_Null.ofExpr(s), input));
            }

            // 尝试 string 列表
            var maybeList = ops.getList(input);
            if (maybeList.result().isPresent()) {

                List<T> raw = new ArrayList<>();
                maybeList.result().get().accept(raw::add);

                List<String> lines = new ArrayList<>();
                for (T t : raw) {
                    var s = ops.getStringValue(t).result();
                    if (s.isEmpty()) {
                        return DataResult.error(() -> "Input_Str_StrList_To_Null 要求 string 列表，但存在非字符串元素");
                    }
                    lines.add(s.get());
                }

                return DataResult.success(Pair.of(Input_Str_StrList_To_Null.ofExprList(lines), input));
            }

            return DataResult.error(() -> "Invalid Input_Str_StrList_To_Null Config format: expected string or [string,...]");
        }


        @Override
        public <T> DataResult<T> encode(Input_Str_StrList_To_Null input, DynamicOps<T> ops, T prefix) {

            if (input.processLines != null) {
                if (input.processLines.size() == 1) {
                    return DataResult.success(ops.createString(input.processLines.getFirst()));
                }

                int size = input.processLines.size();
                List<T> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(ops.createString(input.processLines.get(i)));
                }
                return DataResult.success(ops.createList(list.stream()));
            }

            // 空表达式 → 编码为空字符串
            return DataResult.success(ops.createString(""));
        }

        @Override
        public String toString() {
            return "Input_Str_StrList_To_Null:Codec";
        }
    };
}
