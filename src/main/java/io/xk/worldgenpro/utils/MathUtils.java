package io.xk.worldgenpro.utils;

import net.minecraft.util.math.BlockPos;

public class MathUtils {
    // 三维切比雪夫距离
    public static int chebyshev3D(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }

    // 欧氏距离 (空间向量的模)
    public static double euclidean3D(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        int dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

}
