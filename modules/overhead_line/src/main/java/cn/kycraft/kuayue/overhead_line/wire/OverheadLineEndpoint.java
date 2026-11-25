package cn.kycraft.kuayue.overhead_line.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record OverheadLineEndpoint(ResourceKey<Level> dimension , BlockPos blockPos , int pointIndex) {
    public OverheadLineEndpoint{blockPos = blockPos.immutable();}
}

