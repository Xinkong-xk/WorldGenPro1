package io.xk.worldgenpro.core.configure.loader.event;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EventLoader {
    /**
     * 读取脚本文件
     * @param reader JSON Reader
     * @param identifier 文件路径标识，如 develop/config/event/test.json
     * @return 解析出的脚本项列表
     */
    public static ObjectArrayList<EventEntry> load(Gson gson, BufferedReader reader, String identifier) {
        try {
            Type listType = new TypeToken<List<EventEntry>>(){}.getType();
            ArrayList<EventEntry> scripts = gson.fromJson(reader, listType);

            if (scripts == null) {
                return new ObjectArrayList<>();
            }

            for (EventEntry event : scripts) {
                if (event.name == null || event.name.isEmpty()) {
                    throw new JsonParseException("Missing 'name' in event entry in file: " + identifier);
                }
                if (event.script == null || event.script.isEmpty()) {
                    throw new JsonParseException("Missing 'event' path in event entry: " + event.name);
                }
                if (event.entry == null || event.entry.isEmpty()) {
                    throw new JsonParseException("Missing 'entry' in event entry: " + event.name);
                }
            }
            return new ObjectArrayList<>(scripts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load event file: " + identifier, e);
        }
    }
}
