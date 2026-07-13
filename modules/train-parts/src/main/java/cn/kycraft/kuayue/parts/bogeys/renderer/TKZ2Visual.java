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

public class TKZ2Visual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;

    public TKZ2Visual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_FRAME))
                .createInstance();

        var wheelInstancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_WHEEL));
        wheel1 = wheelInstancer.createInstance();
        wheel2 = wheelInstancer.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        frame.setTransform(poseStack).translate(0, 0.195, 0).setChanged();
        wheel1.setTransform(poseStack).translate(0, 0.695, -0.97).rotateXDegrees(wheelAngle * 1.22f).setChanged();
        wheel2.setTransform(poseStack).translate(0, 0.695, 0.97).rotateXDegrees(wheelAngle * 1.22f).setChanged();
    }

    @Override public void hide() {
        frame.setZeroTransform().setChanged();
        wheel1.setZeroTransform().setChanged();
        wheel2.setZeroTransform().setChanged();
    }

    @Override public void updateLight(int packedLight) {
        frame.light(packedLight).setChanged();
        wheel1.light(packedLight).setChanged();
        wheel2.light(packedLight).setChanged();
    }

    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(frame); consumer.accept(wheel1); consumer.accept(wheel2);
    }

    @Override public void delete() { frame.delete(); wheel1.delete(); wheel2.delete(); }

    public static class Motor implements BogeyVisual {

        private final TransformedInstance frame;
        private final TransformedInstance wheel;
        private final TransformedInstance motorWheel;
        private final TransformedInstance motorPonyWheel;

        public Motor(VisualizationContext ctx, float partialTick, boolean inContraption) {
            frame = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_MOTOR_FRAME))
                    .createInstance();
            var wheelInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_WHEEL));
            wheel = wheelInstancer.createInstance();
            var motorWheelInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_MOTOR_WHEEL));
            motorWheel = motorWheelInstancer.createInstance();
            var ponyInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_MOTOR_PONY_WHEEL));
            motorPonyWheel = ponyInstancer.createInstance();
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

            frame.setTransform(poseStack).translate(0, 0.129, 0).setChanged();
            wheel.setTransform(poseStack).translate(0, 0.695, 0.9761)
                    .rotateXDegrees(isBackward ? -wheelAngle * 1.22f : wheelAngle * 1.22f).setChanged();
            motorWheel.setTransform(poseStack).translate(0, 0.695, -0.9761)
                    .rotateXDegrees(isBackward ? -wheelAngle * 1.22f : wheelAngle * 1.22f).setChanged();
            motorPonyWheel.setTransform(poseStack).translate(0, 0.7235, -1.94)
                    .rotateXDegrees(isBackward ? -wheelAngle * 3.256f : wheelAngle * 3.256f).setChanged();

            if (isBackward) {
                poseStack.popPose();
            }
        }

        @Override public void hide() {
            frame.setZeroTransform().setChanged();
            wheel.setZeroTransform().setChanged();
            motorWheel.setZeroTransform().setChanged();
            motorPonyWheel.setZeroTransform().setChanged();
        }

        @Override public void updateLight(int packedLight) {
            frame.light(packedLight).setChanged();
            wheel.light(packedLight).setChanged();
            motorWheel.light(packedLight).setChanged();
            motorPonyWheel.light(packedLight).setChanged();
        }

        @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
            consumer.accept(frame); consumer.accept(wheel);
            consumer.accept(motorWheel); consumer.accept(motorPonyWheel);
        }

        @Override public void delete() {
            frame.delete(); wheel.delete(); motorWheel.delete(); motorPonyWheel.delete();
        }
    }

    public static class MotorBackward implements BogeyVisual {

        private final TransformedInstance frame;
        private final TransformedInstance wheel;
        private final TransformedInstance motorWheel;
        private final TransformedInstance motorPonyWheel;

        public MotorBackward(VisualizationContext ctx, float partialTick, boolean inContraption) {
            frame = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_MOTOR_FRAME))
                    .createInstance();
            var wheelInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_WHEEL));
            wheel = wheelInstancer.createInstance();
            var motorWheelInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_MOTOR_WHEEL));
            motorWheel = motorWheelInstancer.createInstance();
            var ponyInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ2Renderer.TKZ2_MOTOR_PONY_WHEEL));
            motorPonyWheel = ponyInstancer.createInstance();
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

            frame.setTransform(poseStack).translate(0, 0.129, 0).setChanged();
            wheel.setTransform(poseStack).translate(0, 0.695, 0.9761)
                    .rotateXDegrees(isBackward ? wheelAngle * 1.22f : -wheelAngle * 1.22f).setChanged();
            motorWheel.setTransform(poseStack).translate(0, 0.695, -0.9761)
                    .rotateXDegrees(isBackward ? wheelAngle * 1.22f : -wheelAngle * 1.22f).setChanged();
            motorPonyWheel.setTransform(poseStack).translate(0, 0.7235, -1.94)
                    .rotateXDegrees(isBackward ? wheelAngle * 3.256f : -wheelAngle * 3.256f).setChanged();

            if (!isBackward) {
                poseStack.popPose();
            }
        }

        @Override public void hide() {
            frame.setZeroTransform().setChanged();
            wheel.setZeroTransform().setChanged();
            motorWheel.setZeroTransform().setChanged();
            motorPonyWheel.setZeroTransform().setChanged();
        }

        @Override public void updateLight(int packedLight) {
            frame.light(packedLight).setChanged();
            wheel.light(packedLight).setChanged();
            motorWheel.light(packedLight).setChanged();
            motorPonyWheel.light(packedLight).setChanged();
        }

        @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
            consumer.accept(frame); consumer.accept(wheel);
            consumer.accept(motorWheel); consumer.accept(motorPonyWheel);
        }

        @Override public void delete() {
            frame.delete(); wheel.delete(); motorWheel.delete(); motorPonyWheel.delete();
        }
    }
}
