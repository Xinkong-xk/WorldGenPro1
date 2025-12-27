package io.xk.worldgenpro.vanilla.enhance.config.api;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.xk.worldgenpro.gx.expression.ExpressionManager;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Str_StrList_To_Str;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.regex.Pattern;

public class IdentifierConfig {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");

    // 如果是合法 Identifier，则缓存
    private Identifier cachedId = null;

    // 如果是表达式或字符串列表，交给 Input_Str_StrList_To_Str
    private Input_Str_StrList_To_Str inputExpr = null;

    public IdentifierConfig(List<String> exprs) {
        if (exprs == null || exprs.isEmpty()) return;

        if (exprs.size() == 1 && IDENTIFIER_PATTERN.matcher(exprs.get(0)).matches()) {
            this.cachedId = new Identifier(exprs.get(0));
        } else {
            this.inputExpr = new Input_Str_StrList_To_Str(exprs);
        }
    }

    public boolean isLiteral() {
        return cachedId != null;
    }

    /** 获取最终 Identifier（动态计算时调用表达式） */
    public Identifier get(Random rand, ExpressionManager manager, LinkedHashMap<String, Object> contextVars) {
        if (cachedId != null) return cachedId;
        if (inputExpr != null) {
            String val = inputExpr.evaluate(rand, manager, contextVars);
            // 如果表达式返回合法 Identifier，则缓存
            if (IDENTIFIER_PATTERN.matcher(val).matches()) {
                cachedId = new Identifier(val);
                return cachedId;
            }
            return new Identifier(val); // 不合法也返回，用于占位
        }
        return new Identifier("minecraft:empty");
    }

    /** Codec */
    public static final Codec<IdentifierConfig> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<IdentifierConfig, T>> decode(DynamicOps<T> ops, T input) {
            // 1 尝试单个字符串
            DataResult<String> maybeStr = ops.getStringValue(input);
            if (maybeStr.result().isPresent()) {
                String s = maybeStr.result().get();
                if (IDENTIFIER_PATTERN.matcher(s).matches()) {
                    IdentifierConfig cfg = new IdentifierConfig(List.of(s));
                    return DataResult.success(Pair.of(cfg, input));
                } else {
                    // 非标准 Identifier 交给 Input_Str_StrList_To_Str
                    DataResult<Pair<Input_Str_StrList_To_Str, T>> inner = Input_Str_StrList_To_Str.CODEC.decode(ops, input);
                    return inner.map(pair -> {
                        IdentifierConfig cfg = new IdentifierConfig(null);
                        cfg.inputExpr = pair.getFirst();
                        return Pair.of(cfg, pair.getSecond());
                    });
                }
            }

            // 2 尝试字符串列表
            var maybeList = ops.getList(input);
            if (maybeList.result().isPresent()) {
                List<T> raw = new ArrayList<>();
                maybeList.result().get().accept(raw::add);

                List<String> lines = new ArrayList<>();
                for (T t : raw) {
                    var s = ops.getStringValue(t).result();
                    if (s.isEmpty()) {
                        return DataResult.error(() -> "IdentifierConfig 要求 string 列表，但存在非字符串元素");
                    }
                    lines.add(s.get());
                }

                IdentifierConfig cfg = new IdentifierConfig(null);
                cfg.inputExpr = new Input_Str_StrList_To_Str(lines);
                return DataResult.success(Pair.of(cfg, input));
            }

            return DataResult.error(() -> "Invalid IdentifierConfig format: expected string or [string,...]");
        }

        @Override
        public <T> DataResult<T> encode(IdentifierConfig input, DynamicOps<T> ops, T prefix) {
            if (input.cachedId != null) {
                return DataResult.success(ops.createString(input.cachedId.toString()));
            } else if (input.inputExpr != null) {
                return Input_Str_StrList_To_Str.CODEC.encode(input.inputExpr, ops, prefix);
            }
            return DataResult.error(() -> "Cannot encode empty IdentifierConfig");
        }

        @Override
        public String toString() {
            return "IdentifierConfig:Codec";
        }
    };

    @Override
    public String toString() {
        return "IdentifierConfig{" +
                "cachedId=" + cachedId +
                ", inputExpr=" + inputExpr +
                '}';
    }
}
