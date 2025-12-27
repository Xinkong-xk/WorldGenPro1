package io.xk.worldgenpro.mixin.vanilla.enhance.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.vanilla.enhance.config.Input_Int_IntList_Str_StrList_To_Int;
import io.xk.worldgenpro.vanilla.enhance.config.SuperLargeConfig;
import io.xk.worldgenpro.vanilla.enhance.structure.generator.VanillaStructurePoolBasedGenerator;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.alias.StructurePoolAliasBinding;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * Mixin for JigsawStructure to support random maxDistanceFromCenter range.
 */
@Mixin(JigsawStructure.class)
public abstract class JigsawStructureMixin extends Structure{

    protected JigsawStructureMixin(Config config) {
        super(config);
    }

    @Shadow @Final @Mutable
    public static Codec<JigsawStructure> CODEC;

    @Shadow
    @Final
    public RegistryEntry<StructurePool> startPool;

    @Shadow
    @Final
    public Optional<Identifier> startJigsawName;

    @Shadow
    @Final
    public int size;

    @Shadow
    @Final
    public HeightProvider startHeight;

    @Shadow
    @Final
    public boolean useExpansionHack;

    @Shadow
    @Final
    public Optional<Heightmap.Type> projectStartToHeightmap;

    @Shadow
    @Final
    private List<StructurePoolAliasBinding> poolAliasBindings;

    @Unique
    private Input_Int_IntList_Str_StrList_To_Int maxDistanceConfig = null;

    @Unique
    private Input_Int_IntList_Str_StrList_To_Int sizeConfig = null;

    // superLargeConfig
    @Unique
    private SuperLargeConfig superLargeConfig;

    // facing相关配置
    @Unique
    private Biome facingBiome = null;

    @Unique
    private Direction direction = null;

    @Unique
    private String facingText = null;

    @Unique
    private static final HashSet<String> directions = new HashSet<>(8);
    static {
        directions.add(Direction.NORTH.getName());
        directions.add(Direction.SOUTH.getName());
        directions.add(Direction.EAST.getName());
        directions.add(Direction.WEST.getName());
    }

    static {
        // 重写 CODEC，仍然序列化原来的 int 字段，校验逻辑可以在生成时处理
        // 重写 CODEC，支持原来的 int 字段 + 可选序列化 IntRange
        CODEC = Codecs.validate(
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        configCodecBuilder(instance),
                        StructurePool.REGISTRY_CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                        Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                        Input_Int_IntList_Str_StrList_To_Int.CODEC
                                .fieldOf("size")
                                .forGetter(s -> ((JigsawStructureMixin) (Object) s).sizeConfig),
                        HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                        Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                        Heightmap.Type.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                        Input_Int_IntList_Str_StrList_To_Int.CODEC
                                .fieldOf("max_distance_from_center")
                                .forGetter(s -> ((JigsawStructureMixin) (Object) s).maxDistanceConfig),
                        Codec.STRING.optionalFieldOf("facing","").forGetter(s -> ((JigsawStructureMixin) (Object) s).facingText),
                        SuperLargeConfig.CODEC
                                .optionalFieldOf("super_large", new SuperLargeConfig(false, 128))
                                .forGetter(s -> ((JigsawStructureMixin) (Object) s).superLargeConfig),
                        Codec.list(StructurePoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(s -> s.getPoolAliasBindings())
                ).apply(instance, JigsawStructureMixin::create)),
                JigsawStructureMixin::validateCustom
        ).codec();
    }

    @Unique
    private static JigsawStructure create(
            Config config,
            RegistryEntry<StructurePool> startPool,
            Optional<Identifier> startJigsawName,
            Input_Int_IntList_Str_StrList_To_Int sizeConfig,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Type> projectStartToHeightmap,
            Input_Int_IntList_Str_StrList_To_Int maxDistanceConfig,
            String facingText,
            SuperLargeConfig superLargeConfig, // 新增参数:superLargeConfig用于判断一个结构是否是超大型结构
            List<StructurePoolAliasBinding> poolAliasBindings
    ) {
        JigsawStructure structure = new JigsawStructure(
                config,
                startPool,
                startJigsawName,
                8, // 写入一个默认值 8
                startHeight,
                useExpansionHack,
                projectStartToHeightmap,
                80, // 写入一个默认值 80
                poolAliasBindings
        );
        JigsawStructureMixin mixin = (JigsawStructureMixin) (Object) structure;
        mixin.maxDistanceConfig = maxDistanceConfig; // 设置 maxDistanceConfig 字段
        mixin.sizeConfig = sizeConfig; // 设置 sizeConfig 字段
        mixin.superLargeConfig = superLargeConfig; // 设置 superLargeConfig 字段
        mixin.facingText = facingText; // 设置朝向
        return structure;
    }

    /**
     * 构建朝向数据
     * @param server
     */
    @Unique
    private void buildBiome(MinecraftServer server){
        if (this.facingText != null &&  !this.facingText.isEmpty()) {
            // 获取面向的群系或者方向
            if (directions.contains(this.facingText.toLowerCase(Locale.getDefault()))){
                this.direction = Direction.byName(this.facingText.toLowerCase(Locale.getDefault()));
            }else{
                this.facingBiome = server.getRegistryManager().get(RegistryKeys.BIOME).get(new Identifier(this.facingText));
            }
        }
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/gen/structure/Structure$Config;" +
                    "Lnet/minecraft/registry/entry/RegistryEntry;" +
                    "Ljava/util/Optional;ILnet/minecraft/world/gen/heightprovider/HeightProvider;" +
                    "ZLjava/util/Optional;ILjava/util/List;)V",
            at = @At("RETURN")
    )
    private void onInit(
            Config config,
            RegistryEntry<StructurePool> startPool,
            Optional<Identifier> startJigsawName,
            int size,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Type> projectStartToHeightmap,
            int maxDistanceFromCenter,
            List<StructurePoolAliasBinding> poolAliasBindings,
            CallbackInfo ci
    ) {
        // 正常外部实例化的情况下则默认为单个int的配置项类型,也就是固定大小
        this.maxDistanceConfig = Input_Int_IntList_Str_StrList_To_Int.ofSingleInt(maxDistanceFromCenter);
        this.sizeConfig = Input_Int_IntList_Str_StrList_To_Int.ofSingleInt(size);
        this.superLargeConfig = new SuperLargeConfig(false,128); // superLargeConfig 默认为 (false,128)
        this.facingText = null; // 如果为空则认为随机
    }

    // 校验逻辑保持原样，只验证值 > 0
    @Unique
    private static DataResult<JigsawStructure> validateCustom(JigsawStructure structure) {
        return structure.maxDistanceFromCenter < 0
                ? DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128")
                : DataResult.success(structure);
    }

    // 拦截 getStructurePosition，替换 maxDistanceFromCenter
    /**
     * @author xk
     * @reason
     */
    @Overwrite
    public Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunkPos = context.chunkPos();
        int i = this.startHeight.get(context.random(), new HeightContext(context.chunkGenerator(), context.world()));
        BlockPos blockPos = new BlockPos(chunkPos.getStartX(), i, chunkPos.getStartZ());

        // 构建参数列表
        LinkedHashMap<String,Object> args = new LinkedHashMap<>(1);
        args.put("test",120);

        // 计算距离
        int distance = maxDistanceConfig.evaluate(
                context.random(),
                WorldGenPro.expressionManager,
                args
        );

        // 计算大小
        int size = sizeConfig.evaluate(
                context.random(),
                WorldGenPro.expressionManager,
                args
        );

        // 返回生成结果
        return VanillaStructurePoolBasedGenerator.generate(
                context,
                this.startPool,
                this.startJigsawName,
                size, // 替换原版值
                blockPos,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                distance, // 替换原版值
                StructurePoolAliasLookup.create(this.poolAliasBindings, blockPos, context.seed()),
                (JigsawStructure)(Object)this
        );
    }

}
