package cn.kycraft.kuayue.meals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 饮料罐方块：右键食用，可选持续冒气泡粒子。
 *
 * <p>{@code has_particle} 由 JSON 的 params 控制，蓝牛在 1.0 中就是关闭的。</p>
 */
public class ParticlesDrinkBlock extends PlacementFoodBlock {

    private final boolean hasParticle;

    public ParticlesDrinkBlock(Properties properties, FoodType foodType, boolean hasParticle) {
        super(properties, foodType);
        this.hasParticle = hasParticle;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hasParticle && !level.isClientSide) {
            spawnParticles(level, pos);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (hasParticle) {
            spawnParticles(level, pos);
        }
    }

    private static void spawnParticles(Level level, BlockPos pos) {
        RandomSource random = level.random;
        double d1 = 0.1 * random.nextFloat();
        double d2 = 0.1 * random.nextFloat();
        double d3 = 0.1 * random.nextFloat();
        level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                pos.getX() + 0.4 + d1, pos.getY() + 0.55 + d2, pos.getZ() + 0.4 + d3,
                0.0D, 0.1D, 0.0D);
    }
}
