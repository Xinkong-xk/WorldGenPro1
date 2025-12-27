package io.xk.worldgenpro.addon.roadgen.api.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.addon.roadgen.api.GridGraph;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.structure.StructureSet;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;


import java.util.ArrayList;
import java.util.List;

/**
 * 记录 StructureSet 在每个 region 中的生成结果。
 * 保存：Map → List
 * 加载：List → Map
 */
public class RegionStructureMap {

    /**
     * StructureSet:String → GridGraph
     */
    public final Object2ObjectOpenHashMap<String, GridGraph> regionMap
            = new Object2ObjectOpenHashMap<>();

    /**
     * List 形式的数据结构（Codec 用）
     */
    public record Entry(
            String set,
            GridGraph graph
    ) {}

    public GridGraph getGridGraph(String set,StructureSet structureSet) {
        if (!regionMap.containsKey(set)) {
            RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)structureSet.placement();
            regionMap.put(set,new GridGraph(randomSpreadStructurePlacement.getSpacing()));
        }
        return regionMap.get(set);
    }

    public RegionStructureMap(){}

    /**
     * Entry 的 CODEC：
     * StructureSet 使用 String
     * GridGraph 使用你先前实现的 CODEC
     */
    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("structure_set")
                            .forGetter(Entry::set),
                    GridGraph.CODEC.fieldOf("graph")
                            .forGetter(Entry::graph)
            ).apply(instance, Entry::new)
    );

    /**
     * RegionStructureMap 的 CODEC（核心部分）*
     * 保存时：regionMap → List<Entry>
     * 加载时：List<Entry> → regionMap（重建）
     */
    public static final Codec<RegionStructureMap> CODEC =
            ENTRY_CODEC.listOf().xmap(
                    // --- toMap: List<Entry> -> RegionStructureMap ---
                    list -> {
                        RegionStructureMap map = new RegionStructureMap();
                        for (Entry e : list) {
                            map.regionMap.put(e.set(), e.graph());
                        }
                        return map;
                    },

                    // --- fromMap: RegionStructureMap -> List<Entry> ---
                    map -> {
                        List<Entry> list = new ArrayList<>(map.regionMap.size());
                        map.regionMap.forEach((set, graph) ->
                                list.add(new Entry(set, graph)));
                        return list;
                    }
            );

    @Override
    public String toString() {
        return "RegionStructureMap{" +
                "regionMap=" + regionMap +
                '}';
    }
}
