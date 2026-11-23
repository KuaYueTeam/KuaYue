package cn.kycraft.kuayue.overhead_line.block.line;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class PositionComparator implements Comparator<Vec3> {
    @Override
    public int compare(Vec3 o1, Vec3 o2) {
        return compare(o1.x() , o2.y() , o1.z() , o2.x() , o2.y() ,o2.z());
    }

    public static int comparePosition(Vec3 o1,Vec3 o2){
        return compare(o1.x() , o2.y() , o1.z() , o2.x() , o2.y() ,o2.z());
    }

    public static int compareBlockPosition(BlockPos p1,BlockPos p2){
        return compare(p1.getX(), p1.getY(),p1.getZ(),p2.getX(),p2.getY(),p2.getZ());
    }

    public static int compare(double  x1 , double y1 , double z1 , double x2 , double y2 , double z2){
        if (x1 != x2){return Double.compare(x1,x2);};
        if (y1 != y2){return Double.compare(y1,y2);};
        return Double.compare(z1,z2);
    }
}
