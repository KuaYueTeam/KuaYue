package cn.kycraft.kuayue.parts.core.panel.custom_rendered;

import cn.kycraft.kuayue.parts.core.panel.EndFaceShapes;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MeterCarriageEndFaceBlock extends CustomRenderedEndFaceBlock {

    /**
     * 本家族的端面框用原生方块元素模型建模，元素坐标即碰撞盒坐标，
     * 因此要抵消渲染器默认给端面框加的 1 格 Y 偏移。
     */
    private static final double FRAME_OFFSET_Y = -1.0D;

    private final boolean outside;

    public MeterCarriageEndFaceBlock(Properties properties, TrainPanelProperties.DoorType doorType, boolean outside) {
        // Without a door this block renders its own blockstate model, so the block entity must not draw a frame.
        super(properties, doorType, (String) null, null, null);
        this.outside = outside;
    }

    public MeterCarriageEndFaceBlock(Properties properties, TrainPanelProperties.DoorType doorType,
                                     String leftModel, String rightModel, String frameModel, boolean outside) {
        super(properties, doorType, leftModel, rightModel, frameModel, FRAME_OFFSET_Y);
        this.outside = outside;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (outside) {
            return EndFaceShapes.getEndFaceShape(
                    pState.getValue(FACING).getOpposite(),
                    DOOR_TYPE,
                    pState.getValue(OPEN)
            ).move(0, 0.5, 0);
        }
        return EndFaceShapes.getInsideEndFaceCloseShape(pState.getValue(FACING).getOpposite()).move(0, 0.5, 0);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (outside) {
            return EndFaceShapes.getEndFaceShape(
                    pState.getValue(FACING).getOpposite(),
                    DOOR_TYPE,
                    pState.getValue(OPEN)
            ).move(0, 0.5, 0);
        }
        return EndFaceShapes.getInsideEndFaceShape(
                pState.getValue(FACING).getOpposite(),
                pState.getValue(OPEN)
        ).move(0, 0.5, 0);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        if (DOOR_TYPE == TrainPanelProperties.DoorType.NO_DOOR) {
            return RenderShape.MODEL;
        }
        return super.getRenderShape(pState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        if (DOOR_TYPE == TrainPanelProperties.DoorType.NO_DOOR) {
            return InteractionResult.PASS;
        }
        return super.useWithoutItem(pState, pLevel, pPos, pPlayer, pHitResult);
    }
}
