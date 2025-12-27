package io.xk.worldgenpro.generator.structure.blocks.jigsaw.advance;

import com.mojang.serialization.MapCodec;
import io.xk.worldgenpro.WorldGenPro;
import io.xk.worldgenpro.network.ServerNetworkSender;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.block.enums.Orientation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class AdvanceJigsawBlock extends Block implements BlockEntityProvider, OperatorBlock {
    public static final MapCodec<AdvanceJigsawBlock> CODEC = createCodec(AdvanceJigsawBlock::new);
    public static final EnumProperty<Orientation> ORIENTATION = Properties.ORIENTATION;

    @Override
    public MapCodec<AdvanceJigsawBlock> getCodec() {
        return CODEC;
    }

    public AdvanceJigsawBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ORIENTATION, Orientation.NORTH_UP));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(ORIENTATION, rotation.getDirectionTransformation().mapJigsawOrientation(state.get(ORIENTATION)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(ORIENTATION, mirror.getDirectionTransformation().mapJigsawOrientation(state.get(ORIENTATION)));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction direction = ctx.getSide();
        Direction direction2;
        if (direction.getAxis() == Direction.Axis.Y) {
            direction2 = ctx.getHorizontalPlayerFacing().getOpposite();
        } else {
            direction2 = Direction.UP;
        }

        return this.getDefaultState().with(ORIENTATION, Orientation.byDirections(direction, direction2));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AdvanceJigsawBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // 服务端逻辑
        if (!world.isClient) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be != null) {
                    // 向客户端发送打开 Jigsaw 界面的请求包
                    ServerNetworkSender.sendOpenJigsawScreen(serverPlayer, pos);
                }
            }
            return ActionResult.CONSUME;
        }

        // 客户端只返回 SUCCESS（避免继续传递事件）
        return ActionResult.SUCCESS;
    }

    public static boolean attachmentMatches(StructureTemplate.StructureBlockInfo info1, StructureTemplate.StructureBlockInfo info2) {
        Direction direction = getFacing(info1.state());
        Direction direction2 = getFacing(info2.state());
        Direction direction3 = getRotation(info1.state());
        Direction direction4 = getRotation(info2.state());
        JigsawBlockEntity.Joint joint = (JigsawBlockEntity.Joint)JigsawBlockEntity.Joint.byName(info1.nbt().getString("joint"))
                .orElseGet(() -> direction.getAxis().isHorizontal() ? JigsawBlockEntity.Joint.ALIGNED : JigsawBlockEntity.Joint.ROLLABLE);
        boolean bl = joint == JigsawBlockEntity.Joint.ROLLABLE;
        return direction == direction2.getOpposite() && (bl || direction3 == direction4) && info1.nbt().getString("target").equals(info2.nbt().getString("name"));
    }

    public static Direction getFacing(BlockState state) {
        return ((Orientation)state.get(ORIENTATION)).getFacing();
    }

    public static Direction getRotation(BlockState state) {
        return ((Orientation)state.get(ORIENTATION)).getRotation();
    }
}
