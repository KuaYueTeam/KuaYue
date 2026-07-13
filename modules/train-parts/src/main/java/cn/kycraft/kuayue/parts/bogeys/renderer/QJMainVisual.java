package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.bogey.BogeyVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class QJMainVisual implements BogeyVisual {

    private final TransformedInstance mainDriveWheel, driveWheel24_1, driveWheel24_2, driveWheel15_1, driveWheel15_2;
    private final TransformedInstance crank, crankOtherside;
    private final TransformedInstance expansionLink, expansionLinkOtherside;
    private final TransformedInstance connectingRod, connectingRodOtherside;
    private final TransformedInstance eccentricRod, eccentricRodOtherside;
    private final TransformedInstance crosshead, crossheadOtherside;
    private final TransformedInstance anchorLink, anchorLinkOtherside;
    private final TransformedInstance combinationLever, combinationLeverOtherside;
    private final TransformedInstance steamValve, steamValveOtherside;
    private final TransformedInstance radiusRod, radiusRodOtherside;
    private final TransformedInstance oilPress, oilPressOtherside;
    private final TransformedInstance oilPressRod, oilPressRodOtherside;
    private final TransformedInstance reversing, reversingBoom, reversingBoomOtherside;
    private final TransformedInstance bogeyFrame;

    private static TransformedInstance createInst(
            dev.engine_room.flywheel.api.instance.InstancerProvider provider, PartialModel model) {
        return provider.instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).createInstance();
    }

    public QJMainVisual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        var p = ctx.instancerProvider();
        mainDriveWheel = createInst(p, QJMainRenderer.QJ_MAIN_DRIVE_WHEEL);
        driveWheel24_1 = createInst(p, QJMainRenderer.QJ_DRIVE_WHEEL_2_4);
        driveWheel24_2 = createInst(p, QJMainRenderer.QJ_DRIVE_WHEEL_2_4);
        driveWheel15_1 = createInst(p, QJMainRenderer.QJ_DRIVE_WHEEL_1_5);
        driveWheel15_2 = createInst(p, QJMainRenderer.QJ_DRIVE_WHEEL_1_5);
        crank = createInst(p, QJMainRenderer.QJ_CRANK);
        crankOtherside = createInst(p, QJMainRenderer.QJ_CRANK_OTHERSIDE);
        expansionLink = createInst(p, QJMainRenderer.QJ_EXPANSION_LINK);
        expansionLinkOtherside = createInst(p, QJMainRenderer.QJ_EXPANSION_LINK_OTHERSIDE);
        connectingRod = createInst(p, QJMainRenderer.QJ_CONNECTING_ROD);
        connectingRodOtherside = createInst(p, QJMainRenderer.QJ_CONNECTING_ROD_OTHERSIDE);
        eccentricRod = createInst(p, QJMainRenderer.QJ_ECCENTRIC_ROD);
        eccentricRodOtherside = createInst(p, QJMainRenderer.QJ_ECCENTRIC_ROD_OTHERSIDE);
        crosshead = createInst(p, QJMainRenderer.QJ_CROSSHEAD);
        crossheadOtherside = createInst(p, QJMainRenderer.QJ_CROSSHEAD_OTHERSIDE);
        anchorLink = createInst(p, QJMainRenderer.QJ_ANCHOR_LINK);
        anchorLinkOtherside = createInst(p, QJMainRenderer.QJ_ANCHOR_LINK_OTHERSIDE);
        combinationLever = createInst(p, QJMainRenderer.QJ_COMBINATION_LEVER);
        combinationLeverOtherside = createInst(p, QJMainRenderer.QJ_COMBINATION_LEVER_OTHERSIDE);
        steamValve = createInst(p, QJMainRenderer.QJ_STEAM_VALVE);
        steamValveOtherside = createInst(p, QJMainRenderer.QJ_STEAM_VALVE_OTHERSIDE);
        radiusRod = createInst(p, QJMainRenderer.QJ_RADIUS_ROD);
        radiusRodOtherside = createInst(p, QJMainRenderer.QJ_RADIUS_ROD_OTHERSIDE);
        oilPress = createInst(p, QJMainRenderer.QJ_OIL_PRESS);
        oilPressOtherside = createInst(p, QJMainRenderer.QJ_OIL_PRESS_OTHERSIDE);
        oilPressRod = createInst(p, QJMainRenderer.QJ_OIL_PRESS_ROD);
        oilPressRodOtherside = createInst(p, QJMainRenderer.QJ_OIL_PRESS_ROD_OTHERSIDE);
        reversingBoom = createInst(p, QJMainRenderer.QJ_REVERSING_BOOM);
        reversingBoomOtherside = createInst(p, QJMainRenderer.QJ_REVERSING_BOOM_OTHERSIDE);
        reversing = createInst(p, QJMainRenderer.QJ_REVERSING);
        bogeyFrame = createInst(p, QJMainRenderer.QJ_BOGEY_FRAME);
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        // Simplified rendering: no physics animation in visual mode, just static positions
        // The full valve gear animation runs in the renderer; visual shows static bogey
        boolean flip = direction == Direction.EAST || direction == Direction.SOUTH;
        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        bogeyFrame.setTransform(poseStack).setChanged();

        int yRot = flip ? 0 : 180;
        float sign = flip ? 1 : -1;

        mainDriveWheel.setTransform(poseStack).translate(0, 1.1, 0).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).setChanged();
        driveWheel24_1.setTransform(poseStack).translate(0, 1.1, -1.6).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).setChanged();
        driveWheel24_2.setTransform(poseStack).translate(0, 1.1, 1.6).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).setChanged();
        driveWheel15_1.setTransform(poseStack).translate(0, 1.1, -3.2).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).setChanged();
        driveWheel15_2.setTransform(poseStack).translate(0, 1.1, 3.2).rotateYDegrees(yRot).rotateXDegrees(sign * wheelAngle).setChanged();

        crank.setTransform(poseStack).translate(flip ? -1.15 : 1.15, 1.13, flip ? 2.60 : -2.80).setChanged();
        crankOtherside.setTransform(poseStack).translate(flip ? 1.15 : -1.15, 1.13, flip ? 2.80 : -2.80).setChanged();
        expansionLink.setTransform(poseStack).translate(flip ? -1.32 : 1.32, 1.74, flip ? 1.79 : -1.79).setChanged();
        expansionLinkOtherside.setTransform(poseStack).translate(flip ? 1.32 : -1.32, 1.74, flip ? 1.79 : -1.79).setChanged();
        connectingRod.setTransform(poseStack).translate(flip ? -0.96 : 0.96, 1.5, 0).setChanged();
        connectingRodOtherside.setTransform(poseStack).translate(flip ? 0.96 : -0.96, 1.5, 0).setChanged();
        eccentricRod.setTransform(poseStack).translate(1.36, 1.10, 0.23).setChanged();
        eccentricRodOtherside.setTransform(poseStack).translate(-1.36, 1.10, 0.23).setChanged();
        crosshead.setTransform(poseStack).translate(1.15, 1.13, -2.85).setChanged();
        crossheadOtherside.setTransform(poseStack).translate(-1.15, 1.13, -2.85).setChanged();
        anchorLink.setTransform(poseStack).translate(1.30, 0.83, -2.85).setChanged();
        anchorLinkOtherside.setTransform(poseStack).translate(-1.30, 0.83, -2.85).setChanged();
        combinationLever.setTransform(poseStack).translate(1.30, 1.75, -3.33).setChanged();
        combinationLeverOtherside.setTransform(poseStack).translate(-1.30, 1.75, -3.33).setChanged();
        steamValve.setTransform(poseStack).translate(1.30, 1.62, -3.92).setChanged();
        steamValveOtherside.setTransform(poseStack).translate(-1.30, 1.62, -3.92).setChanged();
        radiusRod.setTransform(poseStack).translate(1.31, 1.80, -3.38).setChanged();
        radiusRodOtherside.setTransform(poseStack).translate(-1.31, 1.80, -3.38).setChanged();
        oilPressRod.setTransform(poseStack).translate(1.29, 1.98, -3.23).setChanged();
        oilPressRodOtherside.setTransform(poseStack).translate(-1.29, 1.99, -3.32).setChanged();
        oilPress.setTransform(poseStack).translate(1.29, 2.16, -3.42).setChanged();
        oilPressOtherside.setTransform(poseStack).translate(-1.29, 2.16, -3.42).setChanged();
        reversingBoom.setTransform(poseStack).translate(1.18, 2.18, -2.13).setChanged();
        reversingBoomOtherside.setTransform(poseStack).translate(-1.18, 2.18, -2.13).setChanged();
        reversing.setTransform(poseStack).translate(0, 2.15, -1.83).setChanged();

        if (flip) {
            poseStack.popPose();
        }
    }

    @Override public void hide() {
        apply(TransformedInstance::setZeroTransform);
    }

    @Override public void updateLight(int packedLight) {
        apply(i -> i.light(packedLight).setChanged());
    }

    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        apply(consumer);
    }

    @Override public void delete() {
        apply(TransformedInstance::delete);
    }

    private void apply(Consumer<? super TransformedInstance> action) {
        action.accept(mainDriveWheel); action.accept(driveWheel24_1); action.accept(driveWheel24_2);
        action.accept(driveWheel15_1); action.accept(driveWheel15_2); action.accept(crank);
        action.accept(crankOtherside); action.accept(expansionLink); action.accept(expansionLinkOtherside);
        action.accept(connectingRod); action.accept(connectingRodOtherside);
        action.accept(eccentricRod); action.accept(eccentricRodOtherside);
        action.accept(crosshead); action.accept(crossheadOtherside);
        action.accept(anchorLink); action.accept(anchorLinkOtherside);
        action.accept(combinationLever); action.accept(combinationLeverOtherside);
        action.accept(steamValve); action.accept(steamValveOtherside);
        action.accept(radiusRod); action.accept(radiusRodOtherside);
        action.accept(oilPress); action.accept(oilPressOtherside);
        action.accept(oilPressRod); action.accept(oilPressRodOtherside);
        action.accept(reversingBoom); action.accept(reversingBoomOtherside);
        action.accept(reversing); action.accept(bogeyFrame);
    }

    public static class Andesite extends QJMainVisual {
        public Andesite(VisualizationContext ctx, float partialTick, boolean inContraption) {
            super(ctx, partialTick, inContraption);
        }

        @Override
        public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
            poseStack.pushPose();
            poseStack.scale(1.2F, 1, 1);
            super.update(bogeyData, wheelAngle, poseStack);
            poseStack.popPose();
        }
    }
}
