package cn.kycraft.kuayue.parts.bogeys.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lib.kasuga.rendering.models.uml.dynamic.animation.AnimationClip;
import lib.kasuga.rendering.models.uml.dynamic.animation.FormulaTransformSampler;
import lib.kasuga.rendering.models.uml.math.Transform;
import lib.kasuga.rendering.models.mc.dynamic.fsm.AnimationClipLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QjFormulaAnimationTest {
    private static final double LINK_1 = 0.225 * 16;
    private static final double LINK_2 = 27.399968;
    private static final double LINK_3 = 0.607 * 16;
    private static final double LINK_4 = 1.9804 * 16;
    private static final float EPS = 2e-4f;

    private static final Map<String, String> PART_CHANNELS = Map.ofEntries(
            Map.entry("part_06", "crank"),
            Map.entry("part_07", "crank_otherside"),
            Map.entry("part_08", "expansion_link"),
            Map.entry("part_09", "expansion_link_otherside"),
            Map.entry("part_10", "connecting_rod"),
            Map.entry("part_11", "connecting_rod_otherside"),
            Map.entry("part_12", "eccentric_rod"),
            Map.entry("part_13", "eccentric_rod_otherside"),
            Map.entry("part_14", "crosshead"),
            Map.entry("part_15", "crosshead_otherside"),
            Map.entry("part_16", "anchor_link"),
            Map.entry("part_17", "anchor_link_otherside")
    );

    @Test
    void formulaClipMatchesTheLegacyQjMechanism() throws IOException {
        AnimationClip clip = qjClip();
        assertEquals(12, clip.formulaTransforms().size());
        FormulaTransformSampler sampler = new FormulaTransformSampler(clip);

        for (float angle = 0f; angle < 360f; angle += 1f) {
            sampler.assignQuery("wheel_angle", angle);
            for (var entry : PART_CHANNELS.entrySet()) {
                Transform actual = sampler.samplePart(entry.getKey());
                String label = entry.getKey() + "@" + angle;
                assertNotNull(actual, label);
                assertTransformEquals(legacyTransform(entry.getValue(), angle), actual, label);
            }
        }
    }

    private static AnimationClip qjClip() throws IOException {
        Path path = Path.of(
                "resources/bogeys/data-driven/data/kuayue/animation_clips/qj_mechanism.json"
        );
        JsonElement root = JsonParser.parseString(Files.readString(path));
        AnimationClipLoader.DecodedFile decoded = AnimationClipLoader.decodeFile(root);
        assertTrue(decoded.errors().isEmpty(), decoded.errors().toString());
        assertEquals(1, decoded.clips().size());
        return decoded.clips().getFirst();
    }

    private static void assertTransformEquals(Transform expected, Transform actual, String label) {
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                assertEquals(
                        expected.transform().get(row, column),
                        actual.transform().get(row, column),
                        EPS,
                        label + " matrix[" + row + "," + column + "]"
                );
            }
        }
        assertTrue(actual.normal().isFinite(), label + " normal");
    }

    private static Transform legacyTransform(String channel, float degrees) {
        double angle = Math.toRadians(degrees);
        double angle2 = Math.toRadians(degrees - 90);
        double otherAngle = Math.toRadians(degrees + 90);
        double otherAngle2 = Math.toRadians(degrees);

        double linkDistance2 = LINK_1 * LINK_1 + LINK_4 * LINK_4
                - 2 * LINK_1 * LINK_4 * Math.cos(angle2 - Math.toRadians(90));
        double linkDistance = Math.sqrt(linkDistance2);
        double otherLinkDistance2 = LINK_1 * LINK_1 + LINK_4 * LINK_4
                - 2 * LINK_1 * LINK_4 * Math.cos(otherAngle2 - Math.toRadians(90));
        double otherLinkDistance = Math.sqrt(otherLinkDistance2);

        double phi1 = Math.toDegrees(Math.asin(
                LINK_1 / linkDistance * Math.sin(angle2)
        ));
        double phi2 = Math.toDegrees(Math.acos(
                (linkDistance2 + LINK_3 * LINK_3 - LINK_2 * LINK_2)
                        / (2 * linkDistance * LINK_3)
        ));
        double otherPhi1 = Math.toDegrees(Math.asin(
                LINK_1 / otherLinkDistance * Math.sin(otherAngle2)
        ));
        double otherPhi2 = Math.toDegrees(Math.acos(
                (otherLinkDistance2 + LINK_3 * LINK_3 - LINK_2 * LINK_2)
                        / (2 * otherLinkDistance * LINK_3)
        ));

        double theta3 = phi1 + phi2;
        double theta2 = Math.toDegrees(Math.asin(
                (LINK_3 * Math.sin(Math.toRadians(-theta3 - 11.4502))
                        + LINK_1 * Math.sin(angle2 + Math.toRadians(70))) / LINK_2
        ));
        double otherTheta3 = otherPhi1 + otherPhi2;
        double otherTheta2 = Math.toDegrees(Math.asin(
                (LINK_3 * Math.sin(Math.toRadians(-otherTheta3 - 11.4502))
                        + LINK_1 * Math.sin(otherAngle2 + Math.toRadians(70))) / LINK_2
        ));

        double crankRotation = Math.toDegrees(Math.atan(-Math.cos(angle) * 0.12)) + 6.89;
        double crankX = 0.4 * Math.sin(angle) - 3.045 + Math.sqrt(
                3.045 * 3.045 - 0.4 * 0.4 * Math.cos(angle) * Math.cos(angle)
        );
        double crankTranslation = -0.1 - crankX;
        double otherCrankRotation = Math.toDegrees(
                Math.atan(-Math.cos(otherAngle) * 0.12)
        ) + 6.89;
        double otherCrankX = 0.4 * Math.sin(otherAngle) - 3.045 + Math.sqrt(
                3.045 * 3.045
                        - 0.4 * 0.4 * Math.cos(otherAngle) * Math.cos(otherAngle)
        );
        double otherCrankTranslation = -0.1 - otherCrankX;
        double expansionAngle = theta3 * 0.76 - 20;
        double otherExpansionAngle = otherTheta3 * 0.76 - 20;
        double connectingY = -0.38 - Math.cos(angle + Math.PI) * 0.38;
        double connectingZ = Math.sin(angle + Math.PI) * 0.38;
        double otherConnectingY = -0.38 - Math.cos(otherAngle + Math.PI) * 0.38;
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

        return switch (channel) {
            case "crank" -> new Transform()
                    .translate(0, 0, (float) -crankTranslation)
                    .rotate(0, 180, 0, true)
                    .rotate((float) -crankRotation, 0, 0, true);
            case "crank_otherside" -> new Transform()
                    .translate(0, 0, (float) -otherCrankTranslation)
                    .rotate(0, 180, 0, true)
                    .rotate((float) -otherCrankRotation, 0, 0, true);
            case "expansion_link" -> new Transform()
                    .rotate((float) expansionAngle, 0, 0, true);
            case "expansion_link_otherside" -> new Transform()
                    .rotate((float) otherExpansionAngle, 0, 0, true);
            case "connecting_rod" -> new Transform()
                    .translate(0, (float) connectingY, (float) -connectingZ)
                    .rotate(0, 180, 0, true);
            case "connecting_rod_otherside" -> new Transform()
                    .translate(0, (float) otherConnectingY, (float) -otherConnectingZ)
                    .rotate(0, 180, 0, true);
            case "eccentric_rod" -> new Transform()
                    .translate(0, (float) eccentricY, (float) eccentricZ)
                    .rotate((float) eccentricRotation, 0, 0, true);
            case "eccentric_rod_otherside" -> new Transform()
                    .translate(0, (float) otherEccentricY, (float) otherEccentricZ)
                    .rotate((float) otherEccentricRotation, 0, 0, true);
            case "crosshead" -> new Transform().translate(0, 0, (float) crankX);
            case "crosshead_otherside" -> new Transform()
                    .translate(0, 0, (float) otherCrankX);
            case "anchor_link" -> new Transform()
                    .translate(0, 0, (float) crankX)
                    .rotate((float) anchorRotation, 0, 0, true);
            case "anchor_link_otherside" -> new Transform()
                    .translate(0, 0, (float) otherCrankX)
                    .rotate((float) otherAnchorRotation, 0, 0, true);
            default -> throw new IllegalArgumentException("Unknown channel " + channel);
        };
    }
}
