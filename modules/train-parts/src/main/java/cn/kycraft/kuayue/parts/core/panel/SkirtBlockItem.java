package cn.kycraft.kuayue.parts.core.panel;

import cn.kycraft.kuayue.utils.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SkirtBlockItem extends PanelBlockItem {


    public SkirtBlockItem(TrainPanelBlock pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return super.canPlace(context, state);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        PlacePosContext placePosContext = getPlacePos(context);
        if (placePosContext.success != 0) {
            if (placePosContext.success == 1) {
                state = state.setValue(SkirtBlock.SKIRT_TYPE, TrainPanelProperties.SkirtType.LEFT);
            } else if (placePosContext.success == -1) {
                state = state.setValue(SkirtBlock.SKIRT_TYPE, TrainPanelProperties.SkirtType.RIGHT);
            }
        }
        return context.getLevel()
                .setBlock(placePosContext.pos, state, 11);
    }

    private PlacePosContext getPlacePos(BlockPlaceContext context) {
        if (context.getPlayer() == null || context.getPlayer().isShiftKeyDown())
            return new PlacePosContext(context.getClickedPos(), 0);
        BlockPos clicked = context.getClickedPos();
        Level level = context.getLevel();
        Direction direction = context.getClickedFace();
        if (direction == Direction.UP || direction == Direction.DOWN)
            return new PlacePosContext(context.getClickedPos(), 0);
        BlockPos parentPos = clicked.relative(direction.getOpposite());
        BlockState parent = level.getBlockState(parentPos);
        if (!parent.is(getBlock()))
            return new PlacePosContext(context.getClickedPos(), 0);
        BlockPos left = DirectionUtil.left(parentPos, direction, 1),
                right = DirectionUtil.right(parentPos, direction, 1);
        Vec3 leftLoc = DirectionUtil.centerOf(left),
                rightLoc = DirectionUtil.centerOf(right),
                clickLoc = context.getClickLocation();
        boolean leftHinge = clickLoc.distanceToSqr(leftLoc) <= clickLoc.distanceToSqr(rightLoc);
        BlockState leftState = level.getBlockState(left),
                rightState = level.getBlockState(right);
        if (leftHinge && !leftState.canBeReplaced()) {
            return new PlacePosContext(context.getClickedPos(), 0);
        }
        if (!leftHinge && !rightState.canBeReplaced()) {
            return new PlacePosContext(context.getClickedPos(), 0);
        }
        return new PlacePosContext(leftHinge ? left : right, leftHinge ? -1 : 1);
    }

    record PlacePosContext(BlockPos pos, int success){}

    public PanelBlockItem.Face getArrowDirection(BlockHitResult result) {
        BlockPos clicked = result.getBlockPos(),
                left = DirectionUtil.left(clicked, result.getDirection(), 1),
                right = DirectionUtil.right(clicked, result.getDirection(), 1);
        Vec3 clickLoc = result.getLocation(),
                leftLoc = DirectionUtil.centerOf(left),
                rightLoc = DirectionUtil.centerOf(right);
        return clickLoc.distanceToSqr(leftLoc) <= clickLoc.distanceToSqr(rightLoc) ?
                PanelBlockItem.Face.LEFT : PanelBlockItem.Face.RIGHT;
    }
}
