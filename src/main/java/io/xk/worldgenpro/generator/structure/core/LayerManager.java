package io.xk.worldgenpro.generator.structure.core;

import io.xk.worldgenpro.generator.structure.core.api.Cell;
import io.xk.worldgenpro.generator.structure.core.base.BaseLayer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;
import java.util.function.BiConsumer;

public class LayerManager {

    public static int[][][] pixels;
    public Random random;
    public int width;
    public int height;
    public BlockPos startPos;
    public ObjectArrayList<BaseLayer> baseLayerList = new ObjectArrayList<>();

    public LayerManager(int width, int height, long seed, BlockPos startPos) {
        this.width = width;
        this.height = height;
        this.startPos = startPos;
        random = new Random(seed);
    }

    public void addLayer(BaseLayer baseLayer) {
        int index = baseLayer.getExceptLayerIndex();
        if (index < 0 || index >= baseLayerList.size()) {
            baseLayerList.add(baseLayer);
        } else {
            baseLayerList.add(index, baseLayer);
        }
    }

    public void generate(ServerWorld serverWorld) {
        Cell[][] currentCells = null;
        for (BaseLayer baseLayer : baseLayerList) {
            baseLayer.inputCells(currentCells);// 如果有上一层的输出，就传入

            baseLayer.generate(random,serverWorld);// 生成（解耦后的WFC用自己的方法）

            currentCells = baseLayer.outputCells();// 输出当前层的结果
        }
    }

    public void start(ServerWorld serverWorld, BiConsumer<LayerManager, BlockPos> prepareTask) {
        LayerManager manager = new LayerManager(480, 480, 2,this.startPos);

        // 加载测试
        prepareTask.accept(manager,this.startPos);

        // 开始生成
        manager.generate(serverWorld);

    }

}
