package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** JY maintenance carport with the established lower-half collision and a proxy model. */
public final class JYMaintenanceCarportBlock extends TrainPanelProxyBlock {
    public JYMaintenanceCarportBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null
                ? null
                : state.setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockState rotated = state.cycle(FACING);
        if (!rotated.canSurvive(level, context.getClickedPos())) {
            return InteractionResult.PASS;
        }

        KineticBlockEntity.switchToBlockState(
                level,
                context.getClickedPos(),
                updateAfterWrenched(rotated, context)
        );
        if (level.getBlockState(context.getClickedPos()) != state) {
            IWrenchable.playRotateSound(level, context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }
}
