package cn.kycraft.kuayue.overhead_line.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record OverheadLineEndpoint(ResourceKey<Level> dimension , BlockPos blockPos , int pointIndex) {}

