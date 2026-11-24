package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.SlabBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** JY maintenance floor and ladder floor, both usable as climbable surfaces. */
public final class JYMaintenanceFloorBlock extends SlabBlock {
    public JYMaintenanceFloorBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, false);
        configureMultiCell(footprint);
    }

    @Override
    public boolean isLadder(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            LivingEntity entity
    ) {
        return true;
    }
}
