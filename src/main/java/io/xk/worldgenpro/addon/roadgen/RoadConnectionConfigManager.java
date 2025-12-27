package io.xk.worldgenpro.addon.roadgen;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.addon.roadgen.api.GridGraph;
import io.xk.worldgenpro.addon.roadgen.codec.config.CountablePoint;
import io.xk.worldgenpro.addon.roadgen.codec.config.RoadEntry;
import io.xk.worldgenpro.addon.roadgen.codec.config.Point;
import io.xk.worldgenpro.addon.roadgen.api.data.RegionStructureMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.Map;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;
import static io.xk.worldgenpro.addon.roadgen.persistence.attachments.WorldDataAttachment.REGION_STRUCTURE_MAP;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;
import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_SMALL_CAPACITY;

public class RoadConnectionConfigManager {

    public final Object2ObjectOpenHashMap<String, RoadEntry> configMap = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    public final Object2ObjectOpenHashMap<StructureSet, RoadEntry> startStructureSetMap = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // 结构集HashSet
    public final ObjectOpenHashSet<RoadEntry> entries = new ObjectOpenHashSet<>(BASE_SMALL_CAPACITY);

    // 结构集与其配置项to和from
    public final Object2ObjectOpenHashMap<StructureSet, Pair<String,String>> structureSetToPair = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // from结构集名称与其配置项to和from
    public final Object2ObjectOpenHashMap<String, Pair<String,String>> FromStructureSetNameToPair = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // to和from的集合对
    public final ObjectOpenHashSet<Pair<Point, CountablePoint>> structureSetNamePairMap = new ObjectOpenHashSet<>(BASE_SMALL_CAPACITY);

    // 结构集名称与结构集对象对应表
    public final Object2ObjectOpenHashMap<String,StructureSet> stringToStructureSetMap = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // 结构与结构集对应表
    public final Object2ObjectOpenHashMap<Structure,StructureSet> structureToSetMap = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // 结构池名称与结构池对象映射表
    public final Object2ObjectOpenHashMap<String,StructurePool> structurePoolMap = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);

    // 结构区域记录表
    public final Object2ObjectOpenHashMap<RegistryKey<World>, RegionStructureMap> regionStructureMap = new Object2ObjectOpenHashMap<>(BASE_SMALL_CAPACITY);

    // 结构模板名称与结构模板对象映射表
    public final Object2ObjectOpenHashMap<String,StructureTemplate> structureTemplateMap = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);

    // 结构集注册项
    public Registry<StructureSet> structureSetRegistry;

    // 结构池注册项
    public Registry<StructurePool> structurePoolRegistry;

    // 需要跳过的区块表,暂时不使用
    public final LongOpenHashSet skipChunks = new LongOpenHashSet();

    /**
     * 清理和重置缓存表与映射表
     */
    public void reset(){
        // 清理缓存表和映射表
        configMap.clear();
        startStructureSetMap.clear();
        entries.clear();
        structureSetToPair.clear();
        FromStructureSetNameToPair.clear();
        structureSetNamePairMap.clear();
        structureTemplateMap.clear();
        regionStructureMap.clear();
        structurePoolMap.clear();
        structureToSetMap.clear();
        stringToStructureSetMap.clear();
        // 设置Null
        structureSetRegistry = null;
        structurePoolRegistry = null;
    }

    /**
     * 服务器关闭时
     * @param server
     */
    public void onServerStopping(MinecraftServer server) {
        LOGGER.info("[RoadGen:Configure] Process Server Stopping...");

        // 保存世界持久化数据
        LOGGER.info("[RoadGen:Data] Process World Data Saving...");
        server.getWorlds().forEach(world -> {
            RegistryKey<World> key = world.getRegistryKey();
            if (regionStructureMap.containsKey(key)) {
                LOGGER.info("[RoadGen:Data] Save Data In World {}",key);
                RegionStructureMap rsm = regionStructureMap.get(key);
                world.setAttached(REGION_STRUCTURE_MAP, rsm);
            }else{
                LOGGER.info("[RoadGen:Data] Cound Not Found Data In World {}, Create New Data",key);
                world.setAttached(REGION_STRUCTURE_MAP, new RegionStructureMap());
            }
        });

        // NOTE 最后再清理和重置缓存表与映射表
        reset();
    }

    /**
     * 服务器启动时
     * @param server
     */
    public void onServerStarting(MinecraftServer server) {
        LOGGER.info("[RoadGen:Configure] Loading...");

        // NOTE 先清理和重置缓存表与映射表
        reset();

        structureSetRegistry = server.getRegistryManager().get(RegistryKeys.STRUCTURE_SET);
        structurePoolRegistry = server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
        WorldGenPro.roadConfigLoader.reload(server.getResourceManager(),server);
    }

    public void put(String id, RoadEntry roadEntry, MinecraftServer server) {
        // 放入配置表里
        configMap.put(id, roadEntry);
        // 设置roadEntryKey
        roadEntry.setRoadEntryKey(id);

        // 查找结构加入startStructureMap里
        roadEntry.build(structureSetRegistry,this,server);
    }

    public static long getRegionId(ChunkPos chunkPos, int spacing) {
        int regionX = toR(chunkPos.x, spacing);
        int regionZ = toR(chunkPos.z, spacing);
        return (((long) regionX) << 32) | (regionZ & 0xffffffffL);
    }

    public static long getRegionId(int chunkX, int chunkZ, int spacing) {
        int regionX = toR(chunkX, spacing);
        int regionZ = toR(chunkZ, spacing);
        return (((long) regionX) << 32) | (regionZ & 0xffffffffL);
    }

    public static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xffffffffL);
    }

    public static int toR(int c,int spacing){
        return  Math.floorDiv(c, spacing);
    }

    public Map<String, RoadEntry> getAll() {
        return configMap;
    }

    public RoadEntry getRoadEntry(String id) {
        return configMap.get(id);
    }

    public Pair<String,String> getName(StructureSet structureSet) {
        return structureSetToPair.get(structureSet);
    }

    public RegionStructureMap getRegionStructureMap(RegistryKey<World> worldRegistryKey) {
        if (!regionStructureMap.containsKey(worldRegistryKey)) {
            regionStructureMap.put(worldRegistryKey, new RegionStructureMap());
        }
        return regionStructureMap.get(worldRegistryKey);
    }

    public GridGraph getGridGraph(RegistryKey<World> worldRegistryKey,String structureSet) {
        RegionStructureMap rsm = regionStructureMap.get(worldRegistryKey);
        return rsm.getGridGraph(structureSet,this.getStructureSet(structureSet));
    }

    public StructureSet getStructureSet(String structureSet) {
        if (! stringToStructureSetMap.containsKey(structureSet)) {
            stringToStructureSetMap.put(structureSet,structureSetRegistry.get(new Identifier(structureSet)));
        }
        return stringToStructureSetMap.get(structureSet);
    }

    public StructureTemplate getStructureTemplate(String structureTemplateName) {
        return structureTemplateMap.computeIfAbsent(
                structureTemplateName,
                s -> WorldGenPro.minecraftServer.getStructureTemplateManager().getTemplateOrBlank(new Identifier(structureTemplateName))
        );
    }

    public StructurePool getStructurePool(String structurePoolName) {
        return structurePoolMap.computeIfAbsent(
                structurePoolName,
                s -> getStructurePool(this.structurePoolRegistry,structurePoolName)
        );
    }

    public StructurePool getStructurePool(Registry<StructurePool> structurePoolRegistry, String pool) {
        var s = structurePoolRegistry.getEntry(RegistryKey.of(RegistryKeys.TEMPLATE_POOL,new Identifier(pool)));
        if (s.isPresent()){
            return s.get().value();
        } else {
            throw new RuntimeException("[RoadGen:Entry.Cross.CrossRule<build-StructurePool>] : pool not found :"+pool);
        }
    }
}
