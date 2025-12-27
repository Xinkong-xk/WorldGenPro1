package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.structure.pool.StructurePool;

public class CrossPoints{
    public String pool;
    public Offset offset;
    // 后置获取
    public StructurePool poolObject;

    public String getPool() {
        return pool;
    }

    public Offset getOffset() {
        return offset;
    }

    /**
     * 穿越点,表示道路穿越某个群系时道路长度归一化后的一个点,范围在[0,1.0]
     * @param pool 结构池,指向一个已配置的结构池对象
     * @param offset 坐标偏移值
     */
    public CrossPoints(String pool, Offset offset){
        this.pool = pool;
        this.offset = offset;
    }

    public static final Codec<CrossPoints> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("pool").forGetter(CrossPoints::getPool),
                    Offset.CODEC.optionalFieldOf("offset", Offset.ZERO)
                            .forGetter(CrossPoints::getOffset)
            ).apply(instance, CrossPoints::new));

    @Override
    public String toString() {
        return "CrossPoints{" +
                "pool='" + pool + '\'' +
                ", offset=" + offset +
                ", poolObject=" + poolObject +
                '}';
    }
}
