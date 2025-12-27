package io.xk.worldgenpro.addon.roadgen;

import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoadGen {

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public static long chunkKeyFromBlockPos(BlockPos pos) {
        int cx = pos.getX() >> 4; // block -> chunk
        int cz = pos.getZ() >> 4;
        return chunkKey(cx, cz);
    }

    public static String MOD_ID = "worldgenpro";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
}
