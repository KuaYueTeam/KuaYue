package cn.kycraft.kuayue.parts.core.multiblock;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** Validates a captured companion's ownership entirely within one contraption. */
final class MultiCellContraptionParent {
    private MultiCellContraptionParent() {
    }

    static @Nullable BlockPos resolve(Contraption contraption, BlockPos companionPos) {
        var companion = contraption.getBlocks().get(companionPos);
        if (companion == null || !(companion.state().getBlock() instanceof MultiCellCompanionBlock)) {
            return null;
        }
        BlockPos offset = readOffset(companion.nbt());
        if (offset == null) {
            return null;
        }
        BlockPos parentPos = companionPos.offset(offset);
        var parent = contraption.getBlocks().get(parentPos);
        if (parent == null || !(parent.state().getBlock() instanceof TrainPanelBlock panel)
                || !panel.multiCellFootprint().relativeCells(parent.state())
                .contains(companionPos.subtract(parentPos))) {
            return null;
        }
        return parentPos;
    }

    private static @Nullable BlockPos readOffset(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains("parent_x", Tag.TAG_INT)
                || !tag.contains("parent_y", Tag.TAG_INT) || !tag.contains("parent_z", Tag.TAG_INT)) {
            return null;
        }
        int x = tag.getInt("parent_x");
        int y = tag.getInt("parent_y");
        int z = tag.getInt("parent_z");
        if ((x == 0 && y == 0 && z == 0)
                || Math.abs((long) x) > MultiCellFootprint.MAX_HORIZONTAL_DISTANCE
                || Math.abs((long) z) > MultiCellFootprint.MAX_HORIZONTAL_DISTANCE
                || Math.abs((long) y) > MultiCellFootprint.MAX_VERTICAL_DISTANCE) {
            return null;
        }
        return new BlockPos(x, y, z);
    }
}
