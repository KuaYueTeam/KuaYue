package cn.kycraft.kuayue.parts.core.panel.proxy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.jetbrains.annotations.Nullable;

/**
 * Panel host that declared a hinge side so one block can carry a right-hand and a left-hand
 * model. Placement sets the hinge from the side of the clicked face, `proxy_model_be` picks the
 * model through {@code model_by_state}, and {@code params.aabb} carries a profile per hinge
 * value — so no separate "…左" block is needed.
 */
public final class TrainPanelProxyHingeBlock extends TrainPanelProxyBlock {
    public TrainPanelProxyHingeBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(BlockStateProperties.DOOR_HINGE));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null
                ? null
                : state.setValue(BlockStateProperties.DOOR_HINGE,
                        TrainOpenableWindowBlock.getHinge(context));
    }
}
