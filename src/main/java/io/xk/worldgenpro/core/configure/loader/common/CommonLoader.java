package io.xk.worldgenpro.core.configure.loader.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CommonLoader {
    /**
     * 读取注册文件
     * @param reader JSON Reader
     * @param identifier 文件路径标识，如 develop/config/register/test.json
     * @return 解析出的注册项列表
     */
    public static ObjectArrayList<CommonEntry> load(Gson gson, BufferedReader reader, String identifier) {
        try {
            Type listType = new TypeToken<List<CommonEntry>>(){}.getType();
            ArrayList<CommonEntry> entries = gson.fromJson(reader, listType);

            if (entries == null) {
                return new ObjectArrayList<>();
            }

            for (CommonEntry entry : entries) {
                if (entry.name == null || entry.name.isEmpty()) {
                    throw new JsonParseException("Missing 'name' in common entry in file: " + identifier);
                }
                if (entry.type == null || entry.type.isEmpty()) {
                    throw new JsonParseException("Missing 'type' in common entry: " + entry.name);
                }
                if (entry.attributes == null) {
                    entry.attributes = new JsonObject();
                }
            }
            return new ObjectArrayList<>(entries);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load common file: " + identifier, e);
        }
    }
}
