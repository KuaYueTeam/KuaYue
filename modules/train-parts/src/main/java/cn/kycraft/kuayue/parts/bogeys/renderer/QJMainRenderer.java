package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class QJMainRenderer implements BogeyRenderer {

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.parse("kuayue:block/" + path));
    }

    public static final PartialModel
            QJ_MAIN_DRIVE_WHEEL = block("bogey/qj/main_drive_wheel"),
            QJ_DRIVE_WHEEL_2_4 = block("bogey/qj/drive_wheel_2_4"),
            QJ_DRIVE_WHEEL_1_5 = block("bogey/qj/drive_wheel_1_5"),
            QJ_CRANK = block("bogey/qj/crank"),
            QJ_CRANK_OTHERSIDE = block("bogey/qj/crank_otherside"),
            QJ_EXPANSION_LINK = block("bogey/qj/expansion_link"),
            QJ_EXPANSION_LINK_OTHERSIDE = block("bogey/qj/expansion_link_otherside"),
            QJ_CONNECTING_ROD = block("bogey/qj/connecting_rod"),
            QJ_CONNECTING_ROD_OTHERSIDE = block("bogey/qj/connecting_rod_otherside"),
            QJ_ECCENTRIC_ROD = block("bogey/qj/eccentric_rod"),
            QJ_ECCENTRIC_ROD_OTHERSIDE = block("bogey/qj/eccentric_rod_otherside"),
            QJ_CROSSHEAD = block("bogey/qj/crosshead"),
            QJ_CROSSHEAD_OTHERSIDE = block("bogey/qj/crosshead_otherside"),
            QJ_ANCHOR_LINK = block("bogey/qj/anchor_link"),
            QJ_ANCHOR_LINK_OTHERSIDE = block("bogey/qj/anchor_link_otherside"),
            QJ_COMBINATION_LEVER = block("bogey/qj/combination_lever"),
            QJ_COMBINATION_LEVER_OTHERSIDE = block("bogey/qj/combination_lever_otherside"),
            QJ_STEAM_VALVE = block("bogey/qj/steam_valve"),
            QJ_STEAM_VALVE_OTHERSIDE = block("bogey/qj/steam_valve_otherside"),
            QJ_RADIUS_ROD = block("bogey/qj/radius_rod"),
            QJ_RADIUS_ROD_OTHERSIDE = block("bogey/qj/radius_rod_otherside"),
            QJ_OIL_PRESS = block("bogey/qj/oil_press"),
            QJ_OIL_PRESS_OTHERSIDE = block("bogey/qj/oil_press_otherside"),
            QJ_OIL_PRESS_ROD = block("bogey/qj/oil_press_rod"),
            QJ_OIL_PRESS_ROD_OTHERSIDE = block("bogey/qj/oil_press_rod_otherside"),
            QJ_REVERSING = block("bogey/qj/reversing"),
            QJ_REVERSING_BOOM = block("bogey/qj/reversing_boom"),
            QJ_REVERSING_BOOM_OTHERSIDE = block("bogey/qj/reversing_boom_otherside"),
            QJ_BOGEY_FRAME = block("bogey/qj/qj_bogey_frame");

    public static final double L1 = 0.225 * 16, L2 = 27.399968, L3 = 0.607 * 16, L4 = 1.9804 * 16;

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        boolean flip = (direction == Direction.EAST || direction == Direction.SOUTH) && !inContraption;
        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        double rWheelAngle = Math.toRadians(wheelAngle);
        double rWheelAngle2 = Math.toRadians(wheelAngle - 90);
        double rWheelAngleOtherside = Math.toRadians(wheelAngle + 90);
        double rWheelAngleOtherside2 = Math.toRadians(wheelAngle + 90 - 90);

        double LBD2 = L1 * L1 + L4 * L4 - 2 * L1 * L4 * Math.cos(rWheelAngle2 - Math.toRadians(90));
        double LBD = Math.sqrt(LBD2);
        double LBD2Otherside = L1 * L1 + L4 * L4 - 2 * L1 * L4 * Math.cos(rWheelAngleOtherside2 - Math.toRadians(90));
        double LBDOtherside = Math.sqrt(LBD2Otherside);

        double PHI1 = Math.toDegrees(Math.asin(L1 / LBD * Math.sin(rWheelAngle2)));
        double PHI2 = Math.toDegrees(Math.acos((LBD2 + L3 * L3 - L2 * L2) / (2 * LBD * L3)));
        double PHI1Otherside = Math.toDegrees(Math.asin(L1 / LBDOtherside * Math.sin(rWheelAngleOtherside2)));
        double PHI2Otherside = Math.toDegrees(Math.acos((LBD2Otherside + L3 * L3 - L2 * L2) / (2 * LBDOtherside * L3)));

        double THITA3 = PHI1 + PHI2;
        double THITA2 = Math.toDegrees(Math.asin((L3 * Math.sin(Math.toRadians(-THITA3 - 11.4502)) + L1 * Math.sin(rWheelAngle2 + Math.toRadians(70))) / L2));
        double THITA3Otherside = PHI1Otherside + PHI2Otherside;
        double THITA2Otherside = Math.toDegrees(Math.asin((L3 * Math.sin(Math.toRadians(-THITA3Otherside - 11.4502)) + L1 * Math.sin(rWheelAngleOtherside2 + Math.toRadians(70))) / L2));

        double crankRotateX = Math.toDegrees(Math.atan(-Math.cos(rWheelAngle) * 0.12)) + 6.89f;
        double crankX = 0.4 * Math.sin(rWheelAngle) - 3.045 + Math.sqrt(3.045 * 3.045 - 0.4 * 0.4 * Math.cos(rWheelAngle) * Math.cos(rWheelAngle));
        double crankTranslateZ = -0.1 - crankX;

        double crankRotateXOtherside = Math.toDegrees(Math.atan(-Math.cos(rWheelAngleOtherside) * 0.12)) + 6.89f;
        double crankXOtherside = 0.4 * Math.sin(rWheelAngleOtherside) - 3.045 + Math.sqrt(3.045 * 3.045 - 0.4 * 0.4 * Math.cos(rWheelAngleOtherside) * Math.cos(rWheelAngleOtherside));
        double crankTranslateZOtherside = -0.1 - crankXOtherside;

        double expansionLinkAngle = (THITA3 * 0.76 - 20);
        double expansionLinkAngleOtherside = (THITA3Otherside * 0.76 - 20);

        double connectingRodTransY = -0.38 - Math.cos(rWheelAngle + Math.toRadians(180)) * 0.38;
        double connectingRodTransZ = Math.sin(rWheelAngle + Math.toRadians(180)) * 0.38;
        double connectingRodTransYOtherside = -0.38 - Math.cos(rWheelAngleOtherside + Math.toRadians(180)) * 0.38;
        double connectingRodTransZOtherside = Math.sin(rWheelAngleOtherside + Math.toRadians(180)) * 0.38;

        double eccentricRodRotateX = THITA2 + 15;
        double eccentricRodTranslateY = -Math.cos(rWheelAngle2) * 0.225;
        double eccentricRodTranslateZ = -(Math.sin(rWheelAngle2) * 0.225 + 0.225);
        double eccentricRodRotateXOtherside = THITA2Otherside + 15;
        double eccentricRodTranslateYOtherside = -Math.cos(rWheelAngleOtherside2) * 0.225;
        double eccentricRodTranslateZOtherside = -(Math.sin(rWheelAngleOtherside2) * 0.225 + 0.225);

        double crossheadTranslateZ = crankX;
        double crossheadTranslateZOtherside = crankXOtherside;

        double anchorLinkRotateX = -Math.toDegrees(Math.atan(Math.abs(Math.sin(rWheelAngle - Math.toRadians(90))))) * 0.1 + 7;
        double anchorLinkTranslateZ = crankX;
        double anchorLinkRotateXOtherside = -Math.toDegrees(Math.atan(Math.abs(Math.sin(rWheelAngleOtherside - Math.toRadians(90))))) * 0.1 + 7;
        double anchorLinkTranslateZOtherside = crankXOtherside;

        int yRot = flip ? 0 : 180;
        float sign = flip ? 1 : -1;

        // Wheels
        part(QJ_MAIN_DRIVE_WHEEL).translate(0, 1.1, 0).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_DRIVE_WHEEL_2_4).translate(0, 1.1, -1.6).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_DRIVE_WHEEL_2_4).translate(0, 1.1, 1.6).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_DRIVE_WHEEL_1_5).translate(0, 1.1, -3.2).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_DRIVE_WHEEL_1_5).translate(0, 1.1, 3.2).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Frame
        part(QJ_BOGEY_FRAME).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Crank
        part(QJ_CRANK).translate(flip ? -1.15 : 1.15, 1.13, flip ? (2.60 + (-crankTranslateZ)) : (-2.80 + (-crankTranslateZ)))
                .rotateYDegrees(yRot).rotateXDegrees((float)-crankRotateX).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_CRANK_OTHERSIDE).translate(flip ? 1.15 : -1.15, 1.13, flip ? (2.80 + (crankTranslateZOtherside)) : (-2.80 + (-crankTranslateZOtherside)))
                .rotateYDegrees(yRot).rotateXDegrees((float)(flip ? crankRotateXOtherside - 15 : -crankRotateXOtherside)).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Expansion link
        part(QJ_EXPANSION_LINK).translate(flip ? -1.32 : 1.32, 1.74, flip ? 1.79 : -1.79)
                .rotateYDegrees(flip ? 180 : 0).rotateXDegrees((float)expansionLinkAngle).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_EXPANSION_LINK_OTHERSIDE).translate(flip ? 1.32 : -1.32, 1.74, flip ? 1.79 : -1.79)
                .rotateYDegrees(flip ? 180 : 0).rotateXDegrees((float)(flip ? -expansionLinkAngleOtherside + 40 : expansionLinkAngleOtherside)).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Connecting rod
        part(QJ_CONNECTING_ROD).translate(flip ? -0.96 : 0.96, 1.5 + connectingRodTransY, -connectingRodTransZ)
                .rotateYDegrees(yRot).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_CONNECTING_ROD_OTHERSIDE).translate(flip ? 0.96 : -0.96, 1.5 + connectingRodTransYOtherside, -connectingRodTransZOtherside)
                .rotateYDegrees(yRot).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Eccentric rod
        part(QJ_ECCENTRIC_ROD).translate(1.36, 1.10 + eccentricRodTranslateY, 0.23 + eccentricRodTranslateZ)
                .rotateXDegrees((float)eccentricRodRotateX).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_ECCENTRIC_ROD_OTHERSIDE).translate(-1.36, 1.10 + eccentricRodTranslateYOtherside, 0.23 + eccentricRodTranslateZOtherside)
                .rotateXDegrees((float)eccentricRodRotateXOtherside).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Crosshead
        part(QJ_CROSSHEAD).translate(1.15, 1.13, -2.85 + crossheadTranslateZ).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_CROSSHEAD_OTHERSIDE).translate(-1.15, 1.13, -2.85 + crossheadTranslateZOtherside).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Anchor link
        part(QJ_ANCHOR_LINK).translate(1.30, 0.83, -2.85 + anchorLinkTranslateZ).rotateXDegrees((float)anchorLinkRotateX).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_ANCHOR_LINK_OTHERSIDE).translate(-1.30, 0.83, -2.85 + anchorLinkTranslateZOtherside).rotateXDegrees((float)anchorLinkRotateXOtherside).light(light).overlay(overlay).renderInto(poseStack, buffer);

        // Static parts (no DTHITA animation in simple render)
        part(QJ_COMBINATION_LEVER).translate(1.30, 1.75, -3.33).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_COMBINATION_LEVER_OTHERSIDE).translate(-1.30, 1.75, -3.33).rotateXDegrees(-25.95f).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_STEAM_VALVE).translate(1.30, 1.62, -3.92).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_STEAM_VALVE_OTHERSIDE).translate(-1.30, 1.62, -3.92).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_RADIUS_ROD).translate(1.31, 1.80, -3.38).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_RADIUS_ROD_OTHERSIDE).translate(-1.31, 1.80, -3.38).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_OIL_PRESS_ROD).translate(1.29, 1.98, -3.23).rotateXDegrees(10).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_OIL_PRESS_ROD_OTHERSIDE).translate(-1.29, 1.99, -3.32).rotateXDegrees(25).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_OIL_PRESS).translate(1.29, 2.16, -3.42).rotateXDegrees(7).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_OIL_PRESS_OTHERSIDE).translate(-1.29, 2.16, -3.42).rotateXDegrees(2).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_REVERSING_BOOM).translate(1.18, 2.18, -2.13).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_REVERSING_BOOM_OTHERSIDE).translate(-1.18, 2.18, -2.13).light(light).overlay(overlay).renderInto(poseStack, buffer);
        part(QJ_REVERSING).translate(0, 2.15, -1.83).light(light).overlay(overlay).renderInto(poseStack, buffer);

        if (flip) {
            poseStack.popPose();
        }
    }

    private static SuperByteBuffer part(PartialModel model) {
        return CachedBuffers.partial(model, Blocks.AIR.defaultBlockState());
    }

    public static class Andesite extends QJMainRenderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            poseStack.pushPose();
            poseStack.scale(1.2F, 1, 1);
            super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);
            poseStack.popPose();
        }
    }
}
