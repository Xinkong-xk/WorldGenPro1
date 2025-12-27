package io.xk.worldgenpro.addon.semantic.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.addon.semantic.geo.GeoExpr;
import io.xk.worldgenpro.addon.semantic.geo.Parser;
import io.xk.worldgenpro.addon.semantic.geo.Tokenizer;

public class GeoSemanticConfig {
    private final String geoText;

    private GeoExpr geoExpr = null; // 缓存表达式结果

    public GeoSemanticConfig(String text) {
        this.geoText = text;

        if (text == null) {
            this.geoExpr = null;
        } else {
            // 过滤空行（全空白也认为空）
            if (text.isEmpty()) {
                this.geoExpr = null;
            }
        }

        if (this.geoText != null && ! this.geoText.isEmpty()) {
            // 提前编译表达式
            Tokenizer tokenizer = new Tokenizer(this.geoText);
            Parser parser = new Parser(tokenizer);
            this.geoExpr = parser.parseExpression();
        }
    }


    public GeoExpr getGeoExpr() {
        return geoExpr;
    }


    public String getGeoText() {
        return geoText;
    }

    /** Codec 实现 **/
    public static final Codec<GeoSemanticConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("geo_semantic","").forGetter(GeoSemanticConfig::getGeoText)
            ).apply(instance, GeoSemanticConfig::new)
    );

   @Override
   public String toString() {
       return "GeoSemanticConfig{" +
               "geoText='" + geoText + '\'' +
               ", geoExpr=" + geoExpr +
               '}';
   }
}
