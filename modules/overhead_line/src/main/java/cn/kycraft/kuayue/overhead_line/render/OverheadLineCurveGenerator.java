package cn.kycraft.kuayue.overhead_line.render;

import cn.kycraft.kuayue.overhead_line.wire.OverheadLineWireKind;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class OverheadLineCurveGenerator {
    private static final double CONIC_END_HEIGHT = 1.3;
    private static final double CONIC_TANGENT_CLEARANCE = 0.5;
    private static final double DROPPER_SPACING = 5.0;
    private static final double CATENARY_SPACING = 0.2;
    private static final double CATENARY_COEFFICIENT = 300.0;
    private static final double HALF_WIDTH = 0.025;
    private static final double MIN_HORIZONTAL_SPAN = 1.0e-4;
    private static final int MAX_SEGMENTS = 4096;

    private OverheadLineCurveGenerator() {}

    public static RenderCurve generate(Vec3 from, Vec3 to, OverheadLineWireKind kind) {
        Vec3 delta = to.subtract(from);
        double span = Math.hypot(delta.x, delta.z);
        List<RenderCurve.Segment> segments = new ArrayList<>();
        if (span < MIN_HORIZONTAL_SPAN) {
            addSegment(segments, from, to);
        } else {
            switch (kind) {
                case OVERHEAD_LINE_WIRE -> conicHangLine(segments, from, to, delta, span);
                case ELECTRONIC_WIRE -> catenaryLine(segments, from, to, delta, span);
            }
        }
        return new RenderCurve(segments, from);
    }

    private static void conicHangLine(List<RenderCurve.Segment> segments, Vec3 from, Vec3 to,
                                      Vec3 delta, double span) {
        int count = segmentCount(Math.floor(span / DROPPER_SPACING));

        double bend = CONIC_TANGENT_CLEARANCE * Math.hypot(1.0, delta.y / span) - CONIC_END_HEIGHT;
        Vec3 previous = from.add(0, CONIC_END_HEIGHT, 0);
        for (int i = 1; i <= count; i++) {
            double t = (double) i / count;
            Vec3 contact = i == count ? to : from.add(delta.scale(t));
            Vec3 messenger = contact.add(0, CONIC_END_HEIGHT + bend * t * (1.0 - t), 0);
            addSegment(segments, previous, messenger);
            if (i < count) addSegment(segments, contact, messenger);
            previous = messenger;
        }
        addSegment(segments, from, to);
    }

    private static void catenaryLine(List<RenderCurve.Segment> segments, Vec3 from, Vec3 to,
                                    Vec3 delta, double span) {
        int count = segmentCount(Math.ceil(span / CATENARY_SPACING));

        double g = CATENARY_COEFFICIENT;
        double rise = delta.y / (2.0 * g * Math.sinh(span / (2.0 * g)));
        double phase = Math.copySign(Math.log(Math.abs(rise) + Math.hypot(rise, 1.0)), rise);
        Vec3 previous = from;
        for (int i = 1; i <= count; i++) {
            double t = (double) i / count;
            double x = span * t;
            double y = 2.0 * g * Math.sinh(x / (2.0 * g)) * Math.sinh((x - span) / (2.0 * g) + phase);

            if (!Double.isFinite(y)) {
                segments.clear();
                addSegment(segments, from, to);
                return;
            }
            Vec3 next = i == count ? to : from.add(delta.x * t, y, delta.z * t);
            addSegment(segments, previous, next);
            previous = next;
        }
    }

    private static int segmentCount(double count) {
        return (int) Math.clamp(count, 1, MAX_SEGMENTS);
    }

    private static void addSegment(List<RenderCurve.Segment> segments, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0e-8 || !Double.isFinite(length)) return;
        segments.add(new RenderCurve.Segment(from, to, delta.scale(1.0 / length), HALF_WIDTH));
    }
}
