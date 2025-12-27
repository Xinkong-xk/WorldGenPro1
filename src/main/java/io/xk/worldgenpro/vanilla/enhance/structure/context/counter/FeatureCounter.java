package io.xk.worldgenpro.vanilla.enhance.structure.context.counter;

import io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.FeaturePoolElement;
import net.minecraft.structure.pool.ListPoolElement;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.world.gen.feature.PlacedFeature;

public class FeatureCounter {
    private final StructureContext context;

    public FeatureCounter(StructureContext context) {
        this.context = context;
    }

    /**
     * 递归统计 FeaturePoolElement（包括嵌套在 ListPoolElement 里的）生成次数
     */
    private int countFeatureElements(StructurePoolElement element, RegistryEntry<PlacedFeature> feature) {
        int count = 0;
        Integer c = this.context.placedElementMap.get(element);
        if (c == null) c = 0;

        if (element instanceof FeaturePoolElement featureElement) {
            if (featureElement.feature.equals(feature)) {
                count += c;
            }
        } else if (element instanceof ListPoolElement list) {
            for (StructurePoolElement sub : list.elements) {
                count += countFeatureElements(sub, feature);
            }
        }

        return count;
    }

    /**
     * 对外方法：统计指定 Feature 被生成的次数
     */
    public int getPlacedFeatureCount(RegistryEntry<PlacedFeature> feature) {
        int total = 0;
        for (StructurePoolElement element : this.context.placedElementMap.keySet()) {
            total += countFeatureElements(element, feature);
        }
        return total;
    }
}
