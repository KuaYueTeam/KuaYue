package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** JY maintenance side panel with left/right proxy model variants. */
public final class JYMaintenancePanelBlock extends TrainPanelProxyBlock {
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoorHingeSide> HINGE =
            BlockStateProperties.DOOR_HINGE;

    public JYMaintenancePanelBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HINGE, DoorHingeSide.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder.add(HINGE));
    }

}
