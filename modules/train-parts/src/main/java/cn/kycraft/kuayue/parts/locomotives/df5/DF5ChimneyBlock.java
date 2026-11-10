package cn.kycraft.kuayue.parts.locomotives.df5;

import cn.kycraft.kuayue.parts.locomotives.df11.DF11GChimneyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class DF5ChimneyBlock extends DF11GChimneyBlock {
    private record DirectionProperties(double xOffset, double zOffset, double xSpeed, double zSpeed) {}

    private static final Map<Direction, DirectionProperties> PROPERTIES = Map.of(
            Direction.NORTH, new DirectionProperties(-0.1F, -0.3F, 0.0F, -0.05F),
            Direction.EAST, new DirectionProperties(1.3F, -0.1F, 0.05F, 0.0F),
            Direction.SOUTH, new DirectionProperties(1.1F, 1.3F, 0.0F, 0.05F),
            Direction.WEST, new DirectionProperties(-0.3F, 1.1F, -0.05F, 0.0F)
    );

    public DF5ChimneyBlock(Properties properties, boolean isCarport) {
        super(properties, isCarport);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        DirectionProperties properties = PROPERTIES.getOrDefault(
                state.getValue(FACING),
                new DirectionProperties(0.0F, 0.0F, 0.0F, 0.0F)
        );
        for (int i = 0; i < 2; i++) {
            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + properties.xOffset(),
                    pos.getY() + 1.2F,
                    pos.getZ() + properties.zOffset(),
                    properties.xSpeed(),
                    0.2F,
                    properties.zSpeed()
            );
        }
    }
}
