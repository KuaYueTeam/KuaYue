package cn.kycraft.kuayue.parts.core.panel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class HeadBlock extends FullShapeDirectionalBlock {
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 1);

    public HeadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEIGHT);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(HEIGHT), 1);
        }
        return InteractionResult.SUCCESS;
    }
}
