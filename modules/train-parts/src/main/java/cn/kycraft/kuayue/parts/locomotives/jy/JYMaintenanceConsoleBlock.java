package cn.kycraft.kuayue.parts.locomotives.jy;

import lib.kasuga.rendering.models.mc.proxies.ProxyModelBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Directional JY maintenance console; its selection and collision are supplied by params.shape_bindings. */
public final class JYMaintenanceConsoleBlock extends ProxyModelBlock {
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public JYMaintenanceConsoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder.add(FACING));
    }
}
