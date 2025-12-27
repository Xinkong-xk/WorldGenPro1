package io.xk.worldgenpro.gx.expression.compilers;

import io.xk.worldgenpro.gx.expression.api.ExpressionCompiler;
import io.xk.worldgenpro.utils.ClassInitializer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class JavaExpressionCompiler extends ExpressionCompiler {

    public static final ObjectArrayList<String> defaultImports = new ObjectArrayList<>(BASE_BIG_CAPACITY);
    static {
        defaultImports.add("import java.util.*;\n");
        defaultImports.add("import java.util.function.*;\n");
        defaultImports.add("import static java.lang.Math.*;\n");
        defaultImports.add("import static java.lang.StrictMath.*;\n");
    }

    private static final ConcurrentHashMap<String, Method> EVAL_METHOD_CACHE = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    public static Object eval(String value) {
        try {
            Method getValueMethod = EVAL_METHOD_CACHE.computeIfAbsent(value, v -> {
                try {
                    String className = "EvalExpr_" + UUID.randomUUID().toString().replace("-", "");
                    StringBuilder code = new StringBuilder();
                    code.append(String.join("", defaultImports));
                    code.append("public class ").append(className).append(" {\n")
                            .append("  public Object getValue() {\n")
                            .append("    return ").append(v).append(";\n")
                            .append("  }\n")
                            .append("}\n");

                    SimpleCompiler compiler = new SimpleCompiler();
                    compiler.cook(code.toString());
                    Class<?> clazz = compiler.getClassLoader().loadClass(className);
                    return clazz.getMethod("getValue");

                } catch (Exception e) {
                    throw new RuntimeException("Failed to compile Java expression: " + v, e);
                }
            });

            // 每次都新建实例
            Object instance = getValueMethod.getDeclaringClass().getDeclaredConstructor().newInstance();
            return getValueMethod.invoke(instance);

        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate Java expression: " + value, e);
        }
    }



    @Override
    public <T> Function<List<Object>, T> compileExpression(List<String> scriptTexts, LinkedHashMap<String, Class<?>> args, Class<T> resultType) {
        try {
            String className = "ExprEval_" + UUID.randomUUID().toString().replace("-", "");
            StringBuilder code = new StringBuilder();
            code.append(String.join("", defaultImports));
            code.append("public class ").append(className)
                    .append(" implements java.util.function.Function<java.util.List<java.lang.Object>, java.lang.Object> {\n");
            code.append("  @Override public Object apply(Object arg) {\n");
            code.append("    List args = (List) arg;\n");

            int index = 0;
            for (Map.Entry<String, Class<?>> entry : args.entrySet()) {
                code.append("    ").append(entry.getValue().getName()).append(" ")
                        .append(entry.getKey()).append(" = (")
                        .append(entry.getValue().getName()).append(") args.get(")
                        .append(index++).append(");\n");
            }

            // 单行脚本：若未显式 return，则自动补上
            if (scriptTexts.size() == 1) {
                String line = scriptTexts.getFirst().trim();
                if (!line.startsWith("return")) {
                    code.append("    return ((").append(resultType.getName()).append(")(").append(line).append("));\n");
                } else {
                    code.append("    ").append(line).append("\n");
                }
            } else {
                // 多行脚本：完全由用户负责 return
                for (String line : scriptTexts) {
                    code.append("    ").append(line).append("\n");
                }
            }

            code.append("  }\n}\n");

            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(code.toString());
            Class<?> clazz = compiler.getClassLoader().loadClass(className);

            @SuppressWarnings("unchecked")
            Function<List<Object>, T> func =
                    (Function<List<Object>, T>) clazz.getDeclaredConstructor().newInstance();

            return func;

        } catch (Exception e) {
            throw new RuntimeException("Failed to compile Java expression", e);
        }
    }

    @Override
    public Consumer<List<Object>> compileProcess(
            List<String> scriptTexts,
            LinkedHashMap<String, Class<?>> args
    ) {
        try {
            String className = "ProcEval_" + UUID.randomUUID().toString().replace("-", "");
            StringBuilder code = new StringBuilder();

            code.append(String.join("", defaultImports));

            code.append("public class ").append(className)
                    .append(" implements java.util.function.Consumer<java.util.List<java.lang.Object>> {\n");

            code.append("  @Override public void accept(java.util.List<java.lang.Object> arg) {\n");
            code.append("    java.util.List args = arg;\n");

            int index = 0;
            for (Map.Entry<String, Class<?>> entry : args.entrySet()) {
                code.append("    ").append(entry.getValue().getName()).append(" ")
                        .append(entry.getKey()).append(" = (")
                        .append(entry.getValue().getName()).append(") args.get(")
                        .append(index++).append(");\n");
            }

            // 过程不需要 return，全部视为执行语句
            for (String line : scriptTexts) {
                code.append("    ").append(line).append("\n");
            }

            code.append("  }\n}\n");

            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(code.toString());
            Class<?> clazz = compiler.getClassLoader().loadClass(className);

            @SuppressWarnings("unchecked")
            Consumer<List<Object>> proc =
                    (Consumer<List<Object>>) clazz.getDeclaredConstructor().newInstance();

            return proc;

        } catch (Exception e) {
            throw new RuntimeException("Failed to compile Java process", e);
        }
    }

    public boolean isInitialized = false;

    @Override
    public void initialize() {
        if (!this.isInitialized) {
            ClassInitializer.forceInitialize(this.getClass());
            this.isInitialized = true;
        }
    }

    @Override
    public boolean isInitialized() {
        return this.isInitialized;
    }
}
