package io.xk.worldgenpro.generator.structure.core.preprocess.wfc.operations;

import io.xk.worldgenpro.generator.structure.core.preprocess.wfc.DrawOperation;

public class RectOperation implements DrawOperation {
    private final int x1, y1, x2, y2;

    public RectOperation(int x1, int y1, int x2, int y2) {
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
    }

    @Override
    public void draw(boolean[][] mask) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);

        for (int x = minX; x <= maxX; x++) {
            mask[x][minY] = true;
            mask[x][maxY] = true;
        }
        for (int y = minY; y <= maxY; y++) {
            mask[minX][y] = true;
            mask[maxX][y] = true;
        }
    }
}