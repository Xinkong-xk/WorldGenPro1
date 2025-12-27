package io.xk.worldgenpro.addon.semantic.geo;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;

import java.util.List;

public class GeoContext {

    public ServerWorld world;

    public Structure structure;

    public GeoContext (ServerWorld world, Structure structure) {
        this.world = world;
    }


    /** 判断当前位置是否属于这些生物群系之一 */
    public boolean isBiome(List<String> biomes) {
        // TODO: 这里实现真实逻辑，例如读取当前坐标的 biome
        System.out.println("Checking biome in: " + biomes);
        return true; // 示例
    }

    public Biome getBiome(Chunk chunk) {
        return null;
    }

}
