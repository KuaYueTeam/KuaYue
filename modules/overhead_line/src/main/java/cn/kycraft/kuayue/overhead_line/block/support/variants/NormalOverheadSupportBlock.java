package cn.kycraft.kuayue.overhead_line.block.support.variants;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;

public class NormalOverheadSupportBlock extends Block {
    public NormalOverheadSupportBlock(BlockBehaviour.Properties properties){
        super(properties.noCollission());
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.EAST));
    }
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection());
    }

    protected BlockState rotate(BlockState state, Rotation rotation){
        return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));
    }

    protected BlockState mirror(BlockState state, Mirror mirror){
        return state.setValue(FACING,mirror.mirror(state.getValue(FACING)));
    }

    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
