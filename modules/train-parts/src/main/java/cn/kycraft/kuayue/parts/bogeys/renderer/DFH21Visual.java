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

public class DFH21Visual implements BogeyVisual {

    private final TransformedInstance main;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;
    private final TransformedInstance transmissionRod;
    private final boolean inContraption;

    public DFH21Visual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        this.inContraption = inContraption;

        main = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(DFH21Renderer.DFH21_MAIN))
                .createInstance();

        var wheelInstancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(DFH21Renderer.DFH21_WHEEL));
        wheel1 = wheelInstancer.createInstance();
        wheel2 = wheelInstancer.createInstance();

        transmissionRod = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(DFH21Renderer.DFH21_TRANSMISSION_ROD))
                .createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;
        boolean flipY = !isBackward || !inContraption;
        boolean negateAngle = isBackward && !inContraption;

        if (flipY) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        float angle = negateAngle ? -wheelAngle : wheelAngle;

        main.setTransform(poseStack).setChanged();
        wheel1.setTransform(poseStack)
                .translate(0, 0.9, -DFH21Renderer.dp + DFH21Renderer.ad)
                .rotateXDegrees(angle)
                .setChanged();
        wheel2.setTransform(poseStack)
                .translate(0, 0.9, DFH21Renderer.ad)
                .rotateXDegrees(angle)
                .setChanged();
        transmissionRod.setTransform(poseStack)
                .translate(0, 0.85, 0)
                .rotateZDegrees(angle * 3.256f)
                .setChanged();

        if (flipY) {
            poseStack.popPose();
        }
    }

    @Override
    public void hide() {
        main.setZeroTransform().setChanged();
        wheel1.setZeroTransform().setChanged();
        wheel2.setZeroTransform().setChanged();
        transmissionRod.setZeroTransform().setChanged();
    }

    @Override
    public void updateLight(int packedLight) {
        main.light(packedLight).setChanged();
        wheel1.light(packedLight).setChanged();
        wheel2.light(packedLight).setChanged();
        transmissionRod.light(packedLight).setChanged();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(main);
        consumer.accept(wheel1);
        consumer.accept(wheel2);
        consumer.accept(transmissionRod);
    }

    @Override
    public void delete() {
        main.delete();
        wheel1.delete();
        wheel2.delete();
        transmissionRod.delete();
    }

    public static class Standard implements BogeyVisual {

        private final TransformedInstance main;
        private final TransformedInstance wheel1;
        private final TransformedInstance wheel2;
        private final TransformedInstance transmissionRod;
        private final boolean inContraption;

        public Standard(VisualizationContext ctx, float partialTick, boolean inContraption) {
            this.inContraption = inContraption;

            main = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(DFH21Renderer.DFH21_MAIN_STANDARD))
                    .createInstance();

            var wheelInstancer = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(DFH21Renderer.DFH21_WHEEL_STANDARD));
            wheel1 = wheelInstancer.createInstance();
            wheel2 = wheelInstancer.createInstance();

            transmissionRod = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(DFH21Renderer.DFH21_TRANSMISSION_ROD_STANDARD))
                    .createInstance();
        }

        @Override
        public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;
            boolean flipY = !isBackward || !inContraption;
            boolean negateAngle = isBackward && !inContraption;

            if (flipY) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            float angle = negateAngle ? -wheelAngle : wheelAngle;

            main.setTransform(poseStack).setChanged();
            wheel1.setTransform(poseStack)
                    .translate(0, 0.875, -DFH21Renderer.dp + DFH21Renderer.ad)
                    .rotateXDegrees(angle)
                    .setChanged();
            wheel2.setTransform(poseStack)
                    .translate(0, 0.875, DFH21Renderer.ad)
                    .rotateXDegrees(angle)
                    .setChanged();
            transmissionRod.setTransform(poseStack)
                    .translate(0, 0.85, 0)
                    .rotateZDegrees(angle * 3.256f)
                    .setChanged();

            if (flipY) {
                poseStack.popPose();
            }
        }

        @Override
        public void hide() {
            main.setZeroTransform().setChanged();
            wheel1.setZeroTransform().setChanged();
            wheel2.setZeroTransform().setChanged();
            transmissionRod.setZeroTransform().setChanged();
        }

        @Override
        public void updateLight(int packedLight) {
            main.light(packedLight).setChanged();
            wheel1.light(packedLight).setChanged();
            wheel2.light(packedLight).setChanged();
            transmissionRod.light(packedLight).setChanged();
        }

        @Override
        public void collectCrumblingInstances(Consumer<Instance> consumer) {
            consumer.accept(main);
            consumer.accept(wheel1);
            consumer.accept(wheel2);
            consumer.accept(transmissionRod);
        }

        @Override
        public void delete() {
            main.delete();
            wheel1.delete();
            wheel2.delete();
            transmissionRod.delete();
        }
    }

    public static class Andesite extends Standard {
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
