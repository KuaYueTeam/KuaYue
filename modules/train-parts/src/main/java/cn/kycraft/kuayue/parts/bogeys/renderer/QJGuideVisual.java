package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.bogey.BogeyVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class QJGuideVisual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel;

    public QJGuideVisual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(QJGuideRenderer.QJ_GUIDE_FRAME))
                .createInstance();

        wheel = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(QJGuideRenderer.QJ_GUIDE_WHEEL))
                .createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean flip = direction == Direction.SOUTH || direction == Direction.EAST;
        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        frame.setTransform(poseStack).translate(0, 0, 0.3).setChanged();
        wheel.setTransform(poseStack).translate(0, 0.77, -2).rotateXDegrees(wheelAngle).setChanged();

        if (flip) {
            poseStack.popPose();
        }
    }

    @Override public void hide() {
        frame.setZeroTransform().setChanged();
        wheel.setZeroTransform().setChanged();
    }

    @Override public void updateLight(int packedLight) {
        frame.light(packedLight).setChanged();
        wheel.light(packedLight).setChanged();
    }

    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(frame);
        consumer.accept(wheel);
    }

    @Override public void delete() { frame.delete(); wheel.delete(); }

    public static class Andesite extends QJGuideVisual {
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
