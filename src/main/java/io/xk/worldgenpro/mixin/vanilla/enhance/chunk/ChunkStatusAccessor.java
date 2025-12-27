package io.xk.worldgenpro.mixin.vanilla.enhance.chunk;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.EnumSet;

@Mixin(ChunkStatus.class)
public interface ChunkStatusAccessor {

    @Invoker("<init>")
    static ChunkStatus invokeNew(
            @Nullable ChunkStatus previous,
            int taskMargin,
            boolean shouldAlwaysUpgrade,
            EnumSet<Heightmap.Type> heightMapTypes,
            ChunkStatus.ChunkType chunkType,
            ChunkStatus.GenerationTask generationTask,
            ChunkStatus.LoadTask loadTask
    ) {
        throw new AssertionError();
    }
}
