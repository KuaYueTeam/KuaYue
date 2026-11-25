package cn.kycraft.kuayue.overhead_line.block.support.variants;

import cn.kycraft.kuayue.overhead_line.types.OverheadLineType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public record OverheadLineBlockDynamicConfiguration(
        ConnectionPointBuilder connectionPoints,
        Predicate<OverheadLineType> typePredicator,
        List<ResourceLocation> renderTypes,
        int maxConnections
) {
    public static  interface ConnectionPointBuilder{
        public List<Vec3> get(Level level, BlockPos blockPos , BlockState blockState);
    }
}
