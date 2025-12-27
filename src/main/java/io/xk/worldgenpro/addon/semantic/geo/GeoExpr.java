package io.xk.worldgenpro.addon.semantic.geo;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.List;

public interface GeoExpr {
    <R> R accept(Visitor<R> visitor);

    /* ==========================
       一元 NOT，支持 target
       ========================== */
    class IsNotBiomeGeoExpr implements GeoExpr {
        public final GeoExpr target;    // 新增 target
        public final BiomeGeoExpr biomeGeoExpr;

        public IsNotBiomeGeoExpr(GeoExpr target, BiomeGeoExpr biomeGeoExpr) {
            this.target = target;
            this.biomeGeoExpr = biomeGeoExpr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIsNotBiome(this);
        }
    }

    /* ==========================
   表示单个或多个 biome 的表达式
   可作为 IS / IS_NOT 的 target
   ========================== */
    class BiomeGeoExpr implements GeoExpr {
        public final String biomeId;  // 记录群系字符串
        public ObjectArrayList<Biome> biomes;

        public BiomeGeoExpr(String biomeId) {
            this.biomeId = biomeId;
        }

        public void loadBiomes(MinecraftServer server){
            biomes = new ObjectArrayList<>();
            String[] biomeList = biomeId.split(",");
            Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
            for (String b : biomeList){
                Biome biome = biomeRegistry.get(new Identifier(b));
                biomes.add(biome);
            }
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBiome(this);
        }

        @Override
        public String toString() {
            return "<" + biomeId + ">";
        }
    }

    /* ==========================
       二元 AND / OR
       ========================== */
    class BinaryGeoExpr implements GeoExpr {
        public final String op;  // "AND" or "OR"
        public final GeoExpr left;
        public final GeoExpr right;

        public BinaryGeoExpr(String op, GeoExpr left, GeoExpr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinary(this);
        }
    }

    /* ==========================
       IS <biome> or bare <biome>, 支持 target
       ========================== */
    class IsBiomeGeoExpr implements GeoExpr {
        public final GeoExpr target;    // 新增 target
        public final BiomeGeoExpr biomeGeoExpr;

        public IsBiomeGeoExpr(GeoExpr target, BiomeGeoExpr biomeGeoExpr) {
            this.target = target;
            this.biomeGeoExpr = biomeGeoExpr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIsBiome(this);
        }
    }

    /* ==========================
       predicate(a,b,c...) 调用
       如 adjacent_to(...)
       ========================== */
    class PredicateCallGeoExpr implements GeoExpr {
        public final String name;
        public final List<GeoExpr> args;

        public PredicateCallGeoExpr(String name, List<GeoExpr> args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPredicate(this);
        }
    }


    /* ==========================
       Visitor 接口
       ========================== */
    interface Visitor<R> {
        R visitIsNotBiome(IsNotBiomeGeoExpr e);
        R visitBinary(BinaryGeoExpr e);
        R visitIsBiome(IsBiomeGeoExpr e);
        R visitPredicate(PredicateCallGeoExpr e);
        R visitBiome(BiomeGeoExpr e);
    }
}
