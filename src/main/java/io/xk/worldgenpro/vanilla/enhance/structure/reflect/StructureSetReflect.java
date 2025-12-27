package io.xk.worldgenpro.vanilla.enhance.structure.reflect;

import io.xk.worldgenpro.addon.semantic.config.GeoSemanticConfig;
import net.minecraft.structure.StructureSet;

import java.lang.reflect.Field;

public class StructureSetReflect {

    public static Field geoSemanticConfig;

    static {
        try{
            geoSemanticConfig = StructureSet.class.getDeclaredField("geoSemanticConfig");
            geoSemanticConfig.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static GeoSemanticConfig getGeoSemanticConfig(StructureSet structureSet) {
        if (geoSemanticConfig != null){
            try{
                return (GeoSemanticConfig) geoSemanticConfig.get(structureSet);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
