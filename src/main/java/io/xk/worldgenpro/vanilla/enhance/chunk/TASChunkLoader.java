package io.xk.worldgenpro.vanilla.enhance.chunk;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import net.minecraft.world.chunk.ChunkStatus;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * TAS 辅助加载工具，输入 List<ChunkPos> 异步加载
 */
public class TASChunkLoader {

    /**
     * 异步加载一组离散 chunk
     *
     * @param world           ServerWorld
     * @param chunkPositions  需要加载的 ChunkPos 列表
     * @param requiredStatus  要求的 ChunkStatus
     * @return CompletableFuture 完成时返回每个 chunk 的 Either
     */
    public static CompletableFuture<List<Either<Chunk, ChunkHolder.Unloaded>>> loadChunksAsync(
            ServerWorld world,
            List<ChunkPos> chunkPositions,
            ChunkStatus requiredStatus
    ) {
        ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
        ThreadedAnvilChunkStorage tas = chunkManager.threadedAnvilChunkStorage;

        List<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> futures = new ArrayList<>();

        for (ChunkPos pos : chunkPositions) {
            ChunkHolder holder = tas.getCurrentChunkHolder(pos.toLong());

            CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future;

            if (holder != null) {
                // 已经有 holder，直接请求 chunk
                future = holder.getChunkAt(requiredStatus, tas);
            } else {
                // holder 不存在，触发 TAS 异步加载
                future = chunkManager.getChunkFuture(pos.x, pos.z, requiredStatus, true);
            }

            futures.add(future);
        }

        // 合并 futures，返回一个 CompletableFuture<List<Either<Chunk, Unloaded>>>
        return Util.combineSafe(futures);
    }

    /**
     * 同步等待并获取 chunk
     */
    public static List<Chunk> loadChunksSync(ServerWorld world, List<ChunkPos> chunkPositions, ChunkStatus requiredStatus) {
        List<Either<Chunk, ChunkHolder.Unloaded>> results = loadChunksAsync(world, chunkPositions, requiredStatus).join();
        List<Chunk> chunks = Lists.newArrayList();

        for (Either<Chunk, ChunkHolder.Unloaded> e : results) {
            Optional<Chunk> optional = e.left();
            if (optional.isEmpty()) {
                throw new IllegalStateException("Chunk " + e.right().get() + " 未能加载");
            }
            chunks.add(optional.get());
        }

        return chunks;
    }
}
