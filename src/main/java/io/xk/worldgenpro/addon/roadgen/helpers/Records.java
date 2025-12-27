package io.xk.worldgenpro.addon.roadgen.helpers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class Records {

    public record WoodAssets(Block fence, Block hangingSign, Block planks) {}

    public record RoadDecoration(BlockPos placePos, Vec3i vector, int centerBlockCount, String signText, boolean isStart) {}

    public record RoadSegmentPlacement(BlockPos middlePos, List<BlockPos> positions) {
        public static final Codec<RoadSegmentPlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("middle_pos").forGetter(RoadSegmentPlacement::middlePos),
                BlockPos.CODEC.listOf().fieldOf("positions").forGetter(RoadSegmentPlacement::positions)
        ).apply(instance, RoadSegmentPlacement::new));

        @Override
        public String toString() {
            return "RoadSegmentPlacement{" +
                    "middlePos=" + middlePos +
                    ", positions=" + positions +
                    '}';
        }
    }

    public record StructureLocationData(List<BlockPos> structureLocations) {
        public StructureLocationData(List<BlockPos> structureLocations) {
            this.structureLocations = new ArrayList<>(structureLocations); // mutable copy
        }

        public void addStructure(BlockPos pos) {
            structureLocations.add(pos);
        }

        public static final Codec<StructureLocationData> CODEC = BlockPos.CODEC
                .listOf()
                .xmap(StructureLocationData::new, StructureLocationData::structureLocations);
    }
    public record StructureConnection(BlockPos from, BlockPos to,String roadEntryKey) {
        public static final Codec<StructureConnection> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("from").forGetter(StructureConnection::from),
                        BlockPos.CODEC.fieldOf("to").forGetter(StructureConnection::to),
                        Codec.STRING.fieldOf("road_entry_key").forGetter(StructureConnection::roadEntryKey)
                ).apply(instance, StructureConnection::new)
        );

        @Override
        public String toString() {
            return "StructureConnection{" +
                    "from=" + from +
                    ", to=" + to +
                    ", roadEntryKey='" + roadEntryKey + '\'' +
                    '}';
        }
    }
}
