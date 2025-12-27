package io.xk.worldgenpro.gx.expression.api;

import io.xk.worldgenpro.api.Initializable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ExpressionCompiler implements Initializable {
    public abstract <T> Function<List<Object>, T> compileExpression(List<String> scriptTexts, LinkedHashMap<String, Class<?>> args, Class<T> resultType);

    public abstract Consumer<List<Object>> compileProcess(List<String> scriptTexts, LinkedHashMap<String, Class<?>> args);
}
