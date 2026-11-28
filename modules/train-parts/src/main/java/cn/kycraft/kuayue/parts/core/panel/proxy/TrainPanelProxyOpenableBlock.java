package cn.kycraft.kuayue.parts.core.panel.proxy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Proxy panel host whose content authors one closed and one open model.
 *
 * <p>The {@code open} property is the only extra state: it selects the model group through
 * {@code proxy_model_be.params.model_by_state} and the collision profile through
 * {@code params.aabb} ({@code facing=...,open=false} / {@code facing=...,open=true}). Interaction
 * mirrors the openable window family — an empty-hand or non-panel-item click flips the state on
 * both sides and plays the vanilla metal open/close cue.
 */
public class TrainPanelProxyOpenableBlock extends TrainPanelProxyBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public TrainPanelProxyOpenableBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(OPEN));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(OPEN, false);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return TrainSmallWindowBlock.windowUse(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        // Holding any panel item keeps the normal placement path; only a bare/other click toggles.
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof TrainPanelBlock) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        TrainSmallWindowBlock.windowUse(state, level, pos, player, hand, hitResult);
        return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }
}
