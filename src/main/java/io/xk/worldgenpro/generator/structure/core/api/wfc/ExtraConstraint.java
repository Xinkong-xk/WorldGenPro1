package io.xk.worldgenpro.generator.structure.core.api.wfc;

import io.xk.worldgenpro.generator.structure.core.api.Cell;
import io.xk.worldgenpro.generator.structure.core.api.Dir;

@FunctionalInterface
public interface ExtraConstraint {
    boolean allowConnection(Cell<StructureTile> c1, Cell<StructureTile> c2, Dir d);
}
