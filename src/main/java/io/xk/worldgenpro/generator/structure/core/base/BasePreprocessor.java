package io.xk.worldgenpro.generator.structure.core.base;

import io.xk.worldgenpro.generator.structure.core.api.Tile;

import java.util.Arrays;

/**
 * 最小预处理基类 —— 只负责记录并把预放置瓦片写入 BaseWFC。
 *
 * 说明：
 * - 本类**不**做智能放置、道路识别或 propagate 等额外行为。
 * - 子类应实现 {@link #build()} 来填充 preplaced 数组（或直接使用 setPreplaced）。
 */
public abstract class BasePreprocessor<T extends BasePreprocessor<T>> {
    protected final int width;
    protected final int height;
    // 稀疏网格，null 表示无预放置瓦片
    protected final Tile[][] preplaced;

    public BasePreprocessor(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("width/height must be > 0");
        this.width = width;
        this.height = height;
        this.preplaced = new Tile[width][height];
        for (int x = 0; x < width; x++) Arrays.fill(this.preplaced[x], null);
    }

    /** 子类实现：在此填充 preplaced 数组（可以调用 setPreplaced），或留空表示不做任何预放置 */
    public abstract T build();

    /** 设置某格的预放置瓦片（tile == null 表示清除该格） */
    public void setPreplaced(int x, int y, Tile tile) {
        if (!inBounds(x, y)) return;
        preplaced[x][y] = tile;
    }

    /** 获取某格的预放置瓦片（可能为 null） */
    public Tile getPreplaced(int x, int y) {
        if (!inBounds(x, y)) return null;
        return preplaced[x][y];
    }

    /** 清空所有预放置记录 */
    public void clear() {
        for (int x = 0; x < width; x++) Arrays.fill(preplaced[x], null);
    }

    /** 导出 preplaced 的深拷贝（安全给外部读取或持久化） */
    public Tile[][] exportCopy() {
        Tile[][] copy = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            System.arraycopy(preplaced[x], 0, copy[x], 0, height);
        }
        return copy;
    }

    /**
     * 将当前预放置记录写入目标 BaseWFC（只调用 BaseWFC.addPreplacedTile）
     *
     * 注意：此方法**不**修改 wfc.possibilities，也**不**触发 wfc.propagateFrom。
     * 如果需要立刻生效，请在调用方在适当时机手动调用 wfc.resetPossibilities() 或 wfc.propagateFrom(...)。
     */
    public void applyTo(BaseGenerator generator) {
        if (generator == null) throw new IllegalArgumentException("wfc == null");
        for (int x = 0; x < Math.min(width, generator.width); x++) {
            for (int y = 0; y < Math.min(height, generator.height); y++) {
                Tile t = preplaced[x][y];
                if (t != null) {
                    generator.addPreplacedTile(x, y, t);
                }
            }
        }
    }

    protected boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
