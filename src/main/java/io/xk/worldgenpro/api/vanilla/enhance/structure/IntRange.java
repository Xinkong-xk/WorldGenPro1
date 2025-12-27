package io.xk.worldgenpro.api.vanilla.enhance.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class IntRange {
    // IntRange.CODEC 改成 safer 版
    public static final Codec<IntRange> CODEC = Codec.either(
            Codec.INT,
            Codec.list(Codec.INT)
    ).xmap(
            either -> {
                if (either.left().isPresent()) {
                    int v = either.left().get();
                    return new IntRange(v, v);
                } else {
                    List<Integer> list = either.right().get();
                    if (list.size() != 2)
                        throw new IllegalArgumentException("Expected [min, max]");
                    return new IntRange(list.get(0), list.get(1));
                }
            },
            range -> range.min == range.max
                    ? Either.left(range.min)
                    : Either.right(List.of(range.min, range.max))
    );

    public final int min;

    public final int max;

    public IntRange(int min, int max) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("min and max values cannot be negative");
        }
        this.min = min;
        this.max = max;
    }

    public static IntRange of(int min, int max) {
        return new IntRange(min, max);
    }

    public int random(ChunkRandom random) {
        if (min == max) return min;
        return min + random.nextInt(max - min + 1);
    }

    public int random(Random random) {
        if (min == max) return min;
        return min + random.nextInt(max - min + 1);
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public String toString() {
        return "IntRange{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
