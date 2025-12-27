package io.xk.worldgenpro.vanilla.enhance.structure.helper;

import io.xk.worldgenpro.vanilla.enhance.config.Input_Int_IntList_Str_StrList_To_Int;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.alias.StructurePoolAliasBinding;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.JigsawStructure;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

public class JigsawStructureHelper {

    /**
     * 创建 JigsawStructure 实例并允许传入自定义 IntRange
     *
     * @param config                          配置对象
     * @param startPool                       起始结构池
     * @param startJigsawName                 起始拼图名称
     * @param size                            结构的大小
     * @param startHeight                     起始高度提供器
     * @param useExpansionHack                是否使用扩展黑客
     * @param projectStartToHeightmap         高度图类型（可选）
     * @param inputIntIntListStrStrListToInt              可配置数字项
     * @param poolAliasBindings               结构池别名绑定列表
     * @return JigsawStructure 实例
     */
    public static JigsawStructure create(
            JigsawStructure.Config config,
            RegistryEntry<StructurePool> startPool,
            Optional<Identifier> startJigsawName,
            int size,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Type> projectStartToHeightmap,
            Input_Int_IntList_Str_StrList_To_Int inputIntIntListStrStrListToInt,
            List<StructurePoolAliasBinding> poolAliasBindings
    ) {
        // 创建 JigsawStructure 实例
        JigsawStructure structure = new JigsawStructure(
                config,
                startPool,
                startJigsawName,
                size,
                startHeight,
                useExpansionHack,
                projectStartToHeightmap,
                0,
                poolAliasBindings
        );

        try {
            Field field = JigsawStructure.class.getDeclaredField("maxDistanceConfig");
            field.setAccessible(true);
            field.set(structure, inputIntIntListStrStrListToInt);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return structure;
    }
}
