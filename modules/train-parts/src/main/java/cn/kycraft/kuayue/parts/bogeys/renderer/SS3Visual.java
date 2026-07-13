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

public class SS3Visual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;
    private final TransformedInstance wheel3;

    public SS3Visual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS3Renderer.SS3_FRAME))
                .createInstance();
        var wi = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS3Renderer.SS3_WHEEL));
        wheel1 = wi.createInstance();
        wheel2 = wi.createInstance();
        wheel3 = wi.createInstance();
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

        frame.setTransform(poseStack).translate(0, 0.012, 0).setChanged();
        wheel1.setTransform(poseStack).translate(0, 0.88, -2.11).rotateXDegrees(wheelAngle).setChanged();
        wheel2.setTransform(poseStack).translate(0, 0.88, 1.96).rotateXDegrees(wheelAngle).setChanged();
        wheel3.setTransform(poseStack).translate(0, 0.88, -0.01).rotateXDegrees(wheelAngle).setChanged();

        if (flip) {
            poseStack.popPose();
        }
    }

    @Override public void hide() {
        frame.setZeroTransform().setChanged();
        wheel1.setZeroTransform().setChanged();
        wheel2.setZeroTransform().setChanged();
        wheel3.setZeroTransform().setChanged();
    }

    @Override public void updateLight(int packedLight) {
        frame.light(packedLight).setChanged();
        wheel1.light(packedLight).setChanged();
        wheel2.light(packedLight).setChanged();
        wheel3.light(packedLight).setChanged();
    }

    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(frame);
        consumer.accept(wheel1);
        consumer.accept(wheel2);
        consumer.accept(wheel3);
    }

    @Override public void delete() {
        frame.delete();
        wheel1.delete();
        wheel2.delete();
        wheel3.delete();
    }

    public static class Backward implements BogeyVisual {

        private final TransformedInstance frame;
        private final TransformedInstance wheel1;
        private final TransformedInstance wheel2;
        private final TransformedInstance wheel3;

        public Backward(VisualizationContext ctx, float partialTick, boolean inContraption) {
            frame = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS3Renderer.SS3_FRAME))
                    .createInstance();
            var wi = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS3Renderer.SS3_WHEEL));
            wheel1 = wi.createInstance();
            wheel2 = wi.createInstance();
            wheel3 = wi.createInstance();
        }

        @Override
        public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean flip = direction == Direction.SOUTH || direction == Direction.EAST;
            float wa = -wheelAngle;

            if (!flip) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            frame.setTransform(poseStack).translate(0, 0.012, 0).setChanged();
            wheel1.setTransform(poseStack).translate(0, 0.88, -0.01).rotateXDegrees(wa).setChanged();
            wheel2.setTransform(poseStack).translate(0, 0.88, 1.96).rotateXDegrees(wa).setChanged();
            wheel3.setTransform(poseStack).translate(0, 0.88, -2.11).rotateXDegrees(wa).setChanged();

            if (!flip) {
                poseStack.popPose();
            }
        }

        @Override public void hide() {
            frame.setZeroTransform().setChanged();
            wheel1.setZeroTransform().setChanged();
            wheel2.setZeroTransform().setChanged();
            wheel3.setZeroTransform().setChanged();
        }

        @Override public void updateLight(int packedLight) {
            frame.light(packedLight).setChanged();
            wheel1.light(packedLight).setChanged();
            wheel2.light(packedLight).setChanged();
            wheel3.light(packedLight).setChanged();
        }

        @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
            consumer.accept(frame);
            consumer.accept(wheel1);
            consumer.accept(wheel2);
            consumer.accept(wheel3);
        }

        @Override public void delete() {
            frame.delete();
            wheel1.delete();
            wheel2.delete();
            wheel3.delete();
        }
    }

    public static class Andesite extends SS3Visual {
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
