package cn.kycraft.kuayue.parts.core.panel.custom_rendered;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelShapes;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DoubleRotateDoorBlock extends CustomRenderedEndFaceBlock {

    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;

    public DoubleRotateDoorBlock(Properties properties, TrainPanelProperties.DoorType doorType,
                                 String leftModel, String rightModel, String frameModel) {
        super(properties, doorType, leftModel, rightModel, frameModel);
        registerDefaultState(this.getStateDefinition().any()
                .setValue(HINGE, DoorHingeSide.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(HINGE));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext)
                .setValue(HINGE, TrainOpenableWindowBlock.getHinge(pContext));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING).getOpposite();
        DoorHingeSide hinge = pState.getValue(HINGE);
        return TrainPanelShapes.getDoubleRotateDoorCloseShape(hinge, direction);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        boolean open = pState.getValue(BlockStateProperties.OPEN);
        Direction direction = pState.getValue(FACING).getOpposite();
        DoorHingeSide hinge = pState.getValue(HINGE);
        return TrainPanelShapes.getDoubleRotateDoorShape(open, hinge, direction);
    }
}
