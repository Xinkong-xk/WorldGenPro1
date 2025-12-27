package io.xk.worldgenpro.generator.structure.core.base.wfc;

import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureCell;
import io.xk.worldgenpro.generator.structure.core.api.Tile;
import io.xk.worldgenpro.generator.structure.core.api.wfc.AdjacencyRules;
import io.xk.worldgenpro.generator.structure.core.api.Dir;
import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureTile;
import io.xk.worldgenpro.generator.structure.core.base.BaseGenerator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * 基于 StructureCell 的 WFC 生成器
 */
public abstract class BaseWFCGenerator extends BaseGenerator<StructureTile> {

    /** 每个格子可能的 StructureCell 列表 */
    @SuppressWarnings("unchecked")
    public final ObjectArrayList<StructureCell>[][] possibilities;

    public final AdjacencyRules rules;

    // 失败的格子（仍然用字符串存，方便调试输出）
    public final Set<String> failedCells = new HashSet<>();

    @SuppressWarnings("unchecked")
    public BaseWFCGenerator(int width, int height,
                            BlockPos startPos,
                            int size,
                            ObjectArrayList<StructureTile> generateTiles,
                            AdjacencyRules rules,
                            boolean[][] mask,
                            Random rnd) {
        super(width, height, startPos, size, generateTiles, mask, rnd);

        this.rules = rules;
        possibilities = new ObjectArrayList[width][height];

        resetPossibilities();
    }

    @Override
    public void addPreplacedTile(int x, int y, StructureTile tile) {
        if (inBounds(x, y) && mask[x][y]) {
            preplacedTiles[x][y] = new StructureCell(tile) ;
        }
    }

    /** 重置所有可能性 */
    public void resetPossibilities() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (mask[x][y]) {
                    // 这里为每个候选 Tile 创建一个独立的 StructureCell 实例
                    ObjectArrayList<StructureCell> list = new ObjectArrayList<>(generateTiles.size());
                    for (StructureTile t : generateTiles) {
                        list.add(new StructureCell(t)); // 每个格子有独立的属性表
                    }
                    possibilities[x][y] = list;
                } else {
                    possibilities[x][y] = new ObjectArrayList<>();
                }
            }
        }
        // 恢复预放置瓦片
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                StructureCell preplaced = (StructureCell) preplacedTiles[x][y];
                if (preplaced != null && preplaced.getTile() != null && mask[x][y]) {
                    possibilities[x][y].clear();
                    possibilities[x][y].add(preplaced);
                    propagateFrom(x, y);
                }
            }
        }
        failedCells.clear();
    }

    /** 传播约束（修复版：以 Tile 身份判断允许性，不用新建 StructureCell 去比较） */
    public boolean propagateFrom(int startX, int startY) {
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{startX, startY});
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1];
            if (!mask[x][y]) continue;
            ObjectArrayList<StructureCell> curPoss = possibilities[x][y];

            for (Dir d : Dir.values()) {
                int nx = x + dx(d), ny = y + dy(d);
                if (!inBounds(nx, ny)) continue;
                if (!mask[nx][ny]) continue;

                ObjectArrayList<StructureCell> neighborPoss = possibilities[nx][ny];
                int before = neighborPoss.size();

                // 1) 收集 allowed Tile 集合（来自 rules.allowed(...)）
                ObjectOpenHashSet<Tile> allowedTiles =
                        new ObjectOpenHashSet<>();

                for (StructureCell c : curPoss) {
                    Collection<Tile> allowedForThis = rules.allowed(c, d);
                    if (allowedForThis != null && !allowedForThis.isEmpty()) {
                        allowedTiles.addAll(allowedForThis);
                    }
                }

                // 2) 用 allowedTiles 过滤 neighborPoss（保留 neighborCell.getTile() 在 allowedTiles 中的那些 StructureCell）
                ObjectArrayList<StructureCell> newPoss = new ObjectArrayList<>();
                for (StructureCell ncell : neighborPoss) {
                    if (allowedTiles.contains(ncell.getTile())) {
                        newPoss.add(ncell);
                    }
                }

                // 3) 判断结果
                if (newPoss.isEmpty()) return false;
                if (newPoss.size() < before) {
                    possibilities[nx][ny] = newPoss;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return true;
    }

    /** 根据当前 StructureCell 和方向，返回允许的邻居 StructureCell（包装了 Tile -> StructureCell） */
    private Collection<StructureCell> wrapAllowedCells(StructureCell c, Dir d) {
        ObjectArrayList<StructureCell> list = new ObjectArrayList<>();
        for (Tile allowedTile : rules.allowed(c.getTile(), d)) {
            list.add(new StructureCell((StructureTile) allowedTile)); // 每次生成独立 StructureCell
        }
        return list;
    }

    /** 判断是否完成 */
    public boolean isComplete() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (mask[x][y] && possibilities[x][y].size() != 1)
                    return false;
        return true;
    }

    /** 获取最终结果 */
    public StructureCell[][] getResult() {
        StructureCell[][] out = new StructureCell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out[x][y] = mask[x][y] && !possibilities[x][y].isEmpty()
                        ? possibilities[x][y].get(0)
                        : null;
            }
        }
        return out;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private int dx(Dir d) {
        return d == Dir.LEFT ? -1 : (d == Dir.RIGHT ? 1 : 0);
    }

    private int dy(Dir d) {
        return d == Dir.UP ? -1 : (d == Dir.DOWN ? 1 : 0);
    }
}
