package io.xk.worldgenpro.generator.structure.core.api;

import java.util.Objects;

@FunctionalInterface
public interface TConsumer<T, U,S> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    void accept(T t, U u,S s);


    default TConsumer<T, U,S> andThen(TConsumer<? super T, ? super U,? super S> after) {
        Objects.requireNonNull(after);

        return (l, r,d) -> {
            accept(l, r,d);
            after.accept(l, r,d);
        };
    }
}