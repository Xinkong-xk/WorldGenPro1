package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.element;

import io.xk.worldgenpro.vanilla.enhance.structure.CustomStructurePoolGenerator;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.item.CheckSizeConfigItem;
import io.xk.worldgenpro.vanilla.enhance.structure.context.StructureContext;
import io.xk.worldgenpro.vanilla.enhance.structure.reflect.StructurePoolMixinReflect;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;

public class CheckSizeConfigElement {
    public static List<StructurePoolElement> findCheckConfigElements(
            CustomStructurePoolGenerator generator,
            PoolStructurePiece piece,
            MutableObject<VoxelShape> pieceShape,
            int currentSize,
            boolean modifyBoundingBox,
            BlockPos currentPos,
            Random random,
            BlockPos startPos,
            Identifier id,
            int maxDistanceFromCenter,
            BlockPos centerPos,
            int maxSize,
            Structure.Context vanillaContext,
            StructurePool structurePool,
            CheckSizeConfigItem checkSizeConfigItem,
            StructureContext structureContext
    ){
        // 先执行过程配置项
        ProcessRunner.run(
                currentPos,
                random,
                startPos,
                id,
                maxDistanceFromCenter,
                centerPos,
                maxSize,
                currentSize,
                vanillaContext,
                checkSizeConfigItem.getScript()
        );
        // 如果传入的是一个结构池
        if (checkSizeConfigItem.structurePoolObject != null){
            return getFromStructurePool(generator,piece,pieceShape,currentSize,modifyBoundingBox,currentPos,
                    random,startPos,id,maxDistanceFromCenter,centerPos,maxSize,vanillaContext,structurePool, checkSizeConfigItem,structureContext);
        }
        // 如果没有结构池
        else{
            return null;
        }
    }

    // 当传入的是一个结构池,则返回此结构池的结果
    public static List<StructurePoolElement> getFromStructurePool(CustomStructurePoolGenerator generator, PoolStructurePiece piece, MutableObject<VoxelShape> pieceShape, int currentSize, boolean modifyBoundingBox, BlockPos currentPos, Random random, BlockPos startPos, Identifier id, int maxDistanceFromCenter, BlockPos centerPos, int maxSize, Structure.Context vanillaContext, StructurePool structurePool,
                                                                  CheckSizeConfigItem checkSizeConfigItem,
                                                                  StructureContext structureContext){
        StructurePool structurePool_Object = checkSizeConfigItem.structurePoolObject;
        // 直接调用目标结构池的getCustomElementIndicesInRandomOrder方法
        return StructurePoolMixinReflect.getCustomElementIndicesInRandomOrder(
                structurePool_Object,
                generator,
                piece,
                pieceShape,
                currentSize,
                modifyBoundingBox,
                currentPos,
                random,
                startPos,
                id,
                maxDistanceFromCenter,
                centerPos,
                maxSize,
                vanillaContext,
                structureContext);
    }

}
