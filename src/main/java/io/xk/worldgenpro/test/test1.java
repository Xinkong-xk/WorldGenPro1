package io.xk.worldgenpro.test;

import com.google.common.collect.Sets;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.source.BiomeCoords;

import java.util.Set;

public class test1 {
    public static void main(String[] args) {
        BlockPos pos = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(0, 0, 0);
        System.err.println(pos2.equals(pos));
    }
    public static Set<int[]> getBiomesInArea(int x, int y, int z, int radius) {
        int i = BiomeCoords.fromBlock(x - radius);
        int j = BiomeCoords.fromBlock(y - radius);
        int k = BiomeCoords.fromBlock(z - radius);
        int l = BiomeCoords.fromBlock(x + radius);
        int m = BiomeCoords.fromBlock(y + radius);
        int n = BiomeCoords.fromBlock(z + radius);
        int o = l - i + 1;
        int p = m - j + 1;
        int q = n - k + 1;
        Set<int[]> set = Sets.newHashSet();

        for (int r = 0; r < q; r++) {
            for (int s = 0; s < o; s++) {
                for (int t = 0; t < p; t++) {
                    int u = i + s;
                    int v = j + t;
                    int w = k + r;
                    set.add(new int[] {u, v, w});
                }
            }
        }

        return set;
    }
}
