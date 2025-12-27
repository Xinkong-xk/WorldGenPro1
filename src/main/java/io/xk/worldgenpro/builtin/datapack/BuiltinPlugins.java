package io.xk.worldgenpro.builtin.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.xk.worldgenpro.core.configure.ConfigureManager;
import io.xk.worldgenpro.generator.structure.core.api.Dir;
import io.xk.worldgenpro.generator.structure.core.api.wfc.StructureTile;
import io.xk.worldgenpro.test.city.CityStructureTiles;

import java.util.EnumSet;


public class BuiltinPlugins {

    public static void register(ConfigureManager configureManager){

        // 注册结构瓦片加载监听器->测试城市生成:大型
        configureManager.registerLoader.eventManager.registerCommon(
                "register:builtin.pool.wfc.test.city.large.structure_tile",
                "builtin.pool.wfc.test.city.large",
                (commonEntry, resourceManager, configureManager1) -> {

                    boolean exists = CityStructureTiles.city_large.stream().anyMatch(structureTile -> structureTile.id.equals(commonEntry.name));
                    if (! exists){
                        CityStructureTiles.city_large.add(
                                new StructureTile(
                                        commonEntry.name,
                                        commonEntry.attributes.get("nbt_file").getAsString(),
                                        commonEntry.attributes.get("weight").getAsFloat(),
                                        parseConnections(commonEntry.attributes)
                                )
                        );
                    }
                }
        );


        // 注册结构瓦片加载监听器->测试城市生成:大型
        configureManager.registerLoader.eventManager.registerCommon(
                "register:builtin.pool.wfc.test.city.middle.structure_tile",
                "builtin.pool.wfc.test.city.middle",
                (commonEntry, resourceManager, configureManager1) -> {

                    boolean exists = CityStructureTiles.city_middle.stream().anyMatch(structureTile -> structureTile.id.equals(commonEntry.name));
                    if (! exists){
                        CityStructureTiles.city_middle.add(
                                new StructureTile(
                                        commonEntry.name,
                                        commonEntry.attributes.get("nbt_file").getAsString(),
                                        commonEntry.attributes.get("weight").getAsFloat(),
                                        parseConnections(commonEntry.attributes)
                                )
                        );
                    }
                }
        );
    }


    /**
     * 从 JSON 对象解析 "connections" 字段为 EnumSet<Dir>
     * @param json JSON 对象
     * @return EnumSet<Dir>，若无 connections 字段则返回空集合
     */
    public static EnumSet<Dir> parseConnections(JsonObject json) {
        EnumSet<Dir> result = EnumSet.noneOf(Dir.class);

        if (!json.has("connections")) {
            return result; // 没有字段，直接返回空
        }

        JsonArray array = json.getAsJsonArray("connections");
        for (JsonElement element : array) {
            String name = element.getAsString();
            try {
                Dir dir = Dir.valueOf(name); // 匹配枚举常量
                result.add(dir);
            } catch (IllegalArgumentException e) {
                // 如果字符串不是合法的枚举常量，可以选择忽略或抛异常
                System.err.println("[WARN] Unknown connection direction: " + name);
            }
        }
        return result;
    }

}
