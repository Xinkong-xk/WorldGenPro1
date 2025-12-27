package io.xk.worldgenpro.generator.structure.core.base;


import io.xk.worldgenpro.generator.structure.core.api.Cell;
import io.xk.worldgenpro.generator.structure.core.api.Tile;
import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureCell;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Random;

/**
 * 通用生成器基类：提供最基础的网格、瓦片、预放置管理能力
 * <p>
 * 该类不包含任何 WFC 逻辑，也不处理可能性列表、传播约束、完成判定等。
 * WFC、PerlinNoise 等具体算法都可以继承此类，基于此类提供的网格和预放置功能实现各自的生成逻辑。
 */
public abstract class BaseGenerator<T extends Tile>{
    public final int width;
    public final int height;

    /** 可供生成使用的全部瓦片集合 */
    public final ObjectArrayList<T> generateTiles;

    public BlockPos startPos;

    /** 每个格子的尺寸（可选） */
    public int size;

    /** 随机数生成器，可被子类使用 */
    public final Random rnd;

    /** 掩膜，控制哪些格子可被生成 */
    public final boolean[][] mask;

    /** 预放置瓦片记录（二维数组，null 表示未预放置） */
    protected final Cell[][] preplacedTiles;

    public BaseGenerator(int width, int height,
                         BlockPos startPos,
                         int size,
                         ObjectArrayList<T> generateTiles,
                         boolean[][] mask,
                         Random rnd) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height must be > 0");
        }
        this.width = width;
        this.height = height;
        this.startPos = startPos;
        this.size = size;
        this.generateTiles = generateTiles != null ? generateTiles : new ObjectArrayList<>();
        this.rnd = rnd != null ? rnd : new Random();
        this.mask = mask != null ? mask : createFullMask(width, height);
        this.preplacedTiles = new Cell[width][height];
    }

    /** 默认创建一个全 true 的掩膜 */
    private boolean[][] createFullMask(int w, int h) {
        boolean[][] m = new boolean[w][h];
        for (int x = 0; x < w; x++) Arrays.fill(m[x], true);
        return m;
    }

    /* ----------------------- 预放置 API ----------------------- */

    /** 添加或更新一个预放置瓦片 */
    public void addPreplacedTile(int x, int y, T tile) {
        if (inBounds(x, y) && mask[x][y]) {
            preplacedTiles[x][y] = new Cell<T>(tile) ;
        }
    }

    /** 获取预放置瓦片（可能为 null） */
    public T getPreplacedTile(int x, int y) {
        return inBounds(x, y) ? (T) preplacedTiles[x][y].getTile() : null;
    }

    public Cell getPreplacedCell(int x, int y) {
        return inBounds(x, y) ? preplacedTiles[x][y] : null;
    }

    /** 清空所有预放置瓦片 */
    public void clearPreplacedTiles() {
        for (int x = 0; x < width; x++) {
            Arrays.fill(preplacedTiles[x], null);
        }
    }

    /** 导出预放置瓦片的深拷贝 */
    public Cell[][] exportPreplacedCopy() {
        Cell[][] copy = new Cell[width][height];
        for (int x = 0; x < width; x++) {
            System.arraycopy(preplacedTiles[x], 0, copy[x], 0, height);
        }
        return copy;
    }

    /* ----------------------- 通用工具 ----------------------- */

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /* ----------------------- 抽象方法 ----------------------- */

    /**
     * 子类实现具体的生成逻辑
     */
    public abstract void generate();

    /**
     * 子类可实现导出最终结果的方法（比如 WFC 会输出唯一瓦片的二维数组）
     */
    public abstract StructureCell[][] getResult();

    public void placeCells(ServerWorld serverWorld) {
        StructureCell[][] result = getResult();

        int y = startPos.getY() - 1;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                StructureCell cell = result[x][z];
                if (cell != null) {
                    int px = startPos.getX() + x * size;
                    int pz = startPos.getZ() + z * size;
                    cell.place(size,px,y, pz,startPos,serverWorld);
                }
            }
        }
    }
}
