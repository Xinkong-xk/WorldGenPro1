package io.xk.worldgenpro.addon.roadgen.codec.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.addon.roadgen.RoadConnectionConfigManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

public class RoadEntry {
    public Point from;
    public CountablePoint to;
    public Map<String, CrossRule> cross;
    public List<Identifier> avoidBiomes;
    public Connection connection;

    // 实际找到的structureObject对象
    public StructureSet fromStructureSetObject = null;
    public StructureSet toStructureSetObject = null;

    public String fromStructureSetName;
    public String toStructureSetName;

    public String roadEntryKey;

    // 其他后置获取:cross
    public Map<Biome,CrossRule> otherCrossRules = new Object2ObjectOpenHashMap<>(8);
    // 对应默认道路规则
    public CrossRule mainCrossRule = null;

    public RoadEntry(
            Point from,
            CountablePoint to,
            Map<String,CrossRule> cross,
            List<Identifier> avoidBiomes,
            Connection connection
    ) {
        this.from = from;
        this.to = to;
        this.cross = cross;
        this.avoidBiomes = avoidBiomes;
        this.connection = connection;
        // 设置
        this.fromStructureSetName = this.from.structure_set().toString();
        this.toStructureSetName = this.to.structure_set().toString();
    }

    public Point getFrom() {
        return from;
    }

    public CountablePoint getTo() {
        return to;
    }

    public void setRoadEntryKey(String roadEntryKey) {
        this.roadEntryKey = roadEntryKey;
    }

    public Map<String, CrossRule> getCross() {
        return cross;
    }

    public List<Identifier> getAvoidBiomes(){
        return avoidBiomes;
    }

    public String getRoadEntryKey() {return roadEntryKey;}

    public Connection getConnection() {return connection;}

    public record Connection(int initialLocatingCount){
        public static final Codec<Connection> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("initial_locating_count",5).forGetter(Connection::initialLocatingCount)
                ).apply(instance, Connection::new)
        );
        // 默认
        public static final Connection DEFAULT = new Connection(5);

        @Override
        public @NotNull String toString() {
            return "Connection{" +
                    "initialLocatingCount=" + initialLocatingCount +
                    '}';
        }
    }

    public static final Codec<RoadEntry> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Point.CODEC.fieldOf("from").forGetter(RoadEntry::getFrom),
                    CountablePoint.CODEC.fieldOf("to").forGetter(RoadEntry::getTo),

                    Codec.unboundedMap(Codec.STRING, CrossRule.CODEC)
                            .optionalFieldOf("cross", Map.of())
                            .forGetter(RoadEntry::getCross),

                    Identifier.CODEC.listOf()
                            .optionalFieldOf("avoid_biomes", List.of())
                            .forGetter(RoadEntry::getAvoidBiomes),

                    Connection.CODEC
                            .optionalFieldOf("connection", Connection.DEFAULT)
                            .forGetter(RoadEntry::getConnection)
            ).apply(instance, RoadEntry::new));

    // 构建
    public void build(Registry<StructureSet> structureSetRegistry, RoadConnectionConfigManager roadConnectionConfigManager, MinecraftServer server){
        roadConnectionConfigManager.entries.add(this);

        Identifier fromStructureSetID = getFrom().structure_set();
        Identifier toStructureSetID = getTo().structure_set();
        StructureSet fromStructureSet = structureSetRegistry.get(fromStructureSetID);
        StructureSet toStructureSet = structureSetRegistry.get(toStructureSetID);
        // 检查是否成功获取
        if (fromStructureSet == null || toStructureSet == null){
            throw new RuntimeException("[RoadGen:Structure] Structure not found for " + this);
        }else{
            fromStructureSetObject = fromStructureSet;
            toStructureSetObject = toStructureSet;

            // 加入映射表
            roadConnectionConfigManager.stringToStructureSetMap.put(fromStructureSetID.toString(),fromStructureSet);
            roadConnectionConfigManager.stringToStructureSetMap.put(toStructureSetID.toString(),toStructureSet);

            // 加入缓存表,以from结构来添加
            roadConnectionConfigManager.startStructureSetMap.put(fromStructureSet, this);

            // 加入名称缓存表
            roadConnectionConfigManager.structureSetToPair.put(fromStructureSet,new Pair<>(fromStructureSetName,toStructureSetName));
            roadConnectionConfigManager.FromStructureSetNameToPair.put(fromStructureSetID.toString(),new Pair<>(fromStructureSetName,toStructureSetName));

            // 加入HashSet
            roadConnectionConfigManager.structureSetNamePairMap.add(new Pair<>(getFrom(),getTo()));

            // 遍历这个结构集,记录对应表
            for (StructureSet.WeightedEntry weightedEntry : fromStructureSet.structures()){
                roadConnectionConfigManager.structureToSetMap.put(weightedEntry.structure().value(), fromStructureSet);
            }

            // 构建其他配置项
            Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
            // 构建cross配置项
            for (String biomeString : cross.keySet()){
                CrossRule crossRule = cross.get(biomeString);
                if (! biomeString.equals("builtin:default")){
                    Biome crossBiome = biomeRegistry.get(new Identifier(biomeString));
                    if (crossBiome != null){
                        // 构建道路规则
                        crossRule.build(server);
                        otherCrossRules.put(crossBiome,crossRule);
                    }else{
                        throw new RuntimeException("[RoadGen:Entry.Cross:Key-Biome] Biome not found for " + biomeString);
                    }
                }else{
                    // 默认道路类型
                    this.mainCrossRule = crossRule;
                    // 构建道路规则
                    this.mainCrossRule.build(server);
                }
            }

            LOGGER.info("[RoadGen:Configure.Entry] load and build config: {} ",this);
        }
    }

    public Map<Biome, CrossRule> getOtherCrossRules() {
        return otherCrossRules;
    }

    public CrossRule getMainCrossRule() {
        return mainCrossRule;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "from=" + from +
                ", to=" + to +
                ", disallowBiomes=" + avoidBiomes +
                ", fromStructureObject=" + fromStructureSetObject +
                ", toStructureObject=" + toStructureSetObject +
                ", mainCrossRule=" + mainCrossRule +
                ", otherCrossRules=" + otherCrossRules +
                '}';
    }
}
