package io.xk.worldgenpro.vanilla.enhance.structure.reflect;

import io.xk.worldgenpro.vanilla.enhance.config.SuperLargeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.JigsawStructure;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JigsawStructureReflect {

    public static Field superLargeConfig;

    public static Field facingText;
    public static Field direction;
    public static Field facingBiome;
    public static Method buildBiome;

    static {
        try{
            superLargeConfig = JigsawStructure.class.getDeclaredField("superLargeConfig");
            superLargeConfig.setAccessible(true);

            facingText = JigsawStructure.class.getDeclaredField("facingText");
            facingText.setAccessible(true);

            direction = JigsawStructure.class.getDeclaredField("direction");
            direction.setAccessible(true);

            facingBiome = JigsawStructure.class.getDeclaredField("facingBiome");
            facingBiome.setAccessible(true);

            buildBiome = JigsawStructure.class.getDeclaredMethod("buildBiome", MinecraftServer.class);
            buildBiome.setAccessible(true);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void buildBiomeInvoke(JigsawStructure structure, MinecraftServer minecraftServer) {
        try {
            buildBiome.invoke(structure, minecraftServer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SuperLargeConfig getSuperLargeConfig(JigsawStructure jigsawStructure) {
        if (superLargeConfig != null){
            try{
                return (SuperLargeConfig) superLargeConfig.get(jigsawStructure);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static Direction getDirection(JigsawStructure jigsawStructure) {
        if (direction != null){
            try{
                return (Direction) direction.get(jigsawStructure);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static Biome getFacingBiome(JigsawStructure jigsawStructure) {
        if (facingBiome != null){
            try{
                return (Biome) facingBiome.get(jigsawStructure);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static String getFacingText(JigsawStructure jigsawStructure) {
        if (facingText != null){
            try{
                return (String) facingText.get(jigsawStructure);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
