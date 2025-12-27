package io.xk.worldgenpro.generator.structure.core.api.wfc;

import io.xk.worldgenpro.generator.structure.core.api.Cell;
import io.xk.worldgenpro.generator.structure.core.api.Dir;
import io.xk.worldgenpro.generator.structure.core.api.Tile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;

/**
 * 负责记录每个 Tile 允许的相邻 Tile 的规则
 * 同时支持基于 Cell 的额外约束检查
 */
public class AdjacencyRules {
    // 每个 Tile -> (方向 -> 可相邻的 Tile 集合)
    private final Map<Tile, Map<Dir, Set<Tile>>> map = new HashMap<>();
    private final ObjectArrayList<ExtraConstraint> extraConstraints = new ObjectArrayList<>();

    public AdjacencyRules() {}

    /** 添加一个 Tile 及其方向映射 */
    public void addTile(Tile t) {
        map.putIfAbsent(t, new EnumMap<>(Dir.class));
        for (Dir d : Dir.values()) {
            map.get(t).putIfAbsent(d, new HashSet<>());
        }
    }

    /** 注册额外约束（基于 Cell） */
    public void registerConstraint(ExtraConstraint constraint) {
        extraConstraints.add(constraint);
    }

    /** 检查两个 Cell 是否符合额外约束 */
    public boolean checkExtraConstraints(Cell c1, Cell c2, Dir d) {
        for (ExtraConstraint constraint : extraConstraints) {
            if (!constraint.allowConnection(c1, c2, d)) return false;
        }
        return true;
    }

    /** 允许 Tile 在某方向上与另一个 Tile 相邻 */
    public void allow(Tile t, Dir d, Tile neighbor) {
        addTile(t);
        addTile(neighbor);
        map.get(t).get(d).add(neighbor);
    }

    public void allow(Cell t, Dir d, Tile neighbor) {
        addTile(t.getTile());
        addTile(neighbor);
        map.get(t).get(d).add(neighbor);
    }

    public void allow(Cell t, Dir d, Cell neighbor) {
        addTile(t.getTile());
        addTile(neighbor.getTile());
        map.get(t).get(d).add(neighbor.getTile());
    }

    /** 查询 Tile 在某方向上的合法相邻 Tile 集合 */
    public Set<Tile> allowed(Tile t, Dir d) {
        return map.getOrDefault(t, Collections.emptyMap())
                .getOrDefault(d, Collections.emptySet());
    }

    public Set<Tile> allowed(Cell t, Dir d) {
        return map.getOrDefault(t.getTile(), Collections.emptyMap())
                .getOrDefault(d, Collections.emptySet());
    }

    /** 获取当前存储的 Tile 集合 */
    public Set<Tile> getTiles() {
        return map.keySet();
    }
}
