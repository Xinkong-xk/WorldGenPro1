package io.xk.worldgenpro.generator.structure.core.api;

import java.util.Objects;

@FunctionalInterface
public interface FConsumer<T, U,S,A> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    void accept(T t, U u,S s,A a);


    default FConsumer<T, U,S,A> andThen(FConsumer<? super T, ? super U,? super S,? super  A> after) {
        Objects.requireNonNull(after);

        return (l, r,d,a) -> {
            accept(l, r,d,a);
            after.accept(l, r,d,a);
        };
    }
}