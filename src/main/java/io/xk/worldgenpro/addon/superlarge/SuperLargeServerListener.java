package io.xk.worldgenpro.addon.superlarge;

public class SuperLargeServerListener {
    public static void registerListeners() {
        // 服务器关闭时保存超大型结构生成预测期
//        ServerLifecycleEvents.SERVER_STOPPING.register(new Identifier("worldgenpro:super_large_structure_prediction_save"),
//                server -> {
//                    if (WorldGenPro.superLargeStructurePredictionManager != null) {
//                        // 保存超大型结构生成预测器
//                        WorldGenPro.LOGGER.info("Saving SuperLargeStructurePredictionManager");
//                        SuperLargeStructurePredictionDat.save(
//                                WorldGenPro.superLargeStructurePredictionManager,
//                                server.getRegistryManager().get(RegistryKeys.STRUCTURE),
//                                server.getSavePath(WorldSavePath.ROOT).resolve(SuperLargeStructurePredictionManager.FILE_NAME)
//                        );
//                    }
//                }
//        );
//
//        // 服务器启动时加载超大型结构生成预测器
//        ServerLifecycleEvents.SERVER_STARTING.register(new Identifier("worldgenpro:super_large_structure_prediction_load"),
//                server -> {
//                    // 创建超大型结构生成预测器
//                    WorldGenPro.LOGGER.info("Loading SuperLargeStructurePredictionManager");
//                    WorldGenPro.superLargeStructurePredictionManager = new SuperLargeStructurePredictionManager(
//                            server.getSavePath(WorldSavePath.ROOT).resolve(SuperLargeStructurePredictionManager.FILE_NAME),
//                            server.getRegistryManager().get(RegistryKeys.STRUCTURE)
//                    );
//                    // 加载文件
//                    SuperLargeStructurePredictionDat.load(
//                            WorldGenPro.superLargeStructurePredictionManager,
//                            server.getRegistryManager().get(RegistryKeys.STRUCTURE),
//                            server.getSavePath(WorldSavePath.ROOT).resolve(SuperLargeStructurePredictionManager.FILE_NAME)
//                    );
//
//                    // 搜索并且加载所有超大型结构
//                    Registry<StructureSet> structureSetRegistry = server.getRegistryManager().get(RegistryKeys.STRUCTURE_SET);
//                    for (StructureSet structureSet : structureSetRegistry) { // 这里直接是 Iterable
//                        for (StructureSet.WeightedEntry structureEntry : structureSet.structures()) {
//                            Structure pool = structureEntry.pool().value();
//                            // 如果是JigsawStructure
//                            if (pool instanceof JigsawStructure jigsawStructure){
//                                // 获取超大型结构配置
//                                SuperLargeConfig superLargeConfig = JigsawStructureReflect.getSuperLargeConfig(jigsawStructure);
//                                if (superLargeConfig != null) {
//                                    // 如果是超大型结构
//                                    if (superLargeConfig.isSuperLarge()){
//                                        WorldGenPro.LOGGER.info("Find Super Large Structure: " + structureEntry.pool().getKey().get() + " - [scan range] : " + superLargeConfig.isSuperLarge());
//                                        WorldGenPro.superLargeStructurePredictionManager.addSuperLargeStructure(jigsawStructure,superLargeConfig,server);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//        );
    }
}
