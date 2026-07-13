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

public class HXD3DVisual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel0;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;
    private final boolean inContraption;

    public HXD3DVisual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        this.inContraption = inContraption;
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(HXD3DRenderer.HXD3D_FRAME))
                .createInstance();
        var wi = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(HXD3DRenderer.HXD3D_WHEEL));
        wheel0 = wi.createInstance();
        wheel1 = wi.createInstance();
        wheel2 = wi.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean flip = direction == Direction.NORTH || direction == Direction.WEST || !inContraption;

        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        float renderAngle = flip ? -wheelAngle : wheelAngle;

        frame.setTransform(poseStack).translate(0, 0.012, 0).setChanged();
        wheel0.setTransform(poseStack).translate(0, 0.905, 0.23).rotateXDegrees(renderAngle).setChanged();
        wheel1.setTransform(poseStack).translate(0, 0.905, 2.365).rotateXDegrees(renderAngle).setChanged();
        wheel2.setTransform(poseStack).translate(0, 0.905, -1.69).rotateXDegrees(renderAngle).setChanged();

        if (flip) {
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
        consumer.accept(frame);
        consumer.accept(wheel0);
        consumer.accept(wheel1);
        consumer.accept(wheel2);
    }

    @Override public void delete() {
        frame.delete();
        wheel0.delete();
        wheel1.delete();
        wheel2.delete();
    }

    public static class Backward implements BogeyVisual {

        private final TransformedInstance frame;
        private final TransformedInstance wheel0;
        private final TransformedInstance wheel1;
        private final TransformedInstance wheel2;
        private final boolean inContraption;

        public Backward(VisualizationContext ctx, float partialTick, boolean inContraption) {
            this.inContraption = inContraption;
            frame = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(HXD3DRenderer.HXD3D_FRAME))
                    .createInstance();
            var wi = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(HXD3DRenderer.HXD3D_WHEEL));
            wheel0 = wi.createInstance();
            wheel1 = wi.createInstance();
            wheel2 = wi.createInstance();
        }

        @Override
        public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean flip = (direction == Direction.SOUTH || direction == Direction.EAST) && inContraption;

            if (flip) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            float renderAngle = flip ? -wheelAngle : wheelAngle;

            frame.setTransform(poseStack).translate(0, 0.012, 0).setChanged();
            wheel0.setTransform(poseStack).translate(0, 0.905, 0.23).rotateXDegrees(renderAngle).setChanged();
            wheel1.setTransform(poseStack).translate(0, 0.905, 2.365).rotateXDegrees(renderAngle).setChanged();
            wheel2.setTransform(poseStack).translate(0, 0.905, -1.69).rotateXDegrees(renderAngle).setChanged();

            if (flip) {
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
            consumer.accept(frame);
            consumer.accept(wheel0);
            consumer.accept(wheel1);
            consumer.accept(wheel2);
        }

        @Override public void delete() {
            frame.delete();
            wheel0.delete();
            wheel1.delete();
            wheel2.delete();
        }
    }

    public static class Andesite extends HXD3DVisual {
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
