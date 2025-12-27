package io.xk.worldgenpro.terrain.adaptation;

import io.xk.worldgenpro.terrain.api.FlattenRegion;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

/**
 * TerrainAdaptorManager:
 * 管理多个 FlattenRegion 区域，并支持快速查询坐标所在区域。
 *
 * 特点：
 *  - 使用二维格网分桶法加速查询（按 X/Z 平面划分）
 *  - 不允许区域重叠（重叠时会抛出异常）
 *  - inside(x,z) 返回 FlattenRegion 或 null
 */
public class TerrainAdaptorManager {

    private final int gridSize;
    private final Map<Long, List<FlattenRegion>> grid = new ConcurrentHashMap<>(BASE_BIG_CAPACITY);

    public TerrainAdaptorManager(int gridSize) {
        this.gridSize = Math.max(1, gridSize);
    }

    /** 注册一批 FlattenRegion */
    public void addRegions(Collection<FlattenRegion> regions) {
        for (FlattenRegion region : regions) {
            addRegion(region);
        }
    }

    /** 注册单个 FlattenRegion */
    public void addRegion(FlattenRegion region) {
        int[] area = region.getSampleArea();
        int x1 = area[0];
        int x2 = area[1];
        int z1 = area[2];
        int z2 = area[3];

        int gx1 = Math.floorDiv(x1, gridSize);
        int gx2 = Math.floorDiv(x2, gridSize);
        int gz1 = Math.floorDiv(z1, gridSize);
        int gz2 = Math.floorDiv(z2, gridSize);

        // 检查是否与已有区域重叠
        for (int gx = gx1; gx <= gx2; gx++) {
            for (int gz = gz1; gz <= gz2; gz++) {
                long key = key(gx, gz);
                List<FlattenRegion> list = grid.get(key);
                if (list != null) {
                    for (FlattenRegion other : list) {
                        int[] oa = other.getSampleArea();
                        if (overlaps(x1, x2, z1, z2, oa[0], oa[1], oa[2], oa[3])) {
                            throw new IllegalStateException(
                                    "FlattenRegion overlap detected between " +
                                            region + " and " + other
                            );
                        }
                    }
                }
            }
        }

        // 无重叠，加入格网
        for (int gx = gx1; gx <= gx2; gx++) {
            for (int gz = gz1; gz <= gz2; gz++) {
                long key = key(gx, gz);
                grid.computeIfAbsent(key, k -> new ArrayList<>()).add(region);
            }
        }
    }

    /** 查询指定 (x,z) 所在区域；若无则返回 null */
    public FlattenRegion inside(int x, int z) {
        int gx = Math.floorDiv(x, gridSize);
        int gz = Math.floorDiv(z, gridSize);

        List<FlattenRegion> list = grid.get(key(gx, gz));
        if (list == null) return null;

        for (FlattenRegion region : list) {
            int[] a = region.getSampleArea();
            if (x >= a[0] && x <= a[1] && z >= a[2] && z <= a[3]) {
                return region;
            }
        }
        return null;
    }

    /** 清空 */
    public void clear() {
        grid.clear();
    }

    /** 桶数量 */
    public int getBucketCount() {
        return grid.size();
    }

    /** 生成格网 key */
    private static long key(int gx, int gz) {
        return (((long) gx) << 32) ^ (gz & 0xffffffffL);
    }

    /** 判断两个矩形是否重叠 */
    private static boolean overlaps(int x1a, int x2a, int z1a, int z2a,
                                    int x1b, int x2b, int z1b, int z2b) {
        return x1a <= x2b && x2a >= x1b && z1a <= z2b && z2a >= z1b;
    }
}
