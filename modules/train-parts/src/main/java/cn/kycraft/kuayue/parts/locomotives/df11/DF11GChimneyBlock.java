package cn.kycraft.kuayue.parts.locomotives.df11;

import cn.kycraft.kuayue.parts.core.panel.HingeSlabBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class DF11GChimneyBlock extends HingeSlabBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public DF11GChimneyBlock(Properties properties, boolean isCarport) {
        super(properties, isCarport);
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, true);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            level.setBlock(context.getClickedPos(), state.cycle(LIT), 3);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            level.setBlock(context.getClickedPos(), state.cycle(BlockStateProperties.DOOR_HINGE), 3);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        Direction direction = state.getValue(FACING);
        for (int i = 0; i < 3; i++) {
            if (random.nextFloat() >= 0.5F) {
                continue;
            }
            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 0.5D,
                    direction.getAxis() == Direction.Axis.X ? 0.05F + random.nextFloat() / 10.0F : 0.0F,
                    0.2F,
                    direction.getAxis() == Direction.Axis.Z ? 0.05F + random.nextFloat() / 10.0F : 0.0F
            );
        }
    }
}
