package io.xk.worldgenpro.generator.structure.core.generators.simplewfc;

import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureCell;
import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureTile;
import io.xk.worldgenpro.generator.structure.core.base.BasePreprocessor;
import io.xk.worldgenpro.generator.structure.core.base.wfc.BaseWFCGenerator;
import io.xk.worldgenpro.generator.structure.core.api.Dir;
import io.xk.worldgenpro.generator.structure.core.preprocess.wfc.DrawOperation;
import io.xk.worldgenpro.generator.structure.core.preprocess.wfc.operations.CircleOperation;
import io.xk.worldgenpro.generator.structure.core.preprocess.wfc.operations.LineOperation;
import io.xk.worldgenpro.generator.structure.core.preprocess.wfc.operations.RectOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;

/**
 * MapPreloader（改进版，适配 BaseWFC）
 */
public class WFCPreprocessor extends BasePreprocessor<WFCPreprocessor> {

    private final StructureTile[][] tiles;
    private final List<DrawOperation> operations = new ArrayList<>();
    private final Map<EnumSet<Dir>, StructureTile> roadMap = new HashMap<>();

    public StructureTile areaTile = null;

    /** 引用 BaseWFC（可延迟绑定） */
    private BaseWFCGenerator wfc;
    private StructureTile[][] result = null;

    /**
     * @param width 地图宽度
     * @param height 地图高度
     * @param allTiles 传入所有 tiles（通常是 BaseWFC.tiles）
     */
    public WFCPreprocessor(int width, int height, ObjectArrayList<StructureTile> allTiles) {
        super(width, height);

        this.tiles = new StructureTile[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                tiles[x][y] = null;

        if (allTiles != null) {
            initByTag(allTiles);
        } else {
            System.out.println("Warning: allTiles is null when initializing MapPreloader.");
        }
    }

    /** 绑定一个 WFC 实例（可多次调用绑定不同的） */
    public WFCPreprocessor bindWFC(BaseWFCGenerator wfc) {
        this.wfc = wfc;
        return this;
    }

    /** 将 build 结果写入当前绑定的 WFC 作为预放置 */
    public void load() {
        if (wfc == null) throw new IllegalStateException("WFC is null — call bindWFC() before load().");
        if (result == null) {
            System.out.println("MapPreloader.load(): no result to load (build() not called or empty).");
            return;
        }

        int placed = 0;
        for (int x = 0; x < Math.min(wfc.width, result.length); x++) {
            for (int y = 0; y < Math.min(wfc.height, result[0].length); y++) {
                StructureTile t = result[x][y];
                if (t != null && set(x, y, t, wfc)) {
                    placed++;
                }
            }
        }
        System.out.println("MapPreloader.load(): total placed = " + placed);
    }

    /** 将另一个 StructureTile[][] 写入当前绑定的 WFC */
    public void load(StructureTile[][] otherResult, BaseWFCGenerator otherWfc) {
        if (otherResult == null) return;
        if (wfc == null) throw new IllegalStateException("WFC is null — call bindWFC() before load().");

        int placed = 0;
        int maxX = Math.min(otherWfc.width, wfc.width);
        int maxY = Math.min(otherWfc.height, wfc.height);
        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                StructureTile t = otherResult[x][y];
                if (t != null && set(x, y, t, wfc)) {
                    placed++;
                }
            }
        }
        System.out.println("MapPreloader.load(otherResult): total placed = " + placed);
    }

    private void initByTag(ObjectArrayList<StructureTile> allTiles) {
        for (StructureTile t : allTiles) {
            if (t.connections.size() == 0){
                areaTile =  t;
                break;
            }
        }

        for (StructureTile tile : allTiles) {
            if (tile == areaTile) continue; // 跳过占位区域块
            if (!(tile instanceof StructureTile st)) continue; // 只处理 StructureTile
            roadMap.putIfAbsent(EnumSet.copyOf(st.connections), st);
        }
        System.out.println("MapPreloader.initByTag: roadMap size = " + roadMap.size());
    }


    public WFCPreprocessor line(int x1, int y1, int x2, int y2) {
        operations.add(new LineOperation(x1, y1, x2, y2));
        return this;
    }

    public WFCPreprocessor rect(int x1, int y1, int x2, int y2) {
        operations.add(new RectOperation(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2)));
        return this;
    }

    public WFCPreprocessor circle(int cx, int cy, int r) {
        operations.add(new CircleOperation(cx, cy, r));
        return this;
    }

    private boolean[][] mask;

    @Override
    public WFCPreprocessor build() {
        mask = new boolean[width][height];
        for (DrawOperation op : operations) op.draw(mask);

        int missing = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!mask[x][y]) { tiles[x][y] = null; continue; }

                EnumSet<Dir> connections = EnumSet.noneOf(Dir.class);
                if (inRoad(mask, x, y - 1)) connections.add(Dir.UP);
                if (inRoad(mask, x + 1, y)) connections.add(Dir.RIGHT);
                if (inRoad(mask, x, y + 1)) connections.add(Dir.DOWN);
                if (inRoad(mask, x - 1, y)) connections.add(Dir.LEFT);
                if (connections.size() == 1) connections.add(opposite(connections.iterator().next()));

                StructureTile chosen = roadMap.get(connections);
                if (chosen == null) {
                    System.out.println("MapPreloader.build(): no road tile for " + connections + " at (" + x + "," + y + ")");
                    missing++;
                    tiles[x][y] = null;
                } else {
                    tiles[x][y] = chosen;
                }
            }
        }
        if (missing > 0) System.out.println("MapPreloader.build(): missing tiles for " + missing + " cells.");
        this.result = tiles;
        return this;
    }

    private Dir opposite(Dir d) {
        switch (d) {
            case UP: return Dir.DOWN;
            case DOWN: return Dir.UP;
            case LEFT: return Dir.RIGHT;
            case RIGHT: return Dir.LEFT;
        }
        throw new IllegalArgumentException("Unknown dir: " + d);
    }

    private boolean set(int x, int y, StructureTile tile, BaseWFCGenerator wfc) {
        if (!wfc.inBounds(x, y) || !wfc.mask[x][y]) return false;
        if (!wfc.generateTiles.contains(tile)) return false;

        wfc.addPreplacedTile(x, y, tile);
        wfc.possibilities[x][y].clear();
        wfc.possibilities[x][y].add(new StructureCell(tile));
        return wfc.propagateFrom(x, y);
    }

    private boolean inRoad(boolean[][] mask, int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && mask[x][y];
    }

    public StructureTile[][] getResult() { return result; }
    public boolean[][] getMask() { return mask; }
}
