package io.xk.worldgenpro.doc.expression;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestDoc {
    public static void main(String[] args) {
        Map<String, Class<?>> params1 = new LinkedHashMap<>();
        params1.put("distance", int.class);
        params1.put("baseWeight", double.class);
        params1.put("rand", java.util.Random.class);

        SimpleExpressionDocRegistry.register(new SimpleExpressionDoc(
                "jigsaw.element.weight",
                params1,
                double.class,
                "计算模板在当前位置的动态权重，用于模板选择"
        ));

        Map<String, Class<?>> params2 = new LinkedHashMap<>();
        params2.put("level", int.class);

        SimpleExpressionDocRegistry.register(new SimpleExpressionDoc(
                "pool.spawnChance",
                params2,
                double.class,
                "根据层级计算结构生成概率"
        ));
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.out.println(SimpleExpressionDocRegistry.exportToMarkdown());
    }
}
