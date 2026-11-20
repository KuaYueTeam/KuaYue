package cn.kycraft.kuayue.overhead_line.block.line;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class PositionComparator implements Comparator<Vec3> {
    @Override
    public int compare(Vec3 o1, Vec3 o2) {
        if (o1.x() != o2.x()){
            return Double.compare(o1.x(),o2.y());
        }
        if (o1.y() != o2.y()){
            return Double.compare(o1.y(),o2.y());
        }
        return Double.compare(o1.z(),o2.z());
    }

    public static int comparePosition(Vec3 o1,Vec3 o2){
        if (o1.x() != o2.x()){
            return Double.compare(o1.x(),o2.x());
        }
        if (o1.y() != o2.y()){
            return Double.compare(o1.y(), o2.y());
        }
        return Double.compare(o1.z(),o2.z());
    }

    public static int compareBlockPosition(BlockPos p1,BlockPos p2){
        if (p1.getX() != p2.getX()){
            return Integer.compare(p1.getX(),p2.getX());
        }
        if (p1.getY() != p2.getY()){
            return Integer.compare(p1.getY(), p2.getY());
        }
        return Integer.compare(p1.getZ(),p2.getZ());
    }
}
