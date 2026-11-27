package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** JY maintenance tail bumper with a model-derived collision profile. */
public final class JYMaintenanceTailBlock extends TrainPanelProxyBlock {
    public JYMaintenanceTailBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

}
