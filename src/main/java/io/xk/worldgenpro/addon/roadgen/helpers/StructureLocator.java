package io.xk.worldgenpro.addon.roadgen.helpers;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.xk.worldgenpro.addon.roadgen.RoadGen;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class StructureLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadGen.MOD_ID);
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(
            id -> Text.translatable("commands.locate.structure.invalid", id)
    );
    private static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(
            id -> Text.translatable("commands.locate.structure.not_found", id)
    );

    public static void locateConfiguredStructure(ServerWorld serverWorld,String structureToLocate ,int locateCount, boolean locateAtPlayer) {
        LOGGER.debug("Locating " + locateCount + " " + structureToLocate);
        try {
            for (int x = 0; x < locateCount; x++) {
                if (locateAtPlayer) {
                    for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                        executeLocateStructure(player.getBlockPos(), serverWorld, new RegistryPredicateArgumentType<>(RegistryKeys.STRUCTURE).parse(new StringReader(structureToLocate)));
                    }
                }
                else {
                    executeLocateStructure(serverWorld.getSpawnPos(), serverWorld, new RegistryPredicateArgumentType<>(RegistryKeys.STRUCTURE).parse(new StringReader(structureToLocate)));
                }
            }
        } catch (CommandSyntaxException e) {
            LOGGER.warn("Failed to locate pool: " + structureToLocate + " in dimension " + serverWorld.getRegistryKey().getValue() + " with exception: " + e.getMessage());
        }
    }

    private static void executeLocateStructure(BlockPos locatePos, ServerWorld serverWorld, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException {
//        Registry<Structure> registry = serverWorld.getRegistryManager().get(RegistryKeys.STRUCTURE);
//        RegistryEntryList<Structure> registryEntryList = (RegistryEntryList<Structure>)getStructureListForPredicate(predicate, registry)
//                .orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asString()));
//        Pair<BlockPos, RegistryEntry<Structure>> pair = WorldGenProStructureLocator.locateStructure(
//                serverWorld, registryEntryList, locatePos, 100, true,null);
//        if (pair == null) {
//            throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.asString());
//        } else {
//            BlockPos structureLocation = pair.getFirst();
//            LOGGER.debug("Structure found at " + structureLocation);
//            serverWorld.getAttachedOrCreate(WorldDataAttachment.STRUCTURE_LOCATIONS,()->new Records.StructureLocationData(List.of())).addStructure(structureLocation);
//        }
    }

    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureListForPredicate(
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> structureRegistry
    ) {
        return predicate.getKey().map(key -> structureRegistry.getEntry(key).map(entry -> RegistryEntryList.of(entry)), structureRegistry::getEntryList);
    }
}
