package cn.kycraft.kuayue.content.gameplay.food.fluid;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 可乐, using the 1.0 Source / Flowing split pattern.
 */
public abstract class ColaFluid extends BaseFlowingFluid {

    protected ColaFluid(Properties properties) {
        super(properties);
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder.add(LEVEL));
    }

    @Override
    public boolean isSource(FluidState state) {
        return state.getType() instanceof ColaFluid && !(state.getType() instanceof Flowing);
    }

    @Override
    public int getAmount(FluidState state) {
        return isSource(state) ? 8 : state.getValue(LEVEL);
    }

    public static class Source extends ColaFluid {
        public Source(Properties properties) {
            super(properties);
        }
    }

    public static class Flowing extends ColaFluid {
        public Flowing(Properties properties) {
            super(properties);
        }
    }
}
