package io.xk.worldgenpro.generator.structure.core.preprocess.wfc.operations;

import io.xk.worldgenpro.generator.structure.core.preprocess.wfc.DrawOperation;

public class LineOperation implements DrawOperation {
    final int x1, y1, x2, y2;

    public LineOperation(int x1, int y1, int x2, int y2) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }

    @Override
    public void draw(boolean[][] mask) {
        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
        int dy = Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int x = x1, y = y1;
        while (true) {
            mask[x][y] = true;
            if (x == x2 && y == y2) break;

            int e2 = 2 * err;
            int nx = x, ny = y;

            if (e2 > -dy) { err -= dy; nx += sx; }
            if (e2 < dx)  { err += dx; ny += sy; }

            if (nx != x && ny != y) {
                mask[nx][y] = true; // 横向补
            }

            x = nx;
            y = ny;
        }
    }
}