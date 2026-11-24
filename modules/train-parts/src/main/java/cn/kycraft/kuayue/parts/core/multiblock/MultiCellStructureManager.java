package cn.kycraft.kuayue.parts.core.multiblock;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Creates, reconciles and removes the invisible cells owned by a multi-cell anchor block. */
public final class MultiCellStructureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED_CONFLICTS = ConcurrentHashMap.newKeySet();

    private MultiCellStructureManager() {
    }

    /**
     * Returns the anchor plus every companion cell selected by the candidate state.
     */
    public static Set<BlockPos> targetPositions(
            BlockPos parentPos,
            BlockState parentState,
            MultiCellFootprint footprint
    ) {
        Set<BlockPos> targets = new LinkedHashSet<>();
        targets.add(parentPos.immutable());
        for (BlockPos offset : footprint.relativeCells(parentState)) {
            targets.add(parentPos.offset(offset));
        }
        return Set.copyOf(targets);
    }

    /**
     * Checks every cell that placement would occupy before the anchor is written to the world.
     */
    public static boolean canPlace(
            BlockGetter level,
            BlockPos parentPos,
            BlockState parentState,
            MultiCellFootprint footprint
    ) {
        return MultiCellPlacementPolicy.canPlace(
                parentPos,
                targetPositions(parentPos, parentState, footprint),
                pos -> {
                    BlockState state = level.getBlockState(pos);
                    return state.isAir()
                            || state.canBeReplaced()
                            || pos.equals(parentPos) && state.is(parentState.getBlock());
                },
                pos -> componentOwner(level, pos)
        );
    }

    public static void ensure(
            ServerLevel level,
            BlockPos parentPos,
            BlockState parentState,
            MultiCellFootprint footprint
    ) {
        if (footprint.isEmpty()) {
            return;
        }
        Set<BlockPos> desired = new HashSet<>();
        for (BlockPos offset : footprint.relativeCells(parentState)) {
            desired.add(parentPos.offset(offset));
        }
        removeStale(level, parentPos, footprint, desired);

        for (BlockPos companionPos : desired) {
            BlockState current = level.getBlockState(companionPos);
            BlockEntity currentEntity = level.getBlockEntity(companionPos);
            if (currentEntity instanceof MultiCellCompanionBlockEntity companion
                    && parentPos.equals(companion.getParentPos())) {
                WARNED_CONFLICTS.remove(conflictKey(level, companionPos));
                continue;
            }
            if (!current.isAir() && !current.canBeReplaced()) {
                warnConflict(level, parentPos, companionPos, current);
                continue;
            }
            if (currentEntity instanceof MultiCellCompanionBlockEntity) {
                warnConflict(level, parentPos, companionPos, current);
                continue;
            }
            BlockState companionState = MultiCellRegistry.MULTI_CELL_COMPANION
                    .getEntry()
                    .defaultBlockState();
            if (!level.setBlock(companionPos, companionState, 3, 512)) {
                continue;
            }
            BlockEntity entity = level.getBlockEntity(companionPos);
            if (entity instanceof MultiCellCompanionBlockEntity companion) {
                companion.setParentOffset(parentPos.subtract(companionPos));
                WARNED_CONFLICTS.remove(conflictKey(level, companionPos));
            }
        }
    }

    public static void removeOwned(ServerLevel level, BlockPos parentPos, MultiCellFootprint footprint) {
        for (BlockPos offset : footprint.allRelativeCells()) {
            removeOwnedAt(level, parentPos, parentPos.offset(offset));
        }
    }

    private static void removeStale(
            ServerLevel level,
            BlockPos parentPos,
            MultiCellFootprint footprint,
            Set<BlockPos> desired
    ) {
        for (BlockPos offset : footprint.allRelativeCells()) {
            BlockPos companionPos = parentPos.offset(offset);
            if (!desired.contains(companionPos)) {
                removeOwnedAt(level, parentPos, companionPos);
            }
        }
    }

    private static BlockPos componentOwner(BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof MultiCellCompanionBlockEntity companion
                ? companion.getParentPos()
                : null;
    }

    private static void removeOwnedAt(ServerLevel level, BlockPos parentPos, BlockPos companionPos) {
        BlockEntity entity = level.getBlockEntity(companionPos);
        if (entity instanceof MultiCellCompanionBlockEntity companion
                && parentPos.equals(companion.getParentPos())) {
            level.removeBlock(companionPos, false);
            WARNED_CONFLICTS.remove(conflictKey(level, companionPos));
        }
    }

    private static void warnConflict(
            ServerLevel level,
            BlockPos parentPos,
            BlockPos companionPos,
            BlockState occupant
    ) {
        String key = conflictKey(level, companionPos);
        if (WARNED_CONFLICTS.add(key)) {
            LOGGER.warn(
                    "[multi_cell] skipped companion at {} for {} because the cell is occupied by {}",
                    companionPos,
                    parentPos,
                    occupant
            );
        }
    }

    private static String conflictKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "@" + pos.asLong();
    }
}
