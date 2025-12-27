package io.xk.worldgenpro.generator.structure.core.base;

import io.xk.worldgenpro.generator.structure.core.api.Cell;
import io.xk.worldgenpro.generator.structure.core.api.Tile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public abstract class BaseLayer<T extends BaseGenerator, U extends Tile, S extends Cell<U>> {
    public BlockPos startPos;
    public ObjectArrayList<U> tiles = new ObjectArrayList<>();
    public int size;
    public int width;
    public int height;
    public boolean[][] mask;

    public T generator; // 每一个层都必须显式持有一个生成器实例

    public T getGeneratorInstance() {
        return generator;
    }

    public void setGeneratorInstance(T generator) {
        this.generator = generator;
    }

    public ObjectArrayList<U> getTiles() {
        return tiles;
    }

    /**
     * 接收上一层的瓦片数组
     */
    public abstract void inputCells(S[][] tiles);

    /**
     * 返回当前层处理后的瓦片数组
     */
    public abstract S[][] outputCells();

    public int getExceptLayerIndex() {
        return -1;
    }

    public abstract void generate(Random random, ServerWorld serverWorld);

    public void setTiles(ObjectArrayList<U> newTiles) {
        this.tiles = newTiles;
    }

    public void registerTile(U tile) {
        this.tiles.add(tile);
    }

    public void setMask(boolean[][] newMask) {
        this.mask = newMask;
    }

    public void mask(int x, int y, boolean masked) {
        this.mask[x][y] = masked;
    }

    public BaseLayer(int size, int width, int height,BlockPos startPos) {
        this.mask = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.mask[x][y] = true;
            }
        }
        this.size = size;
        this.width = width;
        this.height = height;
        this.startPos = startPos;
    }
}
