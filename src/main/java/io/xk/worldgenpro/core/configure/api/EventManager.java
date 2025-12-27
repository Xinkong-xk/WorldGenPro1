package io.xk.worldgenpro.core.configure.api;

import com.mojang.datafixers.util.Pair;
import io.xk.worldgenpro.core.configure.ConfigureManager;
import io.xk.worldgenpro.core.configure.loader.common.CommonEntry;
import io.xk.worldgenpro.core.configure.loader.event.EventEntry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resource.ResourceManager;
import org.apache.logging.log4j.util.TriConsumer;


public class EventManager {
    // 通用注册项
    public Object2ObjectOpenHashMap<String, Pair<String, TriConsumer< CommonEntry, ResourceManager, ConfigureManager>>> CommonRegisterEntries = new Object2ObjectOpenHashMap<>();
    // 事件注册项
    public Object2ObjectOpenHashMap<String, TriConsumer<EventEntry, ResourceManager, ConfigureManager>> EventRegisterEntries = new Object2ObjectOpenHashMap<>();


    public void registerCommon(String identifier,String type, TriConsumer<CommonEntry, ResourceManager, ConfigureManager> listener){
        CommonRegisterEntries.put(identifier,new Pair<>(type,listener));
    }

    public void registerEvent(String identifier, TriConsumer<EventEntry, ResourceManager, ConfigureManager> listener){
        EventRegisterEntries.put(identifier, listener);
    }

    public void processCommonEntry(CommonEntry entry, ResourceManager resourceManager, ConfigureManager configureManager) {
        for (Pair< String, TriConsumer<CommonEntry, ResourceManager, ConfigureManager>> listener : CommonRegisterEntries.values()) {
            if (entry.type.equals(listener.getFirst())){
                listener.getSecond().accept(entry, resourceManager, configureManager);
            }
        }
    }

    public void processEventEntry(EventEntry entry, ResourceManager resourceManager, ConfigureManager configureManager) {
        for (TriConsumer<EventEntry, ResourceManager, ConfigureManager> listener  : EventRegisterEntries.values()) {
            listener.accept(entry, resourceManager, configureManager);
        }
    }
}
