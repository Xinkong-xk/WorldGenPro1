package io.xk.worldgenpro.generator.structure.core.api.wfc;

import io.xk.worldgenpro.generator.structure.core.api.Dir;
import io.xk.worldgenpro.generator.structure.core.api.Tile;

import java.util.EnumSet;

public class StructureTile extends Tile {

    public final EnumSet<Dir> connections;

    public String nbtFile;

    public StructureTile(String id, String nbtFile, double weight, EnumSet<Dir> connections) {
        super(id, weight);
        this.connections = connections;
        this.nbtFile = nbtFile;
    }
}
