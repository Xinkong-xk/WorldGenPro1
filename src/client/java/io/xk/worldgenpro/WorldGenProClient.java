package io.xk.worldgenpro;

import io.xk.worldgenpro.api.CommonRegister;
import io.xk.worldgenpro.network.ClientNetworkListener;
import net.fabricmc.api.ClientModInitializer;

import static io.xk.worldgenpro.WorldGenPro.commonRegisters;

public class WorldGenProClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

        ClientNetworkListener.register();

        commonRegisters.stream().forEach(CommonRegister::registerClient);
	}
}