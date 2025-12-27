package io.xk.worldgenpro.addon.datamixin.mixintype.utils.structurepool;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Int_IntList_Str_StrList_To_Int;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Str_StrList_To_Null;
import io.xk.worldgenpro.vanilla.enhance.config.api.ElemConfig;
import net.minecraft.structure.pool.StructurePoolElement;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

public class StructurePoolUtil {

    public static Codec<Pair<StructurePoolElement, ElemConfig>> CODEC = elementCodec();

    /** Codec for (element + (weightExpr,countExpr)) */
    @Unique
    public static Codec<Pair<StructurePoolElement, ElemConfig>> elementCodec() {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        // 结构池元素
                        StructurePoolElement.CODEC
                                .fieldOf("element")
                                .forGetter(Pair::getFirst),
                        // 元素权重
                        Input_Int_IntList_Str_StrList_To_Int.CODEC
                                .fieldOf("weight")
                                .forGetter(p -> p.getSecond().weight()),
                        // 最大生成数量,默认是int.max_value
                        Input_Int_IntList_Str_StrList_To_Int.CODEC
                                .optionalFieldOf("max_count", Input_Int_IntList_Str_StrList_To_Int.ofSingleInt(Integer.MAX_VALUE))
                                .forGetter(p -> p.getSecond().max_count()),
                        // 脚本
                        Input_Str_StrList_To_Null.CODEC
                                .optionalFieldOf("script", new Input_Str_StrList_To_Null(List.of("")))
                                .forGetter(p -> p.getSecond().script()),
                        // 最小生成数量,默认是0
                        Input_Int_IntList_Str_StrList_To_Int.CODEC
                                .optionalFieldOf("min_count", Input_Int_IntList_Str_StrList_To_Int.ofSingleInt(0))
                                .forGetter(p -> p.getSecond().min_count())
                ).apply(instance,
                        (element, weight, max_count, script, min_count) ->
                                Pair.of(element, new ElemConfig(weight, max_count, script, min_count))
                )
        );
    }
}
