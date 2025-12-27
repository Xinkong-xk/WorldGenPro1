package io.xk.worldgenpro.addon.superlarge.nbt;

import io.xk.worldgenpro.addon.superlarge.RegionPrediction;
import io.xk.worldgenpro.addon.superlarge.SuperLargeStructurePredictionManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


/**例子:<br/>
 * Path file = world.getSavePath(WorldSavePath.ROOT).resolve("super_large_structure_pred.dat");
 *<br/>
 * SuperLargeStructurePredictionDat.save(manager, structureRegistry, file);
 *<br/>
 * SuperLargeStructurePredictionDat.load(manager, structureRegistry, file);
 */
public class SuperLargeStructurePredictionDat {

    /** -------------------------
     *   保存缓存
     * ------------------------- */
    public static void save(
            SuperLargeStructurePredictionManager manager,
            Registry<Structure> structureRegistry,
            Path savePath
    ) {
        try {
            Files.createDirectories(savePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for prediction cache", e);
        }

        NbtCompound root = new NbtCompound();
        NbtList structureList = new NbtList();

        for (var structureEntry : manager.cache.entrySet()) {
            Structure structure = structureEntry.getKey();

            Identifier id = structureRegistry.getId(structure);
            if (id == null) continue; // 理论上不会，但以防万一

            NbtCompound structureTag = new NbtCompound();
            structureTag.putString("pool", id.toString());

            NbtList dimList = new NbtList();

            for (var dimEntry : structureEntry.getValue().entrySet()) {
                RegistryKey<World> dim = dimEntry.getKey();

                NbtCompound dimTag = new NbtCompound();
                dimTag.putString("dimension", dim.getValue().toString());

                NbtList regionList = new NbtList();

                for (var regionEntry : dimEntry.getValue().entrySet()) {
                    long regionId = regionEntry.getKey();
                    RegionPrediction pred = regionEntry.getValue();

                    NbtCompound regionNbt = pred.toNbt();
                    regionNbt.putLong("id", regionId);

                    regionList.add(regionNbt);
                }

                dimTag.put("regions", regionList);
                dimList.add(dimTag);
            }

            structureTag.put("dimensions", dimList);
            structureList.add(structureTag);
        }

        root.put("structures", structureList);

        try {
            NbtIo.write(root, savePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save super large pool prediction cache", e);
        }
    }


    /** -------------------------
     *   加载缓存
     * ------------------------- */
    public static void load(
            SuperLargeStructurePredictionManager manager,
            Registry<Structure> structureRegistry,
            Path savePath
    ) {
        if (!Files.exists(savePath)) return;

        NbtCompound root;
        try {
            root = NbtIo.read(savePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load super large pool prediction cache", e);
        }

        NbtList structureList = root.getList("structures", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < structureList.size(); i++) {
            NbtCompound structureTag = structureList.getCompound(i);

            Identifier sid = new Identifier(structureTag.getString("pool"));
            Structure structure = structureRegistry.get(sid);
            if (structure == null) continue;

            NbtList dimList = structureTag.getList("dimensions", NbtElement.COMPOUND_TYPE);

            for (int j = 0; j < dimList.size(); j++) {
                NbtCompound dimTag = dimList.getCompound(j);

                RegistryKey<World> dim = RegistryKey.of(
                        RegistryKeys.WORLD,
                        new Identifier(dimTag.getString("dimension"))
                );

                NbtList regionList = dimTag.getList("regions", NbtElement.COMPOUND_TYPE);

                Map<Long, RegionPrediction> regionMap =
                        manager.getRegionMap(structure, dim);

                for (int k = 0; k < regionList.size(); k++) {
                    NbtCompound r = regionList.getCompound(k);

                    long id = r.getLong("id");
                    RegionPrediction prediction = RegionPrediction.fromNbt(r);

                    regionMap.put(id, prediction);
                }
            }
        }
    }
}
