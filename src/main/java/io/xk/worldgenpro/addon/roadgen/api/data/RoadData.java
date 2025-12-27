package io.xk.worldgenpro.addon.roadgen.api.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.addon.roadgen.codec.config.Offset;
import io.xk.worldgenpro.addon.roadgen.helpers.Records;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RoadData {

    public int width;
    public int roadType;
    public List<BlockState> materials;
    public List<BlockPos> roadNodes;
    public List<Records.RoadSegmentPlacement> roadSegmentList;
    // 替代
    public Object2ObjectLinkedOpenHashMap<Long, List<RoadData.SegmentData>> roadSegmentLinkMap;
    // id
    public String roadEntryKey;

    public RoadData(
            int width,
            int roadType,
            List<BlockState> materials,
            List<Records.RoadSegmentPlacement> roadSegmentList,
            List<BlockPos> roadNodes,
            Object2ObjectLinkedOpenHashMap<Long, List<RoadData.SegmentData>> roadSegmentLinkMap,
            String roadEntryKey
    ) {
        this.width = width;
        this.roadType = roadType;
        this.materials = materials;
        this.roadSegmentList = roadSegmentList;
        this.roadNodes = roadNodes;
        this.roadSegmentLinkMap = roadSegmentLinkMap;
        this.roadEntryKey = roadEntryKey;
    }

    // getters
    public int getWidth() { return width; }
    public int getRoadType() { return roadType; }
    public List<BlockState> getMaterials() { return materials; }
    public List<Records.RoadSegmentPlacement> getRoadSegmentList() {return roadSegmentList;}
    public List<BlockPos> getRoadNodes() { return roadNodes; }
    public Object2ObjectLinkedOpenHashMap<Long, List<RoadData.SegmentData>> getRoadSegmentLinkMap() { return roadSegmentLinkMap; }
    public String getRoadEntryKey() { return roadEntryKey; }

    /**
     * 一个单独的 entry：用于将 map 变成列表
     */
    public record SegmentEntry(Long pos, List<SegmentData> datas) {
        public static final Codec<SegmentEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(SegmentEntry::pos),
                SegmentData.CODEC.listOf().fieldOf("datas").forGetter(SegmentEntry::datas)
        ).apply(instance, SegmentEntry::new));
    }

    public record PlacedDecoration(BlockPos pos, String pool, float p, Offset offset){
        public static final Codec<PlacedDecoration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("place_pos").forGetter(PlacedDecoration::pos),
                Codec.STRING.fieldOf("pool").forGetter(PlacedDecoration::pool),
                Codec.FLOAT.fieldOf("p").forGetter(PlacedDecoration::p),
                Offset.CODEC.fieldOf("offset").forGetter(PlacedDecoration::offset)
        ).apply(instance, PlacedDecoration::new));

        public static PlacedDecoration EMPTY = new PlacedDecoration(BlockPos.ORIGIN,"",1.0f,Offset.ZERO);
    }

    public record SegmentData(int i,
                              int segmentIndex,
                              Records.RoadSegmentPlacement placement,
                              PlacedDecoration placedDecoration,
                              int averageY){
        public static final Codec<SegmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("i").forGetter(SegmentData::i),
                Codec.INT.fieldOf("index").forGetter(SegmentData::segmentIndex),
                Records.RoadSegmentPlacement.CODEC.fieldOf("placement").forGetter(SegmentData::placement),
                PlacedDecoration.CODEC.fieldOf("placed_decoration").forGetter(SegmentData::placedDecoration),
                Codec.INT.fieldOf("average_y").forGetter(SegmentData::averageY)
        ).apply(instance, SegmentData::new));
    }

    /**
     * RoadData Codec
     */
    public static final Codec<RoadData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("width").forGetter(RoadData::getWidth),
                    Codec.INT.fieldOf("road_type").forGetter(RoadData::getRoadType),

                    BlockState.CODEC.listOf().fieldOf("materials").forGetter(RoadData::getMaterials),
                    Records.RoadSegmentPlacement.CODEC.listOf().fieldOf("placements").forGetter(RoadData::getRoadSegmentList),

                    BlockPos.CODEC.listOf().fieldOf("road_nodes").forGetter(RoadData::getRoadNodes),

                    // roadSegmentLinkMap: List<SegmentEntry> <-> LinkedOpenHashMap
                    SegmentEntry.CODEC.listOf()
                            .fieldOf("road_segments")
                            .xmap(
                                    list -> {
                                        var map = new Object2ObjectLinkedOpenHashMap<Long, List<SegmentData>>();
                                        for (SegmentEntry e : list) {
                                            map.put(e.pos(), e.datas());
                                        }
                                        return map;
                                    },
                                    map -> {
                                        List<SegmentEntry> list = new ArrayList<>(map.size());
                                        map.forEach((k, v) -> list.add(new SegmentEntry(
                                                k, // Long                  :   pos
                                                v  // List<SegmentData>     :   datas
                                                )));
                                        return list;
                                    }
                            )
                            .forGetter(RoadData::getRoadSegmentLinkMap),
                    Codec.STRING.fieldOf("road_entry_key").forGetter(RoadData::getRoadEntryKey)

            ).apply(instance, RoadData::new));

    @Override
    public String toString() {
        return "RoadData{" +
                "width=" + width +
                ", roadType=" + roadType +
                ", materials=" + materials +
                ", roadNodes=" + roadNodes +
                ", roadSegmentList=" + roadSegmentList +
                ", roadSegmentLinkMap=" + roadSegmentLinkMap +
                ", roadEntryKey='" + roadEntryKey + '\'' +
                '}';
    }
}
