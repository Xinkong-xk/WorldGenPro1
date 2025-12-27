package io.xk.worldgenpro.addon.roadgen.resource;


import io.xk.worldgenpro.addon.roadgen.RoadGen;
import io.xk.worldgenpro.addon.roadgen.config.ModConfig;
import io.xk.worldgenpro.addon.roadgen.events.ModEventHandler;
import io.xk.worldgenpro.addon.roadgen.features.config.RoadFeatureRegistry;
import io.xk.worldgenpro.addon.roadgen.persistence.attachments.WorldDataAttachment;
import io.xk.worldgenpro.api.CommonRegister;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;


public class RoadGenRegister implements CommonRegister {

    @Override
	public void registerServer() {
		LOGGER.info("[RoadGen] Initializing Settlement Roads... (By settlement-roads)");
		WorldDataAttachment.registerWorldDataAttachment();
		ModConfig.init(RoadGen.MOD_ID, ModConfig.class);
		RoadFeatureRegistry.registerFeatures();
		ModEventHandler.register();
	}
}