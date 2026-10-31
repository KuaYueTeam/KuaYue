package cn.kycraft.kuayue.content.gameplay.food.fluid;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

/**
 * 蓝牛 source block.
 */
public class BlueBullBlock extends LiquidBlock {

    public BlueBullBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LEVEL, 8);
    }
}
