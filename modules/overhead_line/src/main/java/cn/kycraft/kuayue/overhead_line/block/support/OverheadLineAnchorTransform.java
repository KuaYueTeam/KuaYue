package cn.kycraft.kuayue.overhead_line.block.support;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class OverheadLineAnchorTransform {
    public static Vec3 toWorld(BlockPos pos , Direction facing , Vec3 local){
        double x = local.x;
        double y = local.y;
        double z = local.z;

        Vec3 offest = switch (facing){
            case EAST -> new Vec3(x,y,-z);
            case SOUTH -> new Vec3( z, y,  x);
            case WEST  -> new Vec3(-x, y,  z);
            case NORTH -> new Vec3(-z, y, -x);
            default -> throw new IllegalArgumentException("非预期水平面朝向" + facing);
        };
        return new Vec3(
                pos.getX() + 0.5 + offest.x,
                pos.getY()+offest.y,
                pos.getZ() + 0.5 + offest.z());
    }
}
