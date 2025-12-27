package io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.element;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.utils.MathUtils;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Str_StrList_To_Null;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;

import java.util.LinkedHashMap;

public class ProcessRunner {
    public static void run(
            BlockPos currentPos,
            Random random,
            BlockPos startPos,
            Identifier id,
            int maxDistanceFromCenter,
            BlockPos centerPos,
            int maxSize,
            int currentSize,
            Structure.Context vanillaContext,
            Input_Str_StrList_To_Null runnable
    ){
        if(!runnable.isEmpty){
            // 计算
            int c_distance = MathUtils.chebyshev3D(startPos, currentPos);
            int e_distance = (int) MathUtils.euclidean3D(startPos, currentPos);
            // 构建参数列表
            LinkedHashMap<String, Object> argsMap = new LinkedHashMap<>(11);
            argsMap.put("maxDistanceFromCenter", maxDistanceFromCenter);
            argsMap.put("c_distance", c_distance);
            argsMap.put("e_distance", e_distance);
            argsMap.put("distance", e_distance);
            argsMap.put("startPos", startPos);
            argsMap.put("centerPos", centerPos);
            argsMap.put("maxSize", maxSize);
            argsMap.put("random", random);
            argsMap.put("vanillaContext", vanillaContext);
            argsMap.put("pos", currentPos);
            argsMap.put("currentSize", currentSize);
            // 运行
            runnable.run(
                    WorldGenPro.expressionManager,
                    argsMap
            );
        }
    }
}
