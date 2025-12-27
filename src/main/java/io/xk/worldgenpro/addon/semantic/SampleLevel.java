package io.xk.worldgenpro.addon.semantic;

public enum SampleLevel {
    LEVEL_0(32),
    LEVEL_1(16),
    LEVEL_2(8);
    // LEVEL_3(4);

    public final int sampleSize;

    SampleLevel(int sampleSize) {
        this.sampleSize = sampleSize;
    }
}