package cn.kycraft.kuayue.parts.core.panel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FreightEndFaceBlock extends TrainPanelBlock {

    private final FreightType freightType;

    public FreightEndFaceBlock(Properties properties, FreightType freightType) {
        super(properties);
        this.freightType = freightType;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (freightType) {
            case C70 -> EndFaceShapes.getC70EndFaceShape(pState.getValue(FACING).getOpposite());
            case NX70 -> EndFaceShapes.getNX70EndFaceShape(pState.getValue(FACING).getOpposite());
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }

    public enum FreightType {
        C70, NX70
    }
}
