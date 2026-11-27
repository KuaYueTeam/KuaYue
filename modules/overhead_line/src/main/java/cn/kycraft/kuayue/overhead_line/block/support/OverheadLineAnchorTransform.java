package cn.kycraft.kuayue.overhead_line.block.support;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;

public final class OverheadLineAnchorTransform {
    public static final Vec3 PIVOT = new Vec3(0.5, 0, 0.5);
    private static final double LEGACY_ROTATION_SCALE = 1.03;
    private static final double LEGACY_OFFSET_SCALE = 1.3;

    private OverheadLineAnchorTransform() {}

    public record Adjustment(double rotationDegrees, double x, double y, double z) {
        public static final Adjustment NONE = new Adjustment(0, 0, 0, 0);
    }

    public static Vec3 toWorld(BlockPos pos, Direction facing, Vec3 local) {
        return toWorld(pos, facing, local, Adjustment.NONE);
    }

    public static Vec3 toWorld(BlockPos pos, Direction facing, Vec3 local, Adjustment adjustment) {
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Expected horizontal support facing: " + facing);
        }

        Matrix4d transform = new Matrix4d().translation(PIVOT.x, PIVOT.y, PIVOT.z)
                .rotate(facing.getRotation().normalize())
                .rotateX(-Math.PI / 2)
                .rotateY(-Math.PI / 2)
                .rotateY(Math.toRadians(adjustment.rotationDegrees() * LEGACY_ROTATION_SCALE))
                .translate(-adjustment.x() * LEGACY_OFFSET_SCALE,
                        adjustment.y() * LEGACY_OFFSET_SCALE, -adjustment.z() * LEGACY_OFFSET_SCALE);
        Vector3d offset = transform.transformPosition(new Vector3d(-local.x, local.y, -local.z));
        return new Vec3(pos.getX() + offset.x, pos.getY() + offset.y, pos.getZ() + offset.z);
    }
}
