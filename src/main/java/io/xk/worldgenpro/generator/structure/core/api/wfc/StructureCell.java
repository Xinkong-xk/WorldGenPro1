package io.xk.worldgenpro.generator.structure.core.api.wfc;

import io.xk.worldgenpro.generator.structure.core.api.Cell;
import io.xk.worldgenpro.generator.structure.placement.StructureGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class StructureCell extends Cell<StructureTile> {

    public StructureCell[] neighbours = new StructureCell[4];

    public int x;
    public int z;
    public int y;
    public int size;

    public StructureCell(StructureTile tile) {
        super(tile);
    }

    @Override
    public void place(int size, int x, int y , int z, BlockPos startPos, ServerWorld world,
                      Object... args) {
        BlockPos pos = new BlockPos(x,y,z);
        try {
            StructureGenerator.GenerateNbt(
                    world,
                    getTile().nbtFile,
                    pos,
                    StructurePool.Projection.RIGID,
                    20,
                    80,
                    true,
                    false,
                    null
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "StructureCell{" +
                "neighbours=" + Arrays.toString(neighbours) +
                ", x=" + x +
                ", z=" + z +
                ", y=" + y +
                ", size=" + size +
                '}';
    }
}
