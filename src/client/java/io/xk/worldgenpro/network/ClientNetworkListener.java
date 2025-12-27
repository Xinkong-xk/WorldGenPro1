package io.xk.worldgenpro.network;


import io.xk.worldgenpro.structure.blocks.jigsaw.advance.AdvanceJigsawBlockScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class ClientNetworkListener {

    // 统一定义所有客户端监听的通道 ID
    public static final Identifier OPEN_JIGSAW_SCREEN = new Identifier("worldgenpro", "open_jigsaw_screen");

    // 注册所有客户端端的网络监听器
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OPEN_JIGSAW_SCREEN, (client, handler, buf, responseSender) -> {
            // 在客户端主线程执行 UI 打开逻辑
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> {
                if (client.world != null) {
                    var be = client.world.getBlockEntity(pos);
                    if (be instanceof io.xk.worldgenpro.generator.structure.blocks.jigsaw.advance.AdvanceJigsawBlockEntity jigsaw) {
                        MinecraftClient.getInstance().setScreen(new AdvanceJigsawBlockScreen(jigsaw));
                    }
                }
            });
        });
    }
}
