package io.xk.worldgenpro.generator.structure.core.preprocess.wfc.operations;

import io.xk.worldgenpro.generator.structure.core.preprocess.wfc.DrawOperation;

import java.util.HashSet;
import java.util.Set;

public class CircleOperation implements DrawOperation {
    private final int cx, cy, r; // 圆心和半径

    public CircleOperation(int cx, int cy, int r) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
    }

    @Override
    public void draw(boolean[][] mask) {
        Set<Point> circlePoints = generateCirclePoints();

        for (Point p : circlePoints) {
            if (in(mask, p.x, p.y)) {
                mask[p.x][p.y] = true;
            }
        }
    }

    /** 用中点圆算法生成 1/8 圆弧，然后对称扩展 */
    private Set<Point> generateCirclePoints() {
        Set<Point> points = new HashSet<>();

        int x = r;
        int y = 0;
        int d = 1 - r;

        while (x >= y) {
            // 八对称扩展
            addSymmetric(points, x, y);

            y++;
            if (d < 0) {
                d += 2 * y + 1;
            } else {
                x--;
                // 关键点,需要回退补一个格子
                addSymmetric(points, x, y-1);

                d += 2 * (y - x) + 1;
            }
        }

        return points;
    }

    /** 将 (x,y) 扩展到 8 个对称点 */
    private void addSymmetric(Set<Point> points, int x, int y) {
        points.add(new Point(cx + x, cy + y));
        points.add(new Point(cx - x, cy + y));
        points.add(new Point(cx + x, cy - y));
        points.add(new Point(cx - x, cy - y));

        points.add(new Point(cx + y, cy + x));
        points.add(new Point(cx - y, cy + x));
        points.add(new Point(cx + y, cy - x));
        points.add(new Point(cx - y, cy - x));
    }

    private boolean in(boolean[][] mask, int x, int y) {
        return x >= 0 && y >= 0 && x < mask.length && y < mask[0].length;
    }

    /** 简单的点类 */
    private static class Point {
        final int x, y;
        Point(int x, int y) {
            this.x = x; this.y = y;
        }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }
        @Override
        public int hashCode() {
            return x * 31 + y;
        }
    }
}
