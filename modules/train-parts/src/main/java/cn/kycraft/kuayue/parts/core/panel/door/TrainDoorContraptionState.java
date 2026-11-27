package cn.kycraft.kuayue.parts.core.panel.door;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.EndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.company.CompanyTrainPanel;
import cn.kycraft.kuayue.utils.DirectionUtil;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** One authoritative operation for manual and automatic doors, entirely in captured coordinates. */
final class TrainDoorContraptionState {
    private TrainDoorContraptionState() {
    }

    static boolean setOpen(AbstractContraptionEntity entity, BlockPos pos, boolean open, @Nullable Player player) {
        Contraption contraption = entity.getContraption();
        if (entity.level().isClientSide() || contraption == null) {
            return false;
        }
        var info = contraption.getBlocks().get(pos);
        if (info == null || !OpenableTrainDoor.isDoor(info.state().getBlock())) {
            return false;
        }
        var door = (OpenableTrainDoor) info.state().getBlock();
        if (!info.state().hasProperty(door.doorOpenProperty())) {
            return false;
        }
        BlockState updated = info.state().setValue(door.doorOpenProperty(), open);
        boolean changed = updated != info.state();
        if (changed) {
            entity.setBlock(pos, new StructureBlockInfo(info.pos(), updated, info.nbt()));
        }
        boolean companionChanged = false;
        if (updated.getBlock() instanceof TrainPanelBlock panel) {
            Direction facing = updated.getValue(TrainPanelBlock.FACING);
            boolean left = isLeftHinge(updated);
            BlockPos first = DirectionUtil.left(pos, facing, (int) (left ? -panel.beginPos.x : panel.beginPos.x))
                    .above((int) panel.beginPos.y);
            int width = (int) (panel.endPos.x - panel.beginPos.x);
            int height = (int) (panel.endPos.y - panel.beginPos.y);
            for (int x = 0; x < width; x++) {
                BlockPos column = DirectionUtil.right(first, facing, left ? x : -x);
                for (int y = 0; y < height; y++) {
                    BlockPos cell = column.above(y);
                    if (pos.equals(companyParent(contraption, cell))) {
                        companionChanged |= updateCompany(entity, cell, open);
                    }
                }
            }
        }
        if (changed || companionChanged) {
            contraption.invalidateColliders();
        }
        if (changed) {
            BlockPos soundPos = BlockPos.containing(entity.toGlobalVector(Vec3.atCenterOf(pos), 1f));
            entity.level().levelEvent(null,
                    open ? TrainDoorBlock.getOpenSound(updated) : TrainDoorBlock.getCloseSound(updated), soundPos, 0);
            entity.level().gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, soundPos);
        }
        return changed;
    }

    /** Legacy company BEs store a relative parent offset in their captured NBT. */
    static @Nullable BlockPos companyParent(Contraption contraption, BlockPos pos) {
        var info = contraption.getBlocks().get(pos);
        if (info == null || !(info.state().getBlock() instanceof CompanyTrainPanel)) {
            return null;
        }
        var tag = info.nbt();
        if (tag == null || !tag.contains("px", Tag.TAG_INT) || !tag.contains("py", Tag.TAG_INT)
                || !tag.contains("pz", Tag.TAG_INT) || !tag.contains("direction", Tag.TAG_STRING)) {
            return null;
        }
        int x = tag.getInt("px"), y = tag.getInt("py"), z = tag.getInt("pz");
        if ((x == 0 && y == 0 && z == 0)
                || Math.abs((long) x) > MultiCellFootprint.MAX_HORIZONTAL_DISTANCE
                || Math.abs((long) z) > MultiCellFootprint.MAX_HORIZONTAL_DISTANCE
                || Math.abs((long) y) > MultiCellFootprint.MAX_VERTICAL_DISTANCE) {
            return null;
        }
        Direction oldFacing = Direction.byName(tag.getString("direction").toLowerCase(Locale.ROOT));
        if (oldFacing == null || oldFacing.getAxis().isVertical()) {
            return null;
        }
        BlockPos offset = DirectionUtil.rotate(new BlockPos(x, y, z), oldFacing,
                info.state().getValue(CompanyTrainPanel.FACING));
        BlockPos parentPos = pos.offset(offset);
        var parent = contraption.getBlocks().get(parentPos);
        if (parent == null || !OpenableTrainDoor.isDoor(parent.state().getBlock())
                || !(parent.state().getBlock() instanceof TrainPanelBlock panel)
                || !ownsCompanyCell(panel, parent.state(), parentPos, pos)) {
            return null;
        }
        return parentPos;
    }

    static boolean updateCompany(AbstractContraptionEntity entity, BlockPos pos, boolean open) {
        var info = entity.getContraption().getBlocks().get(pos);
        if (info == null || !info.state().hasProperty(CompanyTrainPanel.OPEN)
                || info.state().getValue(CompanyTrainPanel.OPEN) == open) {
            return false;
        }
        entity.setBlock(pos, new StructureBlockInfo(info.pos(), info.state().setValue(CompanyTrainPanel.OPEN, open), info.nbt()));
        return true;
    }

    private static boolean ownsCompanyCell(TrainPanelBlock panel, BlockState state, BlockPos parent, BlockPos cell) {
        if (cell.equals(parent) || panel instanceof EndFaceBlock && cell.equals(parent.above())) {
            return false;
        }
        BlockPos relative = DirectionUtil.rotate(cell.subtract(parent), state.getValue(TrainPanelBlock.FACING), Direction.NORTH);
        boolean left = isLeftHinge(state);
        int firstX = (int) (left ? -panel.beginPos.x : panel.beginPos.x);
        int column = left ? firstX - relative.getX() : relative.getX() - firstX;
        int row = relative.getY() - (int) panel.beginPos.y;
        return relative.getZ() == 0 && column >= 0 && column < (int) (panel.endPos.x - panel.beginPos.x)
                && row >= 0 && row < (int) (panel.endPos.y - panel.beginPos.y);
    }

    private static boolean isLeftHinge(BlockState state) {
        return !state.hasProperty(BlockStateProperties.DOOR_HINGE)
                || state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT;
    }
}
