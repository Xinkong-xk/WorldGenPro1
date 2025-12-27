package io.xk.worldgenpro.addon.semantic;

import net.minecraft.structure.StructureSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.JigsawStructure;

public class GenerateResult {
    public boolean success;
    public StructureSet structureSet;
    public JigsawStructure structure;
    public ChunkPos chunkPos;

    public GenerateResult(boolean success,
                          StructureSet structureSet,
                          JigsawStructure structure,
                          ChunkPos chunkPos) {
        this.success = success;
        this.structureSet = structureSet;
        this.structure = structure;
        this.chunkPos = chunkPos;
    }

    @Override
    public String toString() {
        return "GenerateResult{" +
                "success=" + success +
                ", structureSet=" + structureSet +
                ", pool=" + structure +
                ", chunkPos=" + chunkPos +
                '}';
    }
}
