package io.xk.worldgenpro.addon.roadgen.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;

import java.util.ArrayList;
import java.util.List;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_SMALL_CAPACITY;

public class GridGraph {

    private final int spacing;

    /** cellKey -> pointId */
    public final Long2IntOpenHashMap cellToPoint = new Long2IntOpenHashMap(BASE_SMALL_CAPACITY);

    /** point arrays (1-based indexing) */
    private final LongArrayList xs = new LongArrayList(BASE_SMALL_CAPACITY);
    private final LongArrayList ys = new LongArrayList(BASE_SMALL_CAPACITY);
    private final IntArrayList cxs = new IntArrayList(BASE_SMALL_CAPACITY);
    private final IntArrayList cys = new IntArrayList(BASE_SMALL_CAPACITY);

    /** palette: pool name indexing */
    private final Object2IntOpenHashMap<String> paletteIndex = new Object2IntOpenHashMap<>(BASE_SMALL_CAPACITY);
    private final ObjectArrayList<String> palette = new ObjectArrayList<>(BASE_SMALL_CAPACITY);

    /** pool index for each point */
    private final IntArrayList structureIndex = new IntArrayList(BASE_SMALL_CAPACITY);

    private int nextId = 0;

    // ----------------- CONSTRUCTOR -----------------

    public GridGraph(int spacing) {
        if (spacing <= 0) throw new IllegalArgumentException("spacing must be > 0");
        this.spacing = spacing;
    }

    // ----------------- UTILITIES -----------------

    private int getOrCreatePaletteIndex(String structure) {
        int idx = paletteIndex.getOrDefault(structure, -1);
        if (idx != -1) return idx;

        idx = palette.size();
        paletteIndex.put(structure, idx);
        palette.add(structure);
        return idx;
    }

    public static long cellKey(int cx, int cy) {
        return ((long) cx << 32) | (cy & 0xffffffffL);
    }

    public int cx(long x) { return (int) Math.floor((double)x / spacing); }
    public int cy(long y) { return (int) Math.floor((double)y / spacing); }

    /** 返回所有 cellKey（按所有点顺序） */
    public long[] getAllCells() {
        long[] arr = new long[nextId];
        for (int i = 0; i < nextId; i++) {
            arr[i] = cellKey(cxs.getInt(i), cys.getInt(i));
        }
        return arr;
    }

    // ----------------- ADD POINT -----------------

    public int addPoint(long x, long y, String structure) {
        int cx = cx(x);
        int cy = cy(y);
        long key = cellKey(cx, cy);

        // 若已有点，直接返回
        if (cellToPoint.containsKey(key)) {
            int id0 = cellToPoint.get(key);
            if (id0 >= 0) return id0; // 真实点
        }

        int id = nextId++;

        xs.add(x);
        ys.add(y);
        cxs.add(cx);
        cys.add(cy);

        int structIdx = getOrCreatePaletteIndex(structure);
        structureIndex.add(structIdx);

        cellToPoint.put(key, id);
        return id;
    }

    public void markEmptyCell(int cx, int cy) {
        long key = cellKey(cx, cy);
        cellToPoint.putIfAbsent(key, -1);
    }

    public boolean hasVisitedCell(int cx, int cy) {
        return cellToPoint.containsKey(cellKey(cx, cy));
    }

    public boolean hasVisitedCell(long key) {
        return cellToPoint.containsKey(key);
    }

    public boolean isEmptyCell(int cx, int cy) {
        long key = cellKey(cx, cy);
        return cellToPoint.getOrDefault(key, -2) == -1;
    }

    // ----------------- QUERY -----------------

    public int[] getCellOfPoint(int id) {
        if (id < 0 || id >= nextId) return null;
        return new int[]{ cxs.getInt(id), cys.getInt(id) };
    }

    public String getStructureOfPoint(int id) {
        return palette.get(structureIndex.getInt(id));
    }

    public String getStructureInCell(int cx, int cy) {
        int id = getPointIdInCell(cx, cy);
        return id == -1 ? null : getStructureOfPoint(id);
    }

    public int getPointIdAt(long x, long y) {
        long key = cellKey(cx(x), cy(y));
        return cellToPoint.getOrDefault(key, -1);
    }

    public boolean hasPointInCell(int cx, int cy) {
        return getPointIdInCell(cx, cy) >= 0;
    }

    public int getPointIdInCell(int cx, int cy) {
        return cellToPoint.getOrDefault(cellKey(cx, cy), -1);
    }

    public long getXByCell(int cx, int cy) {
        int id = getPointIdInCell(cx, cy);
        return id == -1 ? Long.MIN_VALUE : xs.getLong(id);
    }

    public long getYByCell(int cx, int cy) {
        int id = getPointIdInCell(cx, cy);
        return id == -1 ? Long.MIN_VALUE : ys.getLong(id);
    }

    public long[] getPointCoordInCell(int cx, int cy) {
        int id = getPointIdInCell(cx, cy);
        if (id == -1) return null;
        return new long[]{ xs.getLong(id), ys.getLong(id) };
    }

    public boolean hasPointAt(long x, long y) {
        return cellToPoint.containsKey(cellKey(cx(x), cy(y)));
    }

    public long getX(int id) { return xs.getLong(id); }
    public long getY(int id) { return ys.getLong(id); }

    // ----------------- SERIALIZATION -----------------

    public GridGraphData toData() {

        List<Long> emptyCells = new ArrayList<>();
        cellToPoint.forEach((key, v) -> {
            if (v == -1) emptyCells.add(key);
        });

        return new GridGraphData(
                spacing,
                new ArrayList<>(xs),
                new ArrayList<>(ys),
                new ArrayList<>(cxs),
                new ArrayList<>(cys),
                nextId,
                new ArrayList<>(palette),
                new ArrayList<>(structureIndex),
                emptyCells
        );
    }

    // ------------------------------------------------------------------------

    public record GridGraphData(
            int spacing,
            List<Long> xs,
            List<Long> ys,
            List<Integer> cxs,
            List<Integer> cys,
            int nextId,
            List<String> palette,
            List<Integer> structureIndex,
            List<Long> emptyCells
    ) {

        public static final Codec<GridGraphData> DATA_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("spacing").forGetter(GridGraphData::spacing),
                        Codec.LONG.listOf().fieldOf("xs").forGetter(GridGraphData::xs),
                        Codec.LONG.listOf().fieldOf("ys").forGetter(GridGraphData::ys),
                        Codec.INT.listOf().fieldOf("cxs").forGetter(GridGraphData::cxs),
                        Codec.INT.listOf().fieldOf("cys").forGetter(GridGraphData::cys),
                        Codec.INT.fieldOf("next_id").forGetter(GridGraphData::nextId),
                        Codec.STRING.listOf().fieldOf("palette").forGetter(GridGraphData::palette),
                        Codec.INT.listOf().fieldOf("structure_index").forGetter(GridGraphData::structureIndex),
                        Codec.LONG.listOf().fieldOf("empty_cells").forGetter(GridGraphData::emptyCells)
                ).apply(instance, GridGraphData::new)
        );

        public GridGraph toGraph() {
            GridGraph g = new GridGraph(this.spacing);

            g.xs.addAll(this.xs);
            g.ys.addAll(this.ys);
            g.cxs.addAll(this.cxs);
            g.cys.addAll(this.cys);
            g.nextId = this.nextId;

            g.palette.addAll(this.palette);
            for (int i = 0; i < this.palette.size(); i++)
                g.paletteIndex.put(this.palette.get(i), i);

            g.structureIndex.addAll(this.structureIndex);

            // 重建 cellToPoint
            g.cellToPoint.clear();
            for (int id = 0; id < nextId; id++) {
                long key = cellKey(g.cxs.getInt(id), g.cys.getInt(id));
                g.cellToPoint.put(key, id);
            }

            // empty cells
            for (long key : emptyCells) {
                g.cellToPoint.put(key, -1);
            }

            return g;
        }
    }

    /** GridGraph Codec */
    public static final Codec<GridGraph> CODEC = GridGraphData.DATA_CODEC.xmap(
            GridGraphData::toGraph,
            GridGraph::toData
    );

    public static GridGraph decode(Dynamic<?> dynamic) {
        return CODEC.parse(dynamic).result().orElseThrow();
    }

    public static <T> com.mojang.serialization.DataResult<T> encodeToOps(com.mojang.serialization.DynamicOps<T> ops, GridGraph graph) {
        return CODEC.encodeStart(ops, graph);
    }

    public LongSet getAllCellSet(){
        return cellToPoint.keySet();
    }

    @Override
    public String toString() {
        return "GridGraph{" +
                "spacing=" + spacing +
                ", cellToPoint=" + cellToPoint +
                ", xs=" + xs +
                ", ys=" + ys +
                ", cxs=" + cxs +
                ", cys=" + cys +
                ", paletteIndex=" + paletteIndex +
                ", palette=" + palette +
                ", structureIndex=" + structureIndex +
                ", nextId=" + nextId +
                '}';
    }
}
