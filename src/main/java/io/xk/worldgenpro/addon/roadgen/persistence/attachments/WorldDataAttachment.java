package io.xk.worldgenpro.addon.roadgen.persistence.attachments;

import com.mojang.serialization.Codec;
import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.api.data.RegionStructureMap;
import io.xk.worldgenpro.addon.roadgen.api.data.RoadData;
import io.xk.worldgenpro.addon.roadgen.helpers.Records;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WorldDataAttachment {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadGen.MOD_ID);

    public static final AttachmentType<List<Records.StructureConnection>> CONNECTED_STRUCTURES = AttachmentRegistry.createPersistent(
            new Identifier(RoadGen.MOD_ID, "connected_villages"),
            Codec.list(Records.StructureConnection.CODEC)
    );


    public static final AttachmentType<Records.StructureLocationData> STRUCTURE_LOCATIONS = AttachmentRegistry.createPersistent(
            new Identifier(RoadGen.MOD_ID, "village_locations"),
            Records.StructureLocationData.CODEC
    );

    public static final AttachmentType<List<RoadData>> ROAD_DATA_LIST = AttachmentRegistry.createPersistent(
            new Identifier(RoadGen.MOD_ID, "road_chunk_data_map"),
            Codec.list(RoadData.CODEC)
    );

    // 新的
    public static final AttachmentType<RegionStructureMap> REGION_STRUCTURE_MAP = AttachmentRegistry.createPersistent(
            new Identifier(RoadGen.MOD_ID, "region_structure_map"),
            RegionStructureMap.CODEC
    );

    public static final AttachmentType<RegionStructureMap> GRID_GRAPH = AttachmentRegistry.createPersistent(
            new Identifier(RoadGen.MOD_ID, "region_structure_map"),
            RegionStructureMap.CODEC
    );

    public static void registerWorldDataAttachment() {
        LOGGER.info("[RoadGen] Registering WorldData attachment");
    }
}
