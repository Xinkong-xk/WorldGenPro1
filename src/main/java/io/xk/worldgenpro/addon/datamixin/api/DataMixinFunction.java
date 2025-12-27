package io.xk.worldgenpro.addon.datamixin.api;

import com.google.gson.JsonElement;
import io.xk.worldgenpro.addon.datamixin.config.DataMixinElement;
import net.minecraft.server.MinecraftServer;

import java.util.List;

@FunctionalInterface
public interface DataMixinFunction<T> {
    void mixin(MinecraftServer server,
               DataMixinElement dataMixinElement,
               String mixinType,
               String mixinTarget,
               T mixinTargetObject);
}
