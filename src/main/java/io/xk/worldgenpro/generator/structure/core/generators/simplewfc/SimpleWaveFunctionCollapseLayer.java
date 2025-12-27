package io.xk.worldgenpro.generator.structure.core.generators.simplewfc;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.generator.structure.core.api.*;
import io.xk.worldgenpro.generator.structure.core.api.wfc.*;
import io.xk.worldgenpro.generator.structure.core.base.BaseLayer;
import io.xk.worldgenpro.generator.structure.core.api.Dir;
import io.xk.worldgenpro.utils.DynamicConsumerExecutor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static io.xk.worldgenpro.generator.structure.core.api.Dir.opposite;

public class SimpleWaveFunctionCollapseLayer extends BaseLayer<WFCGenerator, StructureTile, StructureCell> {

    /**
     * 使用 StructureCell 版本的特殊规则管理器
     */
    private final SpecialRuleManager specialRuleManager = new SpecialRuleManager();

    /**
     * 现在是 StructureCell 版本的规则存储
     */
    public AdjacencyRules rules;

    public ObjectArrayList<ExtraConstraint> extraConstraints = new ObjectArrayList<>();

    public SimpleWaveFunctionCollapseLayer(int size, int width, int height, BlockPos startPos) {
        super(size, width, height, startPos);
    }

    /**
     * 仍然允许外部注册基于 StructureCell 的约束
     */
    public void registerExtraConstraint(ExtraConstraint extraConstraint) {
        extraConstraints.add(extraConstraint);
    }

    /**
     * 改为注册 CellConsumer 而不是 TConsumer
     */
    public void registerSpecialRule(String tileId, FConsumer action) {
        specialRuleManager.registerRule(tileId, action);
    }

    /**
     * 自动生成邻接规则。
     * 这里仍然基于 Tile 的连接信息生成，但是最终存储的是 tile.id，
     * 以便 AdjacencyRules 能正确匹配 StructureCell。
     */
    static void autoGenerateRules(ObjectArrayList<? extends Tile> tiles, AdjacencyRules rules) {
        for (Tile t1 : tiles) {
            if (!(t1 instanceof StructureTile st1)) continue; // 只处理 StructureTile
            for (Dir d : Dir.values()) {
                for (Tile t2 : tiles) {
                    if (!(t2 instanceof StructureTile st2)) continue;

                    Dir oppositeDir = opposite(d);

                    // 直接用 connections 判断
                    boolean c1 = st1.connections.contains(d);
                    boolean c2 = st2.connections.contains(oppositeDir);
                    if (c1 != c2) continue;

                    // 用 StructureCell 包装 Tile 检查额外约束
                    StructureCell c1Cell = new StructureCell(st1);
                    StructureCell c2Cell = new StructureCell(st2);
                    if (rules.checkExtraConstraints(c1Cell, c2Cell, d)) {
                        rules.allow(st1, d, st2);
                    }
                }
            }
        }
    }

    public void initWFC(Random random) {
        rules = new AdjacencyRules();
        for (ExtraConstraint extraConstraint : extraConstraints) {
            rules.registerConstraint(extraConstraint);
        }
        for (Tile t : tiles) rules.addTile(t); // 现在存 StructureCell 版本

        autoGenerateRules(tiles, rules);

        generator = new WFCGenerator(width, height, startPos, size, tiles, rules, mask, random, specialRuleManager);
    }

    public void generate(Random random, int maxAttempts, boolean useBacktracking, ServerWorld serverWorld) {
        if (generator == null) {
            initWFC(random);
        }

        boolean success = generator.generate(maxAttempts, useBacktracking);

        if (success) {
            System.out.println("WFC层生成完成,准备生成...");

            serverWorld.getServer().execute(() -> {
                generator.placeCells(serverWorld);
            });

            System.out.println("生成完成");
        } else {
            System.out.println("WFC层生成部分完成，失败的格子已标记为红色");
        }
    }

    @Override
    public void inputCells(StructureCell[][] tiles) {
    }

    @Override
    public StructureCell[][] outputCells() {
        return generator != null ? generator.getResult() : new StructureCell[0][];
    }

    @Override
    public void generate(Random random, ServerWorld serverWorld) {
        generate(random, 50, false, serverWorld);
    }
}
