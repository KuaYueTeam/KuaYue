package cn.kycraft.kuayue.parts.bogeys.core;

import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import lib.kasuga.rendering.models.uml.math.Transform;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Binds KuaYue wheel-angle data and articulated mechanisms to the 2.0 model catalogue. */
public final class KuaYueBogeyMotion {

    private static final double QJ_LINK_1 = 0.225 * 16;
    private static final double QJ_LINK_2 = 27.399968;
    private static final double QJ_LINK_3 = 0.607 * 16;
    private static final double QJ_LINK_4 = 1.9804 * 16;

    private KuaYueBogeyMotion() {
    }

    public static List<ModelPart> bind(
            ResourceLocation sizeId,
            List<ModelPart> parts,
            Supplier<Float> wheelAngle,
            boolean frameDomain
    ) {
        Objects.requireNonNull(sizeId, "sizeId");
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(wheelAngle, "wheelAngle");
        float sign = angleSign(sizeId);
        return parts.stream()
                .map(part -> {
                    String path = part.model().getPath();
                    if (path.contains("transmission_rod")) {
                        if (frameDomain) {
                            return part.moving(ModelPart.frameTransform(
                                    () -> rodTransform(wheelAngle, sign)
                            ));
                        }
                        return part.moving(ModelPart.tickTransform(
                                ignored -> rodTransform(wheelAngle, sign)
                        ));
                    }
                    if (isQjMechanism(path)) {
                        if (frameDomain) {
                            return part.moving(ModelPart.frameTransform(
                                    () -> qjMechanismTransform(path, wheelAngle)
                            ));
                        }
                        return part.moving(ModelPart.tickTransform(
                                ignored -> qjMechanismTransform(path, wheelAngle)
                        ));
                    }
                    float scale = spinScale(sizeId, part.model());
                    if (scale == 0) {
                        return part;
                    }
                    ModelPart.Spin.AngleSource source =
                            () -> wheelAngle.get() * spinSign(sizeId);
                    ModelPart.Spin spin = frameDomain
                            ? ModelPart.Spin.frame(source, scale)
                            : ModelPart.Spin.tick(source, scale);
                    return part.spinning(spin);
                })
                .toList();
    }

    private static Transform rodTransform(Supplier<Float> wheelAngle, float sign) {
        return new Transform().rotate(
                0,
                0,
                wheelAngle.get() * sign * 3.256f,
                true
        );
    }

    private static boolean isQjMechanism(String path) {
        return path.endsWith("qj/crank.obj")
                || path.endsWith("qj/crank_otherside.obj")
                || path.endsWith("qj/expansion_link.obj")
                || path.endsWith("qj/expansion_link_otherside.obj")
                || path.endsWith("qj/connecting_rod.obj")
                || path.endsWith("qj/connecting_rod_otherside.obj")
                || path.endsWith("qj/eccentric_rod.obj")
                || path.endsWith("qj/eccentric_rod_otherside.obj")
                || path.endsWith("qj/crosshead.obj")
                || path.endsWith("qj/crosshead_otherside.obj")
                || path.endsWith("qj/anchor_link.obj")
                || path.endsWith("qj/anchor_link_otherside.obj");
    }

    private static Transform qjMechanismTransform(
            String path,
            Supplier<Float> wheelAngle
    ) {
        float degrees = wheelAngle.get();
        double angle = Math.toRadians(degrees);
        double angle2 = Math.toRadians(degrees - 90);
        double otherAngle = Math.toRadians(degrees + 90);
        double otherAngle2 = Math.toRadians(degrees);

        double linkDistance2 = QJ_LINK_1 * QJ_LINK_1
                + QJ_LINK_4 * QJ_LINK_4
                - 2 * QJ_LINK_1 * QJ_LINK_4
                * Math.cos(angle2 - Math.toRadians(90));
        double linkDistance = Math.sqrt(linkDistance2);
        double otherLinkDistance2 = QJ_LINK_1 * QJ_LINK_1
                + QJ_LINK_4 * QJ_LINK_4
                - 2 * QJ_LINK_1 * QJ_LINK_4
                * Math.cos(otherAngle2 - Math.toRadians(90));
        double otherLinkDistance = Math.sqrt(otherLinkDistance2);

        double phi1 = Math.toDegrees(
                Math.asin(QJ_LINK_1 / linkDistance * Math.sin(angle2))
        );
        double phi2 = Math.toDegrees(
                Math.acos(
                        (linkDistance2 + QJ_LINK_3 * QJ_LINK_3 - QJ_LINK_2 * QJ_LINK_2)
                                / (2 * linkDistance * QJ_LINK_3)
                )
        );
        double otherPhi1 = Math.toDegrees(
                Math.asin(QJ_LINK_1 / otherLinkDistance * Math.sin(otherAngle2))
        );
        double otherPhi2 = Math.toDegrees(
                Math.acos(
                        (otherLinkDistance2 + QJ_LINK_3 * QJ_LINK_3
                                - QJ_LINK_2 * QJ_LINK_2)
                                / (2 * otherLinkDistance * QJ_LINK_3)
                )
        );

        double theta3 = phi1 + phi2;
        double theta2 = Math.toDegrees(
                Math.asin(
                        (QJ_LINK_3 * Math.sin(Math.toRadians(-theta3 - 11.4502))
                                + QJ_LINK_1 * Math.sin(angle2 + Math.toRadians(70)))
                                / QJ_LINK_2
                )
        );
        double otherTheta3 = otherPhi1 + otherPhi2;
        double otherTheta2 = Math.toDegrees(
                Math.asin(
                        (QJ_LINK_3 * Math.sin(
                                Math.toRadians(-otherTheta3 - 11.4502)
                        ) + QJ_LINK_1 * Math.sin(
                                otherAngle2 + Math.toRadians(70)
                        )) / QJ_LINK_2
                )
        );

        double crankRotation = Math.toDegrees(
                Math.atan(-Math.cos(angle) * 0.12)
        ) + 6.89;
        double crankX = 0.4 * Math.sin(angle)
                - 3.045
                + Math.sqrt(
                3.045 * 3.045 - 0.4 * 0.4 * Math.cos(angle) * Math.cos(angle)
        );
        double crankTranslation = -0.1 - crankX;

        double otherCrankRotation = Math.toDegrees(
                Math.atan(-Math.cos(otherAngle) * 0.12)
        ) + 6.89;
        double otherCrankX = 0.4 * Math.sin(otherAngle)
                - 3.045
                + Math.sqrt(
                3.045 * 3.045
                        - 0.4 * 0.4 * Math.cos(otherAngle) * Math.cos(otherAngle)
        );
        double otherCrankTranslation = -0.1 - otherCrankX;

        double expansionAngle = theta3 * 0.76 - 20;
        double otherExpansionAngle = otherTheta3 * 0.76 - 20;
        double connectingY = -0.38 - Math.cos(angle + Math.PI) * 0.38;
        double connectingZ = Math.sin(angle + Math.PI) * 0.38;
        double otherConnectingY = -0.38
                - Math.cos(otherAngle + Math.PI) * 0.38;
        double otherConnectingZ = Math.sin(otherAngle + Math.PI) * 0.38;
        double eccentricRotation = theta2 + 15;
        double eccentricY = -Math.cos(angle2) * 0.225;
        double eccentricZ = -(Math.sin(angle2) * 0.225 + 0.225);
        double otherEccentricRotation = otherTheta2 + 15;
        double otherEccentricY = -Math.cos(otherAngle2) * 0.225;
        double otherEccentricZ = -(Math.sin(otherAngle2) * 0.225 + 0.225);
        double anchorRotation = -Math.toDegrees(
                Math.atan(Math.abs(Math.sin(angle - Math.toRadians(90))))
        ) * 0.1 + 7;
        double otherAnchorRotation = -Math.toDegrees(
                Math.atan(Math.abs(Math.sin(otherAngle - Math.toRadians(90))))
        ) * 0.1 + 7;

        if (path.endsWith("qj/crank.obj")) {
            return new Transform()
                    .translate(0, 0, (float) -crankTranslation)
                    .rotate(0, 180, 0, true)
                    .rotate((float) -crankRotation, 0, 0, true);
        }
        if (path.endsWith("qj/crank_otherside.obj")) {
            return new Transform()
                    .translate(0, 0, (float) -otherCrankTranslation)
                    .rotate(0, 180, 0, true)
                    .rotate((float) -otherCrankRotation, 0, 0, true);
        }
        if (path.endsWith("qj/expansion_link.obj")) {
            return new Transform().rotate((float) expansionAngle, 0, 0, true);
        }
        if (path.endsWith("qj/expansion_link_otherside.obj")) {
            return new Transform().rotate((float) otherExpansionAngle, 0, 0, true);
        }
        if (path.endsWith("qj/connecting_rod.obj")) {
            return new Transform()
                    .translate(0, (float) connectingY, (float) -connectingZ)
                    .rotate(0, 180, 0, true);
        }
        if (path.endsWith("qj/connecting_rod_otherside.obj")) {
            return new Transform()
                    .translate(0, (float) otherConnectingY, (float) -otherConnectingZ)
                    .rotate(0, 180, 0, true);
        }
        if (path.endsWith("qj/eccentric_rod.obj")) {
            return new Transform()
                    .translate(0, (float) eccentricY, (float) eccentricZ)
                    .rotate((float) eccentricRotation, 0, 0, true);
        }
        if (path.endsWith("qj/eccentric_rod_otherside.obj")) {
            return new Transform()
                    .translate(0, (float) otherEccentricY, (float) otherEccentricZ)
                    .rotate((float) otherEccentricRotation, 0, 0, true);
        }
        if (path.endsWith("qj/crosshead.obj")) {
            return new Transform().translate(0, 0, (float) crankX);
        }
        if (path.endsWith("qj/crosshead_otherside.obj")) {
            return new Transform().translate(0, 0, (float) otherCrankX);
        }
        if (path.endsWith("qj/anchor_link.obj")) {
            return new Transform()
                    .translate(0, 0, (float) crankX)
                    .rotate((float) anchorRotation, 0, 0, true);
        }
        if (path.endsWith("qj/anchor_link_otherside.obj")) {
            return new Transform()
                    .translate(0, 0, (float) otherCrankX)
                    .rotate((float) otherAnchorRotation, 0, 0, true);
        }
        throw new IllegalArgumentException("unknown QJ mechanism part: " + path);
    }

    private static float angleSign(ResourceLocation sizeId) {
        String path = sizeId.getPath();
        if (!path.contains("backward")) {
            return 1;
        }
        // The 1.0 andesite registrations reused their forward renderer for these variants.
        if (path.equals("df11g_backward_a")
                || path.equals("ss3_backward_a")
                || path.equals("ss8_backward_a")) {
            return 1;
        }
        if (path.equals("hxd3d_backward_a")) {
            return -1;
        }
        // These 1.0 backward renderers already mirrored their geometry and kept a positive angle.
        if (path.startsWith("df5_")
                || path.startsWith("jy290_")
                || path.startsWith("dfh21_")
                || path.startsWith("hxd3d_")
                || path.startsWith("sw220k_")) {
            return 1;
        }
        return -1;
    }

    private static float spinSign(ResourceLocation sizeId) {
        String sizePath = sizeId.getPath();
        if (sizePath.equals("qj_main") || sizePath.equals("qj_main_a")) {
            return -1;
        }
        if (sizePath.startsWith("df5_") || sizePath.startsWith("jy290_")) {
            return sizePath.contains("backward") ? 1 : -1;
        }
        if (sizePath.equals("hxd3d_backward_a")) {
            return -1;
        }
        if (sizePath.startsWith("hxd3d_")) {
            return sizePath.contains("backward") ? 1 : -1;
        }
        return angleSign(sizeId);
    }

    private static float spinScale(ResourceLocation sizeId, ResourceLocation model) {
        String style = sizeId.getPath();
        String path = model.getPath();
        if (path.endsWith("motor_pony_wheel.obj")
                || path.endsWith("pk209_motorwheel.obj")) {
            return 3.256f;
        }
        if (path.endsWith("mkz_wheel.obj")
                || (style.startsWith("tkz") && path.endsWith("tkz2_wheel.obj"))
                || (style.startsWith("tkz") && path.endsWith("tkz2_motor_wheel.obj"))) {
            return 1.22f;
        }
        if (path.endsWith("zk6_wheel.obj")) {
            return 1.09f;
        }
        if (isWheel(path)) {
            return 1f;
        }
        return 0f;
    }

    private static boolean isWheel(String path) {
        return path.contains("wheel")
                || path.contains("drive_wheel")
                || path.endsWith("main_drive_wheel.obj");
    }
}
