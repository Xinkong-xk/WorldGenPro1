package io.xk.worldgenpro.vanilla.enhance.structure.context.counter;

import com.mojang.datafixers.util.Either;
import net.minecraft.structure.pool.*;
import net.minecraft.util.Identifier;
import io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext;

/**
 * 独立的计数器类：根据 location 统计 StructurePoolElement 的生成次数
 */
public class StructureCounter {
    private final StructureContext context;

    public StructureCounter(StructureContext context) {
        this.context = context;
    }

    /**
     * 对外方法：通过字符串形式的 location 统计生成次数
     */
    public int getPlacedElementCountByLocation(String location) {
        int count = 0;
        for (StructurePoolElement element : context.placedElementMap.keySet()) {
            count += countElementByLocation(element, location);
        }
        return count;
    }

    /**
     * 对外方法：通过 Identifier 统计生成次数
     */
    public int getPlacedElementCountByLocation(Identifier id) {
        return getPlacedElementCountByLocation(id.toString());
    }

    /**
     * 递归统计单个 StructurePoolElement 或其内部 ListPoolElement 匹配 location 的数量
     */
    private int countElementByLocation(StructurePoolElement element, String location) {
        int count = 0;
        Integer c = context.placedElementMap.get(element);
        if (c == null) c = 0;

        if (element instanceof SinglePoolElement single) {
            Either<Identifier, ?> loc = single.location;
            if (loc.left().isPresent() && loc.left().get().toString().equals(location)) {
                count += c;
            }
        } else if (element instanceof LegacySinglePoolElement legacy) {
            Either<Identifier, ?> loc = legacy.location;
            if (loc.left().isPresent() && loc.left().get().toString().equals(location)) {
                count += c;
            }
        } else if (element instanceof ListPoolElement list) {
            for (StructurePoolElement sub : list.elements) {
                count += countElementByLocation(sub, location);
            }
        }
        // 未来可以扩展 FeaturePoolElement 或其他类型

        return count;
    }
}
