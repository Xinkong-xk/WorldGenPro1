package io.xk.worldgenpro.core.resource.api;

public interface ResourceLoader {
    java.util.function.Predicate<net.minecraft.util.Identifier> getAllowedPathPredicate();

    String getPath();

    void load(java.util.Map<net.minecraft.util.Identifier, net.minecraft.resource.Resource> resourceMap);
}
