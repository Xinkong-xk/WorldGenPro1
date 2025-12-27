package io.xk.worldgenpro.addon.semantic.geo;

import java.util.stream.Collectors;

public class AstPrinter implements GeoExpr.Visitor<String> {

    public String print(GeoExpr geoExpr) {
        return geoExpr.accept(this);
    }

    // -------------------------
    //  NOT expr
    // -------------------------
    @Override
    public String visitIsNotBiome(GeoExpr.IsNotBiomeGeoExpr e) {
        String targetStr = e.target != null ? e.target.accept(this) + " " : "";
        return targetStr + "IS_NOT" + "<" + visitBiome(e.biomeGeoExpr) + ">";
    }

    // -------------------------
    //  left OP right
    // -------------------------
    @Override
    public String visitBinary(GeoExpr.BinaryGeoExpr e) {
        return "(" + e.left.accept(this) + " " + e.op + " " + e.right.accept(this) + ")";
    }

    // -------------------------
    //  IS <biome>
    // -------------------------
    @Override
    public String visitIsBiome(GeoExpr.IsBiomeGeoExpr e) {
        String targetStr = e.target != null ? e.target.accept(this) + " " : "";
        return "(" +  targetStr + "IS" + "<" + visitBiome(e.biomeGeoExpr) + ">" +")";
    }

    // -------------------------
    //  bare BiomeGeoExpr
    // -------------------------
    @Override
    public String visitBiome(GeoExpr.BiomeGeoExpr e) {
        return "Biome[" + e.biomeId + "]";
    }

    // -------------------------
    // predicate(arg1, arg2 ...)
    // -------------------------
    @Override
    public String visitPredicate(GeoExpr.PredicateCallGeoExpr e) {
        String args = e.args.stream()
                .map(a -> a.accept(this))
                .collect(Collectors.joining(", "));
        return "(" + e.name + "(" + args + ")" +")";
    }
}
