package io.xk.worldgenpro.generator.structure.core.api;


import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 表示地图上的一个格子，包含：
 *  - Tile 引用（全局共享）
 *  - 每格独立的属性表
 */
public class  Cell <T extends Tile> {
    private T tile;
    private Object2ObjectOpenHashMap<String, Object> attributes = new Object2ObjectOpenHashMap<>();

    public Cell(T tile) {
        this.tile = tile;
        this.tile.applyDefaultAttributes(attributes);
    }

    public T getTile() {
        return tile;
    }

    public void setTile(T tile) {
        this.tile = tile;
    }

    /** 添加或更新属性 */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /** 获取单个属性 */
    public Object getAttribute(String key) {
        return attributes == null ? null : attributes.get(key);
    }

    /** 获取完整属性表 */
    public Object2ObjectOpenHashMap<String, Object> getAttributes() {
        return attributes;
    }

    /** 设置完整属性表（通常不建议外部直接替换） */
    public void setAttributes(Object2ObjectOpenHashMap<String, Object> newAttributes) {
        this.attributes = newAttributes;
    }

    public void place(int size, int x, int y , int z, BlockPos startPos, ServerWorld world,
                               Object... args){};

    public void render(){

    }

    @Override
    public String toString() {
        return "Cell{" +
                "tile=" + (tile != null ? tile.id : "null") +
                ", attributes=" + attributes +
                '}';
    }
}
