package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.bogey.BogeyVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class PK209PVisual implements BogeyVisual {

    private final TransformedInstance main;
    private final TransformedInstance wheel;
    private final TransformedInstance wheel2;
    private final TransformedInstance motorWheel;

    public PK209PVisual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        main = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(PK209PRenderer.MAIN))
                .createInstance();

        var wheelInstancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(PK209PRenderer.WHEEL));
        wheel = wheelInstancer.createInstance();

        var wheel2Instancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(PK209PRenderer.WHEEL2));
        wheel2 = wheel2Instancer.createInstance();

        motorWheel = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(PK209PRenderer.MOTOR_WHEEL))
                .createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        main.setTransform(poseStack)
                .translate(0, 0.91, 0)
                .scale(1 - 1 / 512f)
                .setChanged();

        wheel.setTransform(poseStack)
                .translate(0, 0.8, 1.2)
                .rotateXDegrees(wheelAngle)
                .setChanged();

        wheel2.setTransform(poseStack)
                .translate(0, 0.8, -1.2)
                .rotateXDegrees(wheelAngle)
                .setChanged();

        motorWheel.setTransform(poseStack)
                .translate(1.117, 0.82, 2.165)
                .rotateXDegrees(wheelAngle * 3.256f)
                .setChanged();
    }

    @Override
    public void hide() {
        main.setZeroTransform().setChanged();
        wheel.setZeroTransform().setChanged();
        wheel2.setZeroTransform().setChanged();
        motorWheel.setZeroTransform().setChanged();
    }

    @Override
    public void updateLight(int packedLight) {
        main.light(packedLight).setChanged();
        wheel.light(packedLight).setChanged();
        wheel2.light(packedLight).setChanged();
        motorWheel.light(packedLight).setChanged();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(main);
        consumer.accept(wheel);
        consumer.accept(wheel2);
        consumer.accept(motorWheel);
    }

    @Override
    public void delete() {
        main.delete();
        wheel.delete();
        wheel2.delete();
        motorWheel.delete();
    }

    public static class Andesite extends PK209PVisual {
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
