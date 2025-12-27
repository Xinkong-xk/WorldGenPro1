package io.xk.worldgenpro.doc.expression;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表示单个表达式配置项的文档信息
 */
public class SimpleExpressionDoc {
    public final String id;                   // 配置项名称
    public final Map<String, Class<?>> params; // 参数名 -> 类型
    public final Class<?> returnType;         // 返回类型
    public final String hint;                 // 提示文本

    public SimpleExpressionDoc(String id, Map<String, Class<?>> params, Class<?> returnType, String hint) {
        this.id = id;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
        this.returnType = returnType;
        this.hint = hint;
    }
}
