package io.xk.worldgenpro.vanilla.enhance.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SuperLargeConfig {

    private final boolean isSuperLarge;
    private final int scanRange;

    public SuperLargeConfig(boolean isSuperLarge, int scanRange) {
        this.isSuperLarge = isSuperLarge;
        this.scanRange = scanRange;
    }

    public boolean isSuperLarge() {
        return isSuperLarge;
    }

    public int getScanRange() {
        return scanRange;
    }

    /** Codec 实现 **/
    public static final Codec<SuperLargeConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("is_super_large").forGetter(SuperLargeConfig::isSuperLarge),
                    Codec.INT.fieldOf("scan_range").forGetter(SuperLargeConfig::getScanRange)
            ).apply(instance, SuperLargeConfig::new)
    );
}
