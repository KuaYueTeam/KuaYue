package cn.kycraft.kuayue.overhead_line.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RenderCurve {
    private final List<Segment> segments;
    private final AABB bounds;

    RenderCurve(List<Segment> segments, Vec3 origin) {
        this.segments = List.copyOf(segments);
        AABB box = new AABB(origin, origin);
        for (Segment segment : segments) {
            for (int i = 0; i < 8; i++) {
                Vec3 p = segment.corner(i);
                box = box.minmax(new AABB(p, p));
            }
        }
        this.bounds = box;
    }

    public AABB bounds() {
        return bounds;
    }

    List<Segment> segments() {
        return segments;
    }

    static final class Segment {
        private final Vec3[] corners = new Vec3[8];
        final Vec3 tangent;
        final Vec3 right;
        final Vec3 up;
        final BlockPos lightPos;

        Segment(Vec3 from, Vec3 to, Vec3 tangent, double halfWidth) {
            this.tangent = tangent;
            this.right = Math.abs(tangent.y) > 0.9999
                    ? new Vec3(0, 0, 1).cross(tangent).normalize()
                    : new Vec3(tangent.z, 0, -tangent.x).normalize();
            this.up = tangent.cross(right);
            this.lightPos = BlockPos.containing(from.lerp(to, 0.5));
            Vec3 side = right.scale(halfWidth);
            Vec3 top = up.scale(halfWidth);
            setRing(from, side, top, 0);
            setRing(to, side, top, 4);
        }

        private void setRing(Vec3 center, Vec3 side, Vec3 top, int base) {
            corners[base] = center.subtract(side).subtract(top);
            corners[base + 1] = center.add(side).subtract(top);
            corners[base + 2] = center.add(side).add(top);
            corners[base + 3] = center.subtract(side).add(top);
        }

        Vec3 corner(int index) {
            return corners[index];
        }
    }
}
