package io.xk.worldgenpro.generator.structure.core.api.wfc;

import io.xk.worldgenpro.generator.structure.core.api.Cell;
import io.xk.worldgenpro.generator.structure.core.api.FConsumer;
import io.xk.worldgenpro.generator.structure.core.base.wfc.BaseWFCGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * 特殊生成规则管理器，改为基于 Cell。
 */
public class SpecialRuleManager {
    // 存储 Cell.tile.id 与对应操作的映射
    private final Map<String, FConsumer<Cell,Integer, Integer, BaseWFCGenerator>> cellRules = new HashMap<>();

    /**
     * 注册特定 Cell 的触发规则
     */
    public void registerRule(String tileId, FConsumer<Cell,Integer, Integer, BaseWFCGenerator> action) {
        cellRules.put(tileId, action);
    }

    /**
     * 触发规则
     */
    public void triggerRule(Cell cell, int x, int y, BaseWFCGenerator wfc) {
        if (cell == null || cell.getTile() == null || cell.getTile().id == null) return;
        cellRules.getOrDefault(cell.getTile().id, (c, x1, y1, wfc1) -> {}).accept(cell, x, y, wfc);
    }
}
