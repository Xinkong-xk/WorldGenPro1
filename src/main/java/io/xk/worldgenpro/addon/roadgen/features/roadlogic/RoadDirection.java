package io.xk.worldgenpro.addon.roadgen.features.roadlogic;

public enum RoadDirection {

    X_AXIS,
    Z_AXIS,
    DIAGONAL_1,
    DIAGONAL_2;

    public static RoadDirection fromDelta(int dx, int dz) {
        if (dx < 0 && dz > 0 || dx > 0 && dz < 0){
            return RoadDirection.DIAGONAL_1;
        }
        else if (dx < 0 && dz < 0 || dx > 0 && dz > 0) {
            return RoadDirection.DIAGONAL_2;
        }
        else if (dx == 0 && dz != 0) {
            return RoadDirection.Z_AXIS;
        }
        return  RoadDirection.X_AXIS;
    }
}
