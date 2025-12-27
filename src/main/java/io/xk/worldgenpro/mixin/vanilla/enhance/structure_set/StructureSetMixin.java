package io.xk.worldgenpro.mixin.vanilla.enhance.structure_set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(StructureSet.class)
public class StructureSetMixin {

    @Shadow
    @Final
    @Mutable
    private static Codec<StructureSet> CODEC;

    // geoSemanticConfig
    @Unique
    private GeoSemanticConfig geoSemanticConfig;

    static {
        CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                StructureSet.WeightedEntry.CODEC.listOf().fieldOf("structures").forGetter(StructureSet::structures),
                                StructurePlacement.TYPE_CODEC.fieldOf("placement").forGetter(StructureSet::placement),
                                GeoSemanticConfig.CODEC
                                        .optionalFieldOf("geo", new GeoSemanticConfig(""))
                                        .forGetter(s -> ((StructureSetMixin) (Object) s).geoSemanticConfig)
                        )
                        .apply(instance, StructureSetMixin::create));

    }

    /**
     * 自定义创建方法
     * @param structures 权重结构列表
     * @param placement 结构放置
     * @param geoSemanticConfig 地理语义配置
     * @return 结构集
     */
    @Unique
    private static StructureSet create(List<StructureSet.WeightedEntry> structures, StructurePlacement placement, GeoSemanticConfig geoSemanticConfig){
        StructureSet structureSet = new StructureSet(structures, placement);
        StructureSetMixin mixin = (StructureSetMixin) (Object) structureSet;
        mixin.geoSemanticConfig = geoSemanticConfig; // 设置 geoSemanticConfig 字段
        return structureSet;
    }

    /**
     * 拦截正常实例化
     * @param structure 结构
     * @param placement 结构放置
     * @param ci 回调信息
     */
    @Inject(
            method = "<init>(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/world/gen/chunk/placement/StructurePlacement;)V",
            at = @At("RETURN")
    )
    private void onInit(RegistryEntry structure, StructurePlacement placement, CallbackInfo ci){
        this.geoSemanticConfig = new GeoSemanticConfig("");
    }

}
