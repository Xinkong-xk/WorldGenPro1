package io.xk.worldgenpro.test.city.middle;

import io.xk.worldgenpro.generator.structure.core.LayerManager;
import io.xk.worldgenpro.test.api.TestGenerator;
import io.xk.worldgenpro.test.city.CityStructureTiles;
import io.xk.worldgenpro.test.city.CityTilesUtils;
import net.minecraft.util.math.BlockPos;

public class MiddleCityTest implements TestGenerator {

    public static MiddleCityTest instance = new MiddleCityTest();

    public void generate(com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> context) {
        BlockPos pos = BlockPos.ofFloored(context.getSource().getPosition());

        LayerManager layerManager = new LayerManager(30, 30, 0, pos);

        layerManager.start(context.getSource().getWorld(),((layerManager1, blockPos) -> {
            CityTilesUtils.prepare(layerManager1,blockPos, CityStructureTiles.city_middle,17,30,30);
        }));
    }

    @Override
    public TestGenerator getInstance() {
        return instance;
    }

}
