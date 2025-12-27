package io.xk.worldgenpro.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;

/**
 * 独立的网络/客户端动作工具类。
 * 所有 server -> client 的请求都集中放在这里。
 */
public final class ServerNetworkSender {

    public static final Identifier OPEN_JIGSAW_SCREEN_PACKET = new Identifier("worldgenpro", "open_jigsaw_screen");

    private ServerNetworkSender() {}

    /**
     * 服务器请求客户端打开 jigsaw 界面。
     */
    public static void sendOpenJigsawScreen(ServerPlayerEntity player, BlockPos pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ServerPlayNetworking.send(player, OPEN_JIGSAW_SCREEN_PACKET, buf);
    }
}
