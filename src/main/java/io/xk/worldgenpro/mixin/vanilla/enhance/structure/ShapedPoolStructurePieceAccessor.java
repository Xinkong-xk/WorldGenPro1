package io.xk.worldgenpro.mixin.vanilla.enhance.structure;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.shape.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.structure.pool.StructurePoolBasedGenerator.ShapedPoolStructurePiece.class)
public interface ShapedPoolStructurePieceAccessor {
    @Invoker("<init>")
    static StructurePoolBasedGenerator.ShapedPoolStructurePiece invokeNew(
            PoolStructurePiece piece,
            MutableObject<VoxelShape> shape,
            int minY
    ) {
        throw new AssertionError();
    }
}