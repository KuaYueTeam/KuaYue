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

    private final boolean outside;

    public MeterCarriageEndFaceBlock(Properties properties, TrainPanelProperties.DoorType doorType, boolean outside) {
        super(properties, doorType, (String) null, null, "carriage/carriage_m1/end_face/m1_middle_end_face_frame");
        this.outside = outside;
    }

    public MeterCarriageEndFaceBlock(Properties properties, TrainPanelProperties.DoorType doorType,
                                     String leftModel, String rightModel, String frameModel, boolean outside) {
        super(properties, doorType, leftModel, rightModel, frameModel);
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
