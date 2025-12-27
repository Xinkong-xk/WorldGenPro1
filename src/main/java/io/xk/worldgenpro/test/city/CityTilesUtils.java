package io.xk.worldgenpro.test.city;

import io.xk.worldgenpro.generator.structure.core.LayerManager;
import io.xk.worldgenpro.generator.structure.core.api.Dir;
import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureTile;
import io.xk.worldgenpro.generator.structure.core.generators.simplewfc.SimpleWaveFunctionCollapseLayer;
import io.xk.worldgenpro.generator.structure.core.generators.simplewfc.WFCPreprocessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static io.xk.worldgenpro.generator.structure.core.api.Dir.opposite;

public class CityTilesUtils {
    public static void prepare(LayerManager manager, BlockPos startPos, ObjectArrayList<StructureTile> structureTiles,int size,int width,int height) {

        SimpleWaveFunctionCollapseLayer test_1 = new SimpleWaveFunctionCollapseLayer(size, width, height,startPos);

        test_1.setTiles(structureTiles);

        StructureTile areaTile = structureTiles.stream().filter(tile -> tile.connections.size() == 0).findFirst().orElse(null);

        test_1.registerExtraConstraint((t1, t2, d) -> {
            StructureTile tile1 = t1.getTile();
            StructureTile tile2 = t2.getTile();

            boolean c1 = tile1.connections.contains(d);
            if (c1 && (tile1 == areaTile || tile2 == areaTile)) return false;
            return true;
        });

        test_1.registerExtraConstraint((t1, t2, d) -> {
            StructureTile tile1 = t1.getTile();
            StructureTile tile2 = t2.getTile();

            boolean c1 = tile1.connections.contains(d);
            boolean c2 = tile2.connections.contains(opposite(d));
            return !(c1 && c2 && tile1.connections.size() >= 3 && tile2.connections.size() >= 3);
        });

        test_1.registerExtraConstraint((t1, t2, d) -> {
            Dir side1 = (d == Dir.UP || d == Dir.DOWN) ? Dir.LEFT : Dir.UP;
            Dir side2 = (d == Dir.UP || d == Dir.DOWN) ? Dir.RIGHT : Dir.DOWN;

            StructureTile tile1 = t1.getTile();
            StructureTile tile2 = t2.getTile();

            boolean t1Side1 = tile1.connections.contains(side1);
            boolean t1Side2 = tile1.connections.contains(side2);
            boolean t2Side1 = tile2.connections.contains(side1);
            boolean t2Side2 = tile2.connections.contains(side2);

            return !((t1Side1 && t2Side1) || (t1Side2 && t2Side2));
        });

        test_1.registerExtraConstraint((t1, t2, d) -> {
            StructureTile tile1 = t1.getTile();
            StructureTile tile2 = t2.getTile();

            // 取 t1 在 d 方向的连接情况，t2 在相反方向的连接情况
            boolean t1Connects = tile1.connections.contains(d);
            boolean t2Connects = tile2.connections.contains(d.opposite());

            int t1ConnCount = tile1.connections.size();
            int t2ConnCount = tile2.connections.size();

            // 检查是否都是转角瓦片（连接数正好为 2）
            boolean t1IsCorner = t1ConnCount == 2;
            boolean t2IsCorner = t2ConnCount == 2;

            if (t1IsCorner && t2IsCorner) {
                // 情况 1: 允许只有当它们的连接方向对上
                // 否则一律禁止
                return t1Connects && t2Connects;
            }

            // 如果不是两个都是角落，就不做限制，返回 true
            return true;
        });

        manager.addLayer(test_1);
        // preprocess

        test_1.initWFC(new Random(12));

        WFCPreprocessor preloader = new WFCPreprocessor(50, 50, structureTiles);
        preloader
                .circle(15, 15, 10);
        preloader.build();

        preloader.bindWFC(test_1.getGeneratorInstance());

        preloader.load();
    }
}
