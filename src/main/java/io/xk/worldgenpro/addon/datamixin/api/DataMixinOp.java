package io.xk.worldgenpro.addon.datamixin.api;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum DataMixinOp implements StringIdentifiable {
    ADD("add"),
    REMOVE("remove"),
    REPLACE("replace"),
    BASE("base");

    public static final Codec<DataMixinOp> CODEC =
            StringIdentifiable.createCodec(DataMixinOp::values);

    private final String name;

    DataMixinOp(String name) {
        this.name = name;
    }

    public static DataMixinOp fromString(String opText) {
        return switch (opText) {
            case "add" -> ADD;
            case "remove" -> REMOVE;
            case "replace" -> REPLACE;
            default -> BASE;
        };
    }

    @Override
    public String asString() {
        return name;
    }

    public boolean isAdd() {
        return this == ADD;
    }
}
