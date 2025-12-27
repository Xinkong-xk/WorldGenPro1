package io.xk.worldgenpro.gx.expression.compilers;

import io.xk.worldgenpro.gx.expression.api.ExpressionCompiler;
import io.xk.worldgenpro.utils.ClassInitializer;
import org.graalvm.polyglot.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class JSExpressionCompiler extends ExpressionCompiler {

    public static final Context THREAD_CONTEXT = Context.newBuilder("js").allowAllAccess(true).build();


    @Override
    public <T> Function<List<Object>, T> compileExpression(List<String> scriptTexts,
                                                           LinkedHashMap<String, Class<?>> args,
                                                           Class<T> resultType) {
        try {
            StringBuilder body = new StringBuilder();

            if (scriptTexts.size() == 1) {
                String line = scriptTexts.getFirst().trim();
                if (!line.startsWith("return")) {
                    body.append("return ").append(line).append(";\n");
                } else {
                    body.append(line).append("\n");
                }
            } else {
                // 多行情况：不干预 return
                for (String line : scriptTexts) {
                    body.append(line).append("\n");
                }
            }

            StringBuilder funcBuilder = new StringBuilder();
            funcBuilder.append("(function(")
                    .append(String.join(",", args.keySet()))
                    .append("){\n")
                    .append(body)
                    .append("})");

            Value function = THREAD_CONTEXT.eval("js", funcBuilder.toString());

            return argsList -> {
                Value result = function.execute(argsList.toArray());
                if (result.isNumber()) {
                    double val = result.asDouble();
                    if (resultType == Long.class || resultType == long.class) return resultType.cast((long) val);
                    if (resultType == Integer.class || resultType == int.class) return resultType.cast((int) val);
                    if (resultType == Float.class || resultType == float.class) return resultType.cast((float) val);
                    if (resultType == Double.class || resultType == double.class) return resultType.cast(val);
                }
                return result.as(resultType);
            };

        } catch (Exception e) {
            throw new RuntimeException("Failed to compile JS expression", e);
        }
    }

    @Override
    public Consumer<List<Object>> compileProcess(
            List<String> scriptTexts,
            LinkedHashMap<String, Class<?>> args
    ) {
        try {

            StringBuilder body = new StringBuilder();

            // 过程：不处理 return
            for (String line : scriptTexts) {
                body.append(line).append("\n");
            }

            StringBuilder funcBuilder = new StringBuilder();
            funcBuilder.append("(function(")
                    .append(String.join(",", args.keySet()))
                    .append("){\n")
                    .append(body)
                    .append("})");

            Value function = THREAD_CONTEXT.eval("js", funcBuilder.toString());

            return argsList -> function.execute(argsList.toArray());

        } catch (Exception e) {
            throw new RuntimeException("Failed to compile JS process", e);
        }
    }


    public boolean isInitialized = false;

    @Override
    public void initialize() {
        if (!this.isInitialized) {
            ClassInitializer.forceInitialize(this.getClass());
            // 用于触发初始化
            THREAD_CONTEXT.eval("js","");
            this.isInitialized = true;
        }
    }

    @Override
    public boolean isInitialized() {
        return this.isInitialized;
    }
}
