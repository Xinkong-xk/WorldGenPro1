package io.xk.worldgenpro.generator.structure.core.generators.simplewfc;


import io.xk.worldgenpro.generator.structure.core.api.wfc.AdjacencyRules;
import io.xk.worldgenpro.generator.structure.core.api.wfc.SpecialRuleManager;
import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureCell;
import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureTile;
import io.xk.worldgenpro.generator.structure.core.base.wfc.BaseWFCGenerator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class WFCGenerator extends BaseWFCGenerator {
    private final SpecialRuleManager ruleManager;
    private final ObjectArrayList<ObjectArrayList<StructureCell>[][]> statesStack = new ObjectArrayList<>();

    public WFCGenerator(
            int width, int height,
                        BlockPos startPos,
                        int size,
                        ObjectArrayList<StructureTile> generateTiles,
                        AdjacencyRules rules,
                        boolean[][] mask,
                        Random rnd, SpecialRuleManager ruleManager
    ) {
        super(width, height, startPos, size, generateTiles, rules, mask, rnd);
        this.ruleManager = ruleManager;
    }

    // 修改瓦片选择逻辑，在确定瓦片后触发规则
    public boolean runOnceWithWeightedChoice() {
        resetPossibilities();
        failedCells.clear();
        int step = 0;
        int totalSteps = 0;

        // 计算总步数
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (mask[x][y] && possibilities[x][y].size() > 1) {
                    totalSteps++;
                }
            }
        }

        while (!isComplete()) {
            int[] cell = chooseCellWithMinEntropy();
            if (cell == null) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        if (mask[x][y] && possibilities[x][y].size() > 1) {
                            failedCells.add(x + "," + y);
                        }
                    }
                }
                break;
            }

            int cx = cell[0], cy = cell[1];
            StructureCell chosen = weightedRandomChoice(possibilities[cx][cy]);

            possibilities[cx][cy].clear();
            possibilities[cx][cy].add(chosen);

            ruleManager.triggerRule(chosen, cx, cy, this);

            if (!propagateFrom(cx, cy)) {
                System.err.println("error at : "+cx+" , "+cy);
                failedCells.add(cx + "," + cy);
                break;
            }

            step++;
            if (totalSteps > 0 && step * 10 % totalSteps == 0) {
                int progress = step * 100 / totalSteps;
                System.out.println("生成进度: " + progress + "% (" + step + " / " + totalSteps + ")");
            }
        }
        return true;
    }

    public boolean runWithBacktracking() {
        if (isComplete()) return true;

        int[] cell = chooseCellWithMinEntropy();
        if (cell == null) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (mask[x][y] && possibilities[x][y].size() > 1) {
                        failedCells.add(x + "," + y);
                    }
                }
            }
            return false;
        }

        int cx = cell[0], cy = cell[1];
        ObjectArrayList<StructureCell> candidates = new ObjectArrayList<>(possibilities[cx][cy]);
        Collections.shuffle(candidates, rnd);

        for (StructureCell chosen : candidates) {
            statesStack.add(copyPossibilitiesDeep());
            possibilities[cx][cy].clear();
            possibilities[cx][cy].add(chosen);

            ruleManager.triggerRule(chosen, cx, cy, this);

            if (propagateFrom(cx, cy)) {
                if (runWithBacktracking()) {
                    return true;
                }
            }

            restorePossibilitiesDeep(statesStack.remove(statesStack.size() - 1));
        }

        failedCells.add(cx + "," + cy);
        return false;
    }

    @SuppressWarnings("unchecked")
    private ObjectArrayList<StructureCell>[][] copyPossibilitiesDeep() {
        ObjectArrayList<StructureCell>[][] copy = new ObjectArrayList[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ObjectArrayList<StructureCell> src = possibilities[x][y];
                ObjectArrayList<StructureCell> newList = new ObjectArrayList<>(src.size());
                for (StructureCell c : src) {
                    StructureCell clone = new StructureCell(c.getTile());
                    // 复制 attributes（如果存在）
                    if (c.getAttributes() != null) {
                        it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<String, Object> copyAttrs =
                                new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
                        copyAttrs.putAll(c.getAttributes());
                        clone.setAttributes(copyAttrs);
                    }
                    newList.add(clone);
                }
                copy[x][y] = newList;
            }
        }
        return copy;
    }

    private void restorePossibilitiesDeep(ObjectArrayList<StructureCell>[][] state) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // 为避免共享对象再复制一遍（可选）
                ObjectArrayList<StructureCell> src = state[x][y];
                ObjectArrayList<StructureCell> newList = new ObjectArrayList<>(src.size());
                for (StructureCell c : src) {
                    StructureCell clone = new StructureCell(c.getTile());
                    if (c.getAttributes() != null) {
                        it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<String, Object> copyAttrs =
                                new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>();
                        copyAttrs.putAll(c.getAttributes());
                        clone.setAttributes(copyAttrs);
                    }
                    newList.add(clone);
                }
                possibilities[x][y] = newList;
            }
        }
    }

    private int[] chooseCellWithMinEntropy() {
        double bestEntropy = Double.POSITIVE_INFINITY;
        ObjectArrayList<int[]> bestCells = new ObjectArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!mask[x][y]) continue;
                ObjectArrayList<StructureCell> candidates = possibilities[x][y];
                int s = candidates.size();
                if (s <= 1) continue;
                double weightSum = 0.0;
                for (StructureCell t : candidates) weightSum += t.getTile().weight;
                if (weightSum == 0) continue;
                double entropy = 0.0;
                for (StructureCell t : candidates) {
                    double p = t.getTile().weight / weightSum;
                    entropy -= p * Math.log(p);
                }
                if (entropy < bestEntropy) {
                    bestEntropy = entropy;
                    bestCells.clear();
                    bestCells.add(new int[]{x, y});
                } else if (entropy == bestEntropy) {
                    bestCells.add(new int[]{x, y});
                }
            }
        }
        if (bestCells.isEmpty()) return null;
        return bestCells.get(rnd.nextInt(bestCells.size()));
    }

    private StructureCell weightedRandomChoice(ObjectArrayList<StructureCell> candidates) {
        double total = 0.0;
        for (StructureCell t : candidates) total += t.getTile().weight;
        double r = rnd.nextDouble() * total;
        double acc = 0.0;
        for (StructureCell t : candidates) {
            acc += t.getTile().weight;
            if (r <= acc) return t;
        }
        return candidates.get(candidates.size() - 1);
    }

    public boolean generate(int maxAttempts, boolean useBacktracking) {
        boolean success = false;
        for (int i = 0; i < maxAttempts; i++) {
            resetPossibilities();
            failedCells.clear();

            System.out.println("尝试生成第 " + (i + 1) + " 次");

            if (useBacktracking) {
                success = runWithBacktracking();
            } else {
                success = runOnceWithWeightedChoice();
            }

            if (success && isComplete()) {
                System.out.println("生成成功！所有格子都已确定。");
                return true;
            } else if (!failedCells.isEmpty()) {
                System.out.println("生成部分成功，有 " + failedCells.size() + " 个格子无法确定。");
                return false;
            }
        }
        System.out.println("在尝试了 " + maxAttempts + " 次后，生成失败。");
        return false;
    }

    @Override
    public void generate() {
        generate();
    }
}