package io.xk.worldgenpro.generator.structure.placement;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.structure.StructureTemplate;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 高性能结构放置器（快速版）。
 *
 * 设计原则：
 * 1) 先执行 processors（process(...)）得到最终的 StructureBlockInfo 列表；
 * 2) 按 Chunk 分组，直接在对应 Chunk (LevelChunk) 上调用底层 setBlockState(...)（不触发更新）；
 * 3) 收集带 NBT 的 BlockEntity 数据，全部写入到 Chunk 的 BE-NBT 存储（一次性注入）；
 * 4) 标记 chunk 脏并（可选）批量触发光照/邻居刷新（由调用方根据版本实现合适的刷新方式）。
 *
 * WARNING:
 * - 该类假设你处于结构生成阶段（即可以安全跳过即时的 neighbor/light/physics 处理）。
 * - 部分 API 调用在不同 MC 版本中命名不同（注释中标注了可能需要调整的位置）。
 */
public final class FastStructurePlacer {

    private FastStructurePlacer() {}

    /**
     * 快速放置结构（高性能替代 StructureTemplate.place() 的一部分功能）。
     *
     * @param template       结构模板
     * @param world          ServerWorldAccess/StructureWorldAccess（通常在 pool 生成时可用）
     * @param pos            放置偏移（世界坐标）
     * @param pivot          pivot（原版 place 里会用到）
     * @param placementData  placementData（旋转/镜像/processor list/ignore entities 等）
     * @param random         随机
     * @param flags          原始 flags（会被忽略掉对 world.setBlockState 的使用）
     *
     * @return true 如果放置了方块（至少一个）
     */
    public static boolean placeFast(StructureTemplate template,
                                    ServerWorldAccess world,
                                    BlockPos pos,
                                    BlockPos pivot,
                                    StructurePlacementData placementData,
                                    Random random,
                                    int flags) {
        // 0) 快速预检（与原版一致）
        if (template.blockInfoLists.isEmpty()) return false;

        List<StructureTemplate.StructureBlockInfo> chosen = placementData.getRandomBlockInfos(template.blockInfoLists, pos).getAll();
        if ((chosen.isEmpty() && (placementData.shouldIgnoreEntities() || template.entities.isEmpty()))
                || template.getSize().getX() < 1 || template.getSize().getY() < 1 || template.getSize().getZ() < 1) {
            return false;
        }

        // 1) 运行 processors -> 得到最终要放的 blockInfos 列表（与原版 process 保持一致）
        List<StructureTemplate.StructureBlockInfo> processed = StructureTemplate.process(world, pos, pivot, placementData, chosen);

        // 2) 分组：把 processed 按 ChunkPos 分组，便于对每个 chunk 批量写入
        Map<ChunkPos, List<StructureTemplate.StructureBlockInfo>> byChunk = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            BlockPos bp = info.pos();
            ChunkPos cp = new ChunkPos(bp);
            byChunk.computeIfAbsent(cp, k -> new ArrayList<>()).add(info);
        }

        // 3) 收集要注入的 BlockEntity NBT（延后注入）
        Map<ChunkPos, Map<BlockPos, NbtCompound>> beNbtPerChunk = new HashMap<>();

        // 4) 对每个 chunk 执行批量写入
        for (Map.Entry<ChunkPos, List<StructureTemplate.StructureBlockInfo>> e : byChunk.entrySet()) {
            ChunkPos cp = e.getKey();
            List<StructureTemplate.StructureBlockInfo> infos = e.getValue();

            // 4.1 确保 chunk 已加载（如果没有加载，加载它；在生成阶段通常会已加载）
            //    API差异：某些版本使用 world.getChunk(x,z) 或 world.getChunkManager().getChunk(...)
            //    这里尝试先用 ServerWorldAccess 的常见方法，若你的版本不同请替换成正确的加载方法。
            net.minecraft.world.chunk.Chunk chunk;
            try {
                // 强制加载/获取 chunk（阻塞获取）
                chunk = ((net.minecraft.server.world.ServerWorld) world).getChunk(cp.x, cp.z);
                // 如果上面返回 null 或抛异常，请在你的版本替换为合适的加载调用，例如:
                // chunk = ((ServerWorld)world).getChunk(cp.x, cp.z);
            } catch (Exception ex) {
                // 作为稳妥退路，使用 getChunk (可能触发同步加载)
                chunk = ((net.minecraft.server.world.ServerWorld) world).getChunk(cp.x, cp.z);
            }
            if (chunk == null) {
                // 如果还是 null，跳过这个 chunk（理论上不该发生）
                continue;
            }

            // 4.2 对该 chunk 的每个 StructureBlockInfo 批量写入
            for (StructureTemplate.StructureBlockInfo info : infos) {
                BlockPos blockPos = info.pos();

                // bounding box 检查（如果 placementData 有 bounding box）
                BlockBox box = placementData.getBoundingBox();
                if (box != null && !box.contains(blockPos)) continue;

                // 读取并镜像/旋转 blockState（这一步成本低，可保留）
                BlockState targetState = info.state().mirror(placementData.getMirror()).rotate(placementData.getRotation());

                // 如果该方块携带 NBT（BlockEntity），收集 NBT 并延后注入
                if (info.nbt() != null) {
                    beNbtPerChunk.computeIfAbsent(cp, k -> new HashMap<>()).put(blockPos, info.nbt());
                    // 同时我们仍然需要把目标方块写入 chunk（以便世界里包含正确的 block）
                }

                // === 关键：直接向 chunk 写入 blockState，避免 world.setBlockState 的更新开销 ===
                // API note: LevelChunk / Chunk 中的方法随版本不同，常见方法有 chunk.setBlockState(BlockPos, BlockState, boolean)
                // 我们以 chunk.setBlockState(pos, state, false) 为目标（false 表示不触发外部更新）
                try {
                    // 下面调用在各版本上可能是 LevelChunk#setBlockState 或 Chunk#setBlockState
                    // 你可能需要将其替换为你当前目标版本的具体方法名。
                    chunk.setBlockState(blockPos, targetState, false);
                } catch (NoSuchMethodError nsme) {
                    // 如果方法签名不同，退回到更通用但慢的方式（最后手段）
                    ((net.minecraft.server.world.ServerWorld) world).setBlockState(blockPos, targetState, 2);
                }
            } // end infos loop

            // 4.3 标记 chunk 为脏（需要保存）
            try {
                // 常见方法：((LevelChunk)chunk).setNeedsSaving(true) 或 chunk.setUnsaved(true)
                // 下面尝试多种常用方法，实测时根据你版本注释/取消不匹配项
                try {
                    ((net.minecraft.world.chunk.Chunk) chunk).setNeedsSaving(true);
                } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Method m = chunk.getClass().getMethod("setUnsaved", boolean.class);
                    m.invoke(chunk, true);
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        } // end byChunk

        // 5) 批量注入 BlockEntity NBT（延后创建 BE）
        //    不同版本的 LevelChunk 提供不同注入方式：
        //    - LevelChunk#setBlockEntityNbt(BlockPos, CompoundTag)
        //    - LevelChunk#setBlockEntity(BlockPos, BlockEntity) （需要实例化）
        // 我优先尝试 setBlockEntityNbt；若不存在则回退到在 world 上创建 BE（较慢但兼容）
        for (Map.Entry<ChunkPos, Map<BlockPos, NbtCompound>> chunkEntry : beNbtPerChunk.entrySet()) {
            ChunkPos cp = chunkEntry.getKey();
            Map<BlockPos, NbtCompound> map = chunkEntry.getValue();
            net.minecraft.world.chunk.Chunk chunk;
            try {
                chunk = ((net.minecraft.server.world.ServerWorld) world).getChunk(cp.x, cp.z);
            } catch (Throwable t) {
                chunk = ((net.minecraft.server.world.ServerWorld) world).getChunk(cp.x, cp.z);
            }
            if (chunk == null) continue;

            for (Map.Entry<BlockPos, NbtCompound> beEntry : map.entrySet()) {
                BlockPos bePos = beEntry.getKey();
                NbtCompound nbt = beEntry.getValue();

                boolean injected = false;

                // 尝试 LevelChunk#setBlockEntityNbt
                try {
                    java.lang.reflect.Method setNbt = chunk.getClass().getMethod("setBlockEntityNbt", BlockPos.class, NbtCompound.class);
                    setNbt.invoke(chunk, bePos, nbt);
                    injected = true;
                } catch (Throwable ignored) {}

                if (!injected) {
                    // 退回：在 world 上创建 BlockEntity（慢）
                    try {
                        // 创建后再 readNbt（注意：readNbt 可能期望坐标正确）
                        net.minecraft.block.entity.BlockEntity be = ((net.minecraft.server.world.ServerWorld) world).getBlockEntity(bePos);
                        if (be != null) {
                            be.readNbt(nbt);
                        } else {
                            // 如果没有 BE，尝试生成一个
                            // 这里我们尝试使用 BlockEntityType#create 从方块状态创建实例（版本差异很大）
                            // 作为最后手段调用 world.setBlockState + world.getBlockEntity 组合（代价高）
                            // 以避免抛错，我们先跳过（记录日志）
                            // logger.warn("FastStructurePlacer: cannot inject BE NBT, your MC version may require different API hook.");
                        }
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            } // end per-be
        }

        // 6) （可选）批量光照与邻居刷新
        //    这里我不默认进行完整光照重算（因为各版本 API 差异大且成本可能高）。
        //    我会提供两种建议的处理策略，你按需启用：
        //
        //    A) 延后光照：在所有结构放置结束后，调用服务器的 lighting provider 的批量刷新接口（如果你的版本有的话）。
        //    B) 批量通知边界块：只对修改区块边界的方块进行一次 updateNeighbors 或 light check。
        //
        // 下面只是一个占位/提示（不做实际调用）。
        //
        // IMPORTANT: 如果你不进行任何光照刷新，你可能会在客户端看到黑块或光照不正确。
        //
        // 示例（伪代码，按你的 MC 版本替换）:
        // ((ServerWorld)world).getChunkManager().getLightingProvider().relightChunk(cp.x, cp.z); // 若存在
        //
        // 由于各版本差异，这部分交给你决定何时触发（例如：在整个区域全部写完后执行一次全区域 lighting recompute）。

        return true;
    }
}
