package io.xk.worldgenpro.terrain.api;

import java.util.Arrays;

/**
 * FlattenRegion:
 * 表示一个带有平整与过渡区域控制的地形区域。
 * 支持开发者自定义 flattenArea 并自动扩展平滑过渡区域。
 */
public class FlattenRegion {

    public int adaptRadius;
    public int x;
    public int z;
    public TransitionType transitionType;
    public int[] sampleArea;
    public int targetY;
    public YProvider yProvider;
    public double[][] flattenArea;

    // 在 FlattenRegion 类中新增一个字段：
    /** 将高度差转换为 density 偏移的缩放因子。越大表示同样的高度差带来越小的 density 偏移。 */
    public double heightToDensityScale = 24.0; // 经验值应该在24左右最好

    public FlattenRegion(int x, int z, double[][] flattenArea, int targetY,
                         int adaptRadius, TransitionType transitionType, YProvider yProvider) {
        this.x = x;
        this.z = z;
        this.flattenArea = flattenArea;
        this.targetY = targetY;
        this.adaptRadius = adaptRadius;
        this.transitionType = transitionType;
        this.yProvider = yProvider;

        buildSampleArea();
        buildFlattenArea();
    }

    /** 根据 flattenArea 和 adaptRadius 生成采样边界 */
    public void buildSampleArea() {
        int width = flattenArea.length;
        int height = flattenArea[0].length;
        this.sampleArea = new int[]{
                x - adaptRadius,
                x + width + adaptRadius - 1,
                z - adaptRadius,
                z + height + adaptRadius - 1
        };
        this.x -= adaptRadius;
        this.z -= adaptRadius;
    }

    /**
     * 使用二维欧几里得距离场算法（Distance Transform）高效生成过渡区域。
     * 内部非零区域保持原值，外部根据最近非零距离按 TransitionType 曲线衰减。
     */
    public void buildFlattenArea() {
        int origW = flattenArea.length;
        int origH = flattenArea[0].length;
        int newW = origW + adaptRadius * 2;
        int newH = origH + adaptRadius * 2;

        // 新数组，初始化为0
        double[][] newArea = new double[newW][newH];

        // 将原区域复制到中心
        for (int i = 0; i < origW; i++) {
            System.arraycopy(flattenArea[i], 0, newArea[i + adaptRadius], adaptRadius, origH);
        }

        // 距离场数组（单位：格）
        double[][] dist = new double[newW][newH];

        // Step 1: 初始化距离场——非零点为0，零点为正无穷
        for (int x = 0; x < newW; x++) {
            for (int z = 0; z < newH; z++) {
                dist[x][z] = newArea[x][z] > 0 ? 0.0 : Double.POSITIVE_INFINITY;
            }
        }

        // Step 2: 执行二维距离变换（欧几里得近似）——4邻域 + 反向扫描
        for (int x = 0; x < newW; x++) {
            for (int z = 0; z < newH; z++) {
                double min = dist[x][z];
                if (x > 0) min = Math.min(min, dist[x - 1][z] + 1);
                if (z > 0) min = Math.min(min, dist[x][z - 1] + 1);
                if (x > 0 && z > 0) min = Math.min(min, dist[x - 1][z - 1] + Math.sqrt(2));
                dist[x][z] = min;
            }
        }
        for (int x = newW - 1; x >= 0; x--) {
            for (int z = newH - 1; z >= 0; z--) {
                double min = dist[x][z];
                if (x < newW - 1) min = Math.min(min, dist[x + 1][z] + 1);
                if (z < newH - 1) min = Math.min(min, dist[x][z + 1] + 1);
                if (x < newW - 1 && z < newH - 1) min = Math.min(min, dist[x + 1][z + 1] + Math.sqrt(2));
                dist[x][z] = min;
            }
        }

        // Step 3: 根据距离和 TransitionType 计算过渡权重
        for (int x = 0; x < newW; x++) {
            for (int z = 0; z < newH; z++) {
                double base = newArea[x][z];
                if (base > 0) continue; // 原区域保持不变

                double d = dist[x][z];
                if (d > adaptRadius) {
                    newArea[x][z] = 0;
                } else {
                    // 查找最近边界点的原始强度（近似：反推d格内中心）
                    int nx = Math.max(adaptRadius, Math.min(origW + adaptRadius - 1, x));
                    int nz = Math.max(adaptRadius, Math.min(origH + adaptRadius - 1, z));
                    double nearestVal = flattenArea[nx - adaptRadius][nz - adaptRadius];
                    double f = transitionType.factor(d, adaptRadius);
                    newArea[x][z] = nearestVal * f;
                }
            }
        }

        this.flattenArea = newArea;
    }


    public double sample(int x, int y, int z, double parentSample) {
        // 直接假设调用方已保证 (x,z) 在本区域范围内（你之前说会外部判断）
        int localX = x - this.x;
        int localZ = z - this.z;

        // 读取预计算好的影响力（0..1）
        double influence = flattenArea[localX][localZ];
        if (influence <= 0.0) return parentSample; // 理论上外部已经保证不会进来，但保险检查一下

        // 目标高度：支持 YProvider 或固定 targetY
        double targetYValue = (this.yProvider != null)
                ? this.yProvider.sampleY(x, y, z)
                : this.targetY;

        // 计算高度差（目标高度 - 当前方块高度）
        double dy = targetYValue - (double) y;

        // 把高度差映射为 density 偏移量（经验比例）
        double offsetDensity = (dy / this.heightToDensityScale) * influence;

        // 将偏移量加入原始密度并返回
        return parentSample + offsetDensity;
    }


    public int[] getSampleArea() {
        if (this.sampleArea == null) buildSampleArea();
        return this.sampleArea;
    }

    @FunctionalInterface
    public interface YProvider {
        double sampleY(int x, int y, int z);
    }
}
