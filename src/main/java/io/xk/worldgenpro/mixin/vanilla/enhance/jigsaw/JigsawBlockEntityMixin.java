package io.xk.worldgenpro.mixin.vanilla.enhance.jigsaw;

import io.xk.worldgenpro.vanilla.enhance.structure.generator.VanillaStructurePoolBasedGenerator;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.block.enums.Orientation;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(net.minecraft.block.entity.JigsawBlockEntity.class)
public class JigsawBlockEntityMixin {

    /**
     * @author xk
     * @reason 使用了新的生成器
     */
    @Overwrite
    public void generate(ServerWorld world, int maxDepth, boolean keepJigsaws) {
        JigsawBlockEntity jigsawBlockEntity = (JigsawBlockEntity)(Object)this;
        BlockPos blockPos = jigsawBlockEntity.getPos().offset(((Orientation)jigsawBlockEntity.getCachedState().get(JigsawBlock.ORIENTATION)).getFacing());
        Registry<StructurePool> registry = world.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
        RegistryEntry<StructurePool> registryEntry = registry.entryOf(jigsawBlockEntity.getPool());
        VanillaStructurePoolBasedGenerator.generate(world, registryEntry, jigsawBlockEntity.getTarget(), maxDepth, blockPos, keepJigsaws);
    }
}
