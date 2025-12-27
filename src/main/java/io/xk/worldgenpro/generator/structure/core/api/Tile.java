package io.xk.worldgenpro.generator.structure.core.api;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public abstract class Tile {
    public final String id;
    public final double weight;

    public Tile(String id, double weight){
        this.id = id;
        this.weight = weight;
    }

    public void applyDefaultAttributes(Object2ObjectOpenHashMap<String,Object> attributes) {}

    @Override
    public String toString() {
        return super.toString();
    }
}
