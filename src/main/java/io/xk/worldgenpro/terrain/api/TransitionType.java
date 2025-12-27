package io.xk.worldgenpro.terrain.api;

/**
 * 定义 FlattenRegion 的过渡类型。
 * 每种类型提供独立的衰减曲线函数。
 */
public enum TransitionType {
    LINEAR {
        @Override
        public double factor(double distance, double radius) {
            if (distance >= radius) return 0;
            return 1.0 - (distance / radius);
        }
    },
    SMOOTH {
        @Override
        public double factor(double distance, double radius) {
            if (distance >= radius) return 0;
            double t = 1.0 - (distance / radius);
            // cosine 插值: 平滑但边缘不陡
            return 0.5 - 0.5 * Math.cos(t * Math.PI);
        }
    },
    INSTANT {
        @Override
        public double factor(double distance, double radius) {
            // 只要在半径范围内都立即应用
            return distance < radius ? 1.0 : 0.0;
        }
    },
    CUSTOM {
        @Override
        public double factor(double distance, double radius) {
            // 可自定义策略，这里先给线性占位
            if (distance >= radius) return 0;
            return 1.0 - (distance / radius);
        }
    };

    /**
     * 计算该过渡类型在指定距离下的强度（0~1）。
     * @param distance 距离平整区边缘的距离
     * @param radius 过渡半径
     * @return 强度值（0~1）
     */
    public abstract double factor(double distance, double radius);
}
