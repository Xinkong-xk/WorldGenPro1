package io.xk.worldgenpro.doc.expression;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理所有表达式文档注册与导出
 */
public class SimpleExpressionDocRegistry {

    // 存储注册的文档，Key = 配置项ID
    private static final Map<String, SimpleExpressionDoc> DOCS = new LinkedHashMap<>();

    /**
     * 注册新的表达式文档
     */
    public static void register(SimpleExpressionDoc doc) {
        if (doc == null || doc.id == null || doc.id.isEmpty()) {
            throw new IllegalArgumentException("Document or id cannot be null/empty");
        }
        DOCS.put(doc.id, doc);
    }

    /**
     * 获取注册文档
     */
    public static SimpleExpressionDoc get(String id) {
        return DOCS.get(id);
    }

    /**
     * 导出所有注册文档为 Markdown 格式
     */
    public static String exportToMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("# Expression Configuration Reference\n\n");

        for (SimpleExpressionDoc doc : DOCS.values()) {
            md.append("## ").append(doc.id).append("\n\n");
            md.append("- **返回类型**: ").append(doc.returnType.getSimpleName()).append("\n");

            if (!doc.params.isEmpty()) {
                md.append("- **参数**:\n");
                for (Map.Entry<String, Class<?>> entry : doc.params.entrySet()) {
                    md.append("  - `").append(entry.getKey()).append("`: ").append(entry.getValue().getSimpleName()).append("\n");
                }
            } else {
                md.append("- **参数**: 无\n");
            }

            md.append("- **提示**: ").append(doc.hint).append("\n\n");
        }

        return md.toString();
    }

    /**
     * 清空注册文档（测试用）
     */
    public static void clear() {
        DOCS.clear();
    }
}
