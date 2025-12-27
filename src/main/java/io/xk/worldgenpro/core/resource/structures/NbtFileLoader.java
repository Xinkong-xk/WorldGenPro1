package io.xk.worldgenpro.core.resource.structures;

import io.xk.worldgenpro.core.resource.api.ResourceLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Predicate;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;
import static io.xk.worldgenpro.WorldGenPro.structuresNbtMap;

public class NbtFileLoader implements ResourceLoader {

    @Override
    public Predicate<Identifier> getAllowedPathPredicate() {
        return path -> path.toString().endsWith(".nbt");
    }

    @Override
    public String getPath() {
        return "develop/structures";
    }

    @Override
    public void load(Map<Identifier, Resource> resourceMap) {
        LOGGER.info("[Nbt] Loading Nbt Files...");
        for(Identifier id : resourceMap.keySet()) {
            try(InputStream stream = resourceMap.get(id).getInputStream()) {
                // Consume the stream however you want, medium, rare, or well done.
                NbtCompound nbtCompound = NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes());
                structuresNbtMap.put(id,nbtCompound);
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred while loading resource json " + id.toString()+ e.toString());
            }
        }
        LOGGER.info("[Nbt] Load {} Nbt Files", structuresNbtMap.size());
    }
}
