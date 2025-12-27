package io.xk.worldgenpro.generator.structure.blocks.jigsaw;

import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.api.CommonRegister;
import io.xk.worldgenpro.generator.structure.blocks.jigsaw.advance.AdvanceJigsawBlock;
import io.xk.worldgenpro.generator.structure.blocks.jigsaw.advance.AdvanceJigsawBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class JigsawRegister implements CommonRegister {

    // =============================
    // Identifier 定义
    // =============================
    public static final Identifier ADV_JIGSAW_ID = new Identifier(WorldGenPro.MOD_ID, "advance_jigsaw");
    public static final Identifier ADV_JIGSAW_CHANNEL = new Identifier(WorldGenPro.MOD_ID, "update_advance_jigsaw");
    public static final Identifier ADV_JIGSAW_GENERATE_CHANNEL = new Identifier(WorldGenPro.MOD_ID, "generate_advance_jigsaw");

    // =============================
    // 注册对象
    // =============================
    public static Block ADV_JIGSAW_BLOCK;
    public static BlockEntityType<AdvanceJigsawBlockEntity> ADV_JIGSAW_BLOCK_ENTITY;

    @Override
    public void registerServer() {
        // 注册方块
        ADV_JIGSAW_BLOCK = Registry.register(
                Registries.BLOCK,
                ADV_JIGSAW_ID,
                new AdvanceJigsawBlock(Blocks.JIGSAW.getSettings())
        );

        // 注册方块实体
        ADV_JIGSAW_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                ADV_JIGSAW_ID,
                FabricBlockEntityTypeBuilder.create(AdvanceJigsawBlockEntity::new, ADV_JIGSAW_BLOCK).build()
        );

        Registry.register(
                Registries.ITEM,
                ADV_JIGSAW_ID,
                new OperatorOnlyBlockItem(ADV_JIGSAW_BLOCK, new Item.Settings().rarity(Rarity.EPIC))
        );

        // 注册服务器网络通道
        registerNetworking();
    }

    @Override
    public void registerClient() {
        // 客户端不需要注册 ScreenHandler
        // 客户端 GUI 逻辑由 AdvanceJigsawBlockScreen 自己调用 ClientPlayNetworking 发送数据包
    }

    private void registerNetworking() {
        // 更新方块属性数据包
        ServerPlayNetworking.registerGlobalReceiver(ADV_JIGSAW_CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    var pos = buf.readBlockPos();
                    var name = buf.readIdentifier();
                    var target = buf.readIdentifier();
                    var poolId = buf.readIdentifier(); // 读 Identifier
                    String finalState = buf.readString(32767);
                    String jointName = buf.readString(32767);
                    int selectionPriority = buf.readVarInt();
                    int placementPriority = buf.readVarInt();

                    server.execute(() -> {
                        if (!player.isCreativeLevelTwoOp()) return;
                        var world = player.getWorld();
                        if (!(world.getBlockEntity(pos) instanceof AdvanceJigsawBlockEntity be)) return;

                        var joint = AdvanceJigsawBlockEntity.Joint.byName(jointName)
                                .orElse(AdvanceJigsawBlockEntity.Joint.ALIGNED);

                        be.setName(name);
                        be.setTarget(target);
                        be.setPool(RegistryKey.of(RegistryKeys.TEMPLATE_POOL, poolId)); // 转换成 RegistryKey
                        be.setFinalState(finalState);
                        be.setJoint(joint);
                        be.setPlacementPriority(placementPriority);
                        be.setSelectionPriority(selectionPriority);
                        be.markDirty();
                        world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_ALL);
                    });
                });


                                // 生成结构数据包
        ServerPlayNetworking.registerGlobalReceiver(
                ADV_JIGSAW_GENERATE_CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    var pos = buf.readBlockPos();
                    int depth = buf.readVarInt();
                    boolean keepJigsaws = buf.readBoolean();

                    server.execute(() -> {
                        if (!player.isCreativeLevelTwoOp()) return;
                        if (player.getWorld().getBlockEntity(pos) instanceof AdvanceJigsawBlockEntity entity) {
                            entity.generate(player.getServerWorld(), depth, keepJigsaws);
                        }
                    });
                }
        );
    }
}
