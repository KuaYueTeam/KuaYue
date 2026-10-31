package cn.kycraft.kuayue.content.gameplay.food.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

/**
 * 可乐 source block. Bubbles constantly, like the 1.0 implementation.
 */
public class ColaBlock extends LiquidBlock {

    public ColaBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LEVEL, 8);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        spawnParticles(level, pos, random);
    }

    private static void spawnParticles(Level level, BlockPos pos, RandomSource random) {
        double dx = 0.5 * random.nextFloat();
        double dy = 0.5 * random.nextFloat();
        double dz = 0.5 * random.nextFloat();
        level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                pos.getX() + 0.4 + dx, pos.getY() + 1.05 + dy, pos.getZ() + 0.4 + dz,
                0.0D, 0.1D, 0.0D);
    }
}
