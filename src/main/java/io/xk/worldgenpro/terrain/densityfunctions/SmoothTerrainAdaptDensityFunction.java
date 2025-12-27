package io.xk.worldgenpro.terrain.densityfunctions;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.terrain.api.FlattenRegion;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;


public class SmoothTerrainAdaptDensityFunction implements DensityFunction {
    private final DensityFunction parent;

    public SmoothTerrainAdaptDensityFunction(DensityFunction parent) {
        this.parent = parent;
    }

    @Override
    public double sample(NoisePos pos) {
        // 1) 获取 parent density（原始密度）
        double original = parent.sample(pos);

        // 2) 快速判断是否在外面——避免不必要计算
        int x = pos.blockX();
        int y = pos.blockY();
        int z = pos.blockZ();

        FlattenRegion flattenRegion = WorldGenPro.terrainAdaptorManager.inside(x,z);
        if (flattenRegion != null ) {
            return flattenRegion.sample(x,y,z,original);
        }
        return original;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        // 当 world 在构造噪声时会对所有 DensityFunction 调用 apply(visitor)：
        // 我们需要确保 parent 也被 visitor 处理并包回去。
        return new SmoothTerrainAdaptDensityFunction(this.parent.apply(visitor));
    }

    @Override
    public double minValue() {
        return parent.minValue();
    }

    @Override
    public double maxValue() {
        return parent.maxValue();
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return null;
    }
}