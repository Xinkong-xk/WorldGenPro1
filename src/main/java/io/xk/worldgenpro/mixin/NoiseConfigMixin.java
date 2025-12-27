package io.xk.worldgenpro.mixin;

import io.xk.worldgenpro.terrain.densityfunctions.SmoothTerrainAdaptDensityFunction;

import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(NoiseConfig.class)
public class NoiseConfigMixin {

    @Inject(method = "getNoiseRouter", at = @At("RETURN"), cancellable = true)
    private void onGetNoiseRouter(CallbackInfoReturnable<NoiseRouter> cir) {
        NoiseRouter original = cir.getReturnValue();

        // === 这里决定你要修改的区域 / 参数来源 ===
        // 推荐不要硬编码，改为从你的配置/单例区域管理器读取
        // 示例用硬编码矩形（仅演示），生产要改成可配置/可注册的区域
        // 包装 finalDensity
        DensityFunction oldFinal = original.finalDensity();
        DensityFunction wrappedFinal = new SmoothTerrainAdaptDensityFunction(oldFinal);

        // 创建替代 NoiseRouter（其余字段原样复制）
        NoiseRouter modified = new NoiseRouter(
                original.barrierNoise(),
                original.fluidLevelFloodednessNoise(),
                original.fluidLevelSpreadNoise(),
                original.lavaNoise(),
                original.temperature(),
                original.vegetation(),
                original.continents(),
                original.erosion(),
                original.depth(),
                original.ridges(),
                original.initialDensityWithoutJaggedness(),
                wrappedFinal, // <--- 替换这里
                original.veinToggle(),
                original.veinRidged(),
                original.veinGap()
        );

        cir.setReturnValue(modified);
    }
}
