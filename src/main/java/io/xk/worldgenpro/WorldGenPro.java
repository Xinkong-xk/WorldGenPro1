package io.xk.worldgenpro;

import io.xk.worldgenpro.addon.datamixin.DataMixinManager;
import io.xk.worldgenpro.addon.datamixin.config.DataMixinLoader;
import io.xk.worldgenpro.api.CommonRegister;
import io.xk.worldgenpro.command.CommandRegister;
import io.xk.worldgenpro.configfile.ConfigFile;
import io.xk.worldgenpro.core.configure.ConfigureManager;
import io.xk.worldgenpro.core.listener.BaseConfigureListeners;
import io.xk.worldgenpro.core.resource.CommonResourceListener;
import io.xk.worldgenpro.gx.expression.ExpressionManager;
import io.xk.worldgenpro.generator.structure.blocks.jigsaw.JigsawRegister;
import io.xk.worldgenpro.gx.expression.ExpressionServerListener;
import io.xk.worldgenpro.terrain.adaptation.TerrainAdaptorManager;
import io.xk.worldgenpro.test.TestCommandRegister;
import io.xk.worldgenpro.addon.roadgen.resource.RoadGenRegister;
import io.xk.worldgenpro.addon.roadgen.resource.RoadConfigLoader;
import io.xk.worldgenpro.addon.roadgen.RoadConnectionConfigManager;
import io.xk.worldgenpro.addon.semantic.GeoSemanticStructureManager;
import io.xk.worldgenpro.addon.semantic.GeoSemanticStructureServerListener;
import io.xk.worldgenpro.addon.superlarge.SuperLargeServerListener;
import io.xk.worldgenpro.addon.superlarge.SuperLargeStructurePredictionManager;
import io.xk.worldgenpro.vanilla.enhance.structure.checkConfig.CheckConfigListener;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xk.worldgenpro.addon.semantic.CapacityValue.BASE_BIG_CAPACITY;

public class WorldGenPro implements ModInitializer {

    // ID
    public static final String MOD_ID = "worldgenpro";

    // 输出日志
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 服务器实例
    public static net.minecraft.server.MinecraftServer minecraftServer = null;

    // 结构文件表
    public static Object2ObjectOpenHashMap<Identifier, NbtCompound> structuresNbtMap = new Object2ObjectOpenHashMap<>(BASE_BIG_CAPACITY);

    // 随机数
    public static Random random = Random.create();

    // 配置管理器
    public static ConfigureManager configureManager = new ConfigureManager();

    // 所有通用注册器
    public static ObjectArrayList<CommonRegister> commonRegisters = new ObjectArrayList<>(BASE_BIG_CAPACITY);

    // 表达式管理器实例
    public static ExpressionManager expressionManager = new ExpressionManager();

    // 存档配置文件
    public static ConfigFile configFile = null;

    // 地形适应管理器
    public static TerrainAdaptorManager terrainAdaptorManager = new TerrainAdaptorManager(16);

    // 超大型结构预测管理器
    public static SuperLargeStructurePredictionManager superLargeStructurePredictionManager = null;

    // 地理语义结构管理器
    public static GeoSemanticStructureManager geoSemanticStructureManager = new GeoSemanticStructureManager();

    // 道路生成配置管理器
    public static RoadConnectionConfigManager roadConnectionConfigManager = new RoadConnectionConfigManager();

    // DataMixin配置管理器
    public static DataMixinManager dataMixinManager = new DataMixinManager();

    // 通用资源加载监听器
    public static CommonResourceListener commonResourceListener = new CommonResourceListener();

    // 道路配置加载器
    public static RoadConfigLoader roadConfigLoader = new RoadConfigLoader();

    // DataMixin文件加载器
    public static DataMixinLoader dataMixinLoader = new DataMixinLoader();

    // 添加通用注册器
    static {
        // 注册道路生成器相关配置
        commonRegisters.add(new RoadGenRegister());
        // 注册测试指令
        commonRegisters.add(new TestCommandRegister());
        // 注册Jigsaw方块
        commonRegisters.add(new JigsawRegister());
        // 注册其他指令
        commonRegisters.add(new CommandRegister());
    }

    @Override
    public void onInitialize() {
        // 输出版本号
        LOGGER.info("WorldGenPro Version 0.0.1 alpha");

        // 2. 注册基础配置监听器 [暂时停用]
        BaseConfigureListeners.registerListeners();

        // 3. 注册所有通用注册器
        commonRegisters.forEach(CommonRegister::registerServer);

        // 5. 注册基础监听器
        registerBaseListeners();

        // 6. 注册其他监听器
        registerOtherListeners();
    }

    public void registerOtherListeners() {
        // 注册表达式脚本引擎初始化在服务器上的监听器
        ExpressionServerListener.registerListeners();

        // 注册超大型结构管理器在服务器上的监听器
        SuperLargeServerListener.registerListeners();

        // 注册地理语义结构管理器在服务器上的监听器
        GeoSemanticStructureServerListener.registerListeners();

        // 注册CheckConfig配置构建
        CheckConfigListener.registerListeners();
    }

    public void registerBaseListeners() {
        // 服务器关闭后释清理释放配置和资源
        ServerLifecycleEvents.SERVER_STOPPED.register(new Identifier("worldgenpro:base_listener_stopped"),
                server -> {
                    // 解绑服务器对象
                    minecraftServer = null;

                    // 保存全局配置文件
                    WorldGenPro.configFile.save();

                    // DataMixin配置管理器在服务器结束时清理缓存和记录
                    dataMixinManager.onServerStopping(server);

                    // 道路连接配置管理器在服务器结束时清理缓存和记录
                    roadConnectionConfigManager.onServerStopping(server);
                }
        );
        // 服务器启动时加载基础配置和资源
        ServerLifecycleEvents.SERVER_STARTING.register(new Identifier("worldgenpro:base_listener_starting"),
                server -> {
                    // 绑定服务器对象
                    minecraftServer = server;

                    // 加载全局配置文件
                    WorldGenPro.configFile = ConfigFile.loadFor(server);

                    // 加载DataMixin配置
                    dataMixinManager.onServerStarting(server);

                    // 加载通用资源配置
                    commonResourceListener.reload(server.getResourceManager());

                    // 道路连接配置管理器服务器启动时初始化,包括配置表初始化和从资源加载
                    roadConnectionConfigManager.onServerStarting(server);
                }
        );
    }
}