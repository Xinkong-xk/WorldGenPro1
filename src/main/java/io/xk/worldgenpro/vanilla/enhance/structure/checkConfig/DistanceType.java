package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig;

import io.xk.worldgenpro.utils.MathUtils;
import net.minecraft.util.math.BlockPos;

public enum DistanceType {
    Euclidean3D("euclidean3d"),
    Chebyshev3D("chebyshev3d");

    private final String name;
    DistanceType(String name){
        this.name = name;
    }

    public static double distance(DistanceType distanceType,BlockPos a, BlockPos b) {
        if (distanceType == DistanceType.Euclidean3D) {
            return MathUtils.euclidean3D(a, b);
        }else {
            return MathUtils.chebyshev3D(a, b);
        }
    }

    public static DistanceType fromName(String s) {
        for (DistanceType t : values()) {
            if (t.name.equalsIgnoreCase(s)) return t;
        }
        throw new IllegalArgumentException("Unknown type: " + s);
    }


    public String getName() {
        return name;
    }
}
