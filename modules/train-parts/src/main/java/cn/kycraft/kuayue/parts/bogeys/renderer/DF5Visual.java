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

public class DF5Visual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel0;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;

    public DF5Visual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(DF5Renderer.DF5_FRAME))
                .createInstance();

        var wheelInstancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(DF5Renderer.DF5_WHEEL));
        wheel0 = wheelInstancer.createInstance();
        wheel1 = wheelInstancer.createInstance();
        wheel2 = wheelInstancer.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;

        if (!isBackward) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        frame.setTransform(poseStack).translate(0, 1.00f, 0).setChanged();
        float angle = isBackward ? wheelAngle : -wheelAngle;
        wheel0.setTransform(poseStack).translate(0, 0.878f, 0).rotateXDegrees(angle).setChanged();
        wheel1.setTransform(poseStack).translate(0, 0.878f, -1.995f).rotateXDegrees(angle).setChanged();
        wheel2.setTransform(poseStack).translate(0, 0.878f, 1.995f).rotateXDegrees(angle).setChanged();

        if (!isBackward) {
            poseStack.popPose();
        }
    }

    @Override public void hide() {
        frame.setZeroTransform().setChanged();
        wheel0.setZeroTransform().setChanged();
        wheel1.setZeroTransform().setChanged();
        wheel2.setZeroTransform().setChanged();
    }

    @Override public void updateLight(int packedLight) {
        frame.light(packedLight).setChanged();
        wheel0.light(packedLight).setChanged();
        wheel1.light(packedLight).setChanged();
        wheel2.light(packedLight).setChanged();
    }

    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(frame); consumer.accept(wheel0); consumer.accept(wheel1); consumer.accept(wheel2);
    }

    @Override public void delete() { frame.delete(); wheel0.delete(); wheel1.delete(); wheel2.delete(); }

    public static class Backward implements BogeyVisual {

        private final TransformedInstance frame;
        private final TransformedInstance wheel0;
        private final TransformedInstance wheel1;
        private final TransformedInstance wheel2;

        public Backward(VisualizationContext ctx, float partialTick, boolean inContraption) {
            frame = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(DF5Renderer.DF5_FRAME))
                    .createInstance();

            var wheelInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(DF5Renderer.DF5_WHEEL));
            wheel0 = wheelInstancer.createInstance();
            wheel1 = wheelInstancer.createInstance();
            wheel2 = wheelInstancer.createInstance();
        }

        @Override
        public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;

            if (isBackward) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            frame.setTransform(poseStack).translate(0, 1.00f, 0).setChanged();
            wheel0.setTransform(poseStack).translate(0, 0.878f, 0).rotateXDegrees(wheelAngle).setChanged();
            wheel1.setTransform(poseStack).translate(0, 0.878f, -1.995f).rotateXDegrees(wheelAngle).setChanged();
            wheel2.setTransform(poseStack).translate(0, 0.878f, 1.995f).rotateXDegrees(wheelAngle).setChanged();

            if (isBackward) {
                poseStack.popPose();
            }
        }

        @Override public void hide() {
            frame.setZeroTransform().setChanged();
            wheel0.setZeroTransform().setChanged();
            wheel1.setZeroTransform().setChanged();
            wheel2.setZeroTransform().setChanged();
        }

        @Override public void updateLight(int packedLight) {
            frame.light(packedLight).setChanged();
            wheel0.light(packedLight).setChanged();
            wheel1.light(packedLight).setChanged();
            wheel2.light(packedLight).setChanged();
        }

        @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
            consumer.accept(frame); consumer.accept(wheel0); consumer.accept(wheel1); consumer.accept(wheel2);
        }

        @Override public void delete() { frame.delete(); wheel0.delete(); wheel1.delete(); wheel2.delete(); }
    }
}
