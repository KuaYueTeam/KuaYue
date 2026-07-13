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

public class SS8Visual implements BogeyVisual {

    private static final double dp = 2.604;
    private static final double ad = 1.304;

    private final TransformedInstance frame;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;
    private final boolean inContraption;

    public SS8Visual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        this.inContraption = inContraption;
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS8Renderer.SS8_FRAME))
                .createInstance();
        var wi = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS8Renderer.SS8_WHEEL));
        wheel1 = wi.createInstance();
        wheel2 = wi.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean isSouthOrEast = direction == Direction.SOUTH || direction == Direction.EAST;
        boolean flip = !isSouthOrEast || !inContraption;

        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        frame.setTransform(poseStack).setChanged();
        wheel1.setTransform(poseStack).translate(0, 0.905, -dp + ad).rotateXDegrees(wheelAngle).setChanged();
        wheel2.setTransform(poseStack).translate(0, 0.905, ad).rotateXDegrees(wheelAngle).setChanged();

        if (flip) {
            poseStack.popPose();
        }
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
        consumer.accept(frame);
        consumer.accept(wheel1);
        consumer.accept(wheel2);
    }

    @Override public void delete() {
        frame.delete();
        wheel1.delete();
        wheel2.delete();
    }

    public static class Backward implements BogeyVisual {

        private static final double dp = 2.604;
        private static final double ad = 1.304;

        private final TransformedInstance frame;
        private final TransformedInstance wheel1;
        private final TransformedInstance wheel2;
        private final boolean inContraption;

        public Backward(VisualizationContext ctx, float partialTick, boolean inContraption) {
            this.inContraption = inContraption;
            frame = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS8Renderer.SS8_FRAME))
                    .createInstance();
            var wi = ctx.instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(SS8Renderer.SS8_WHEEL));
            wheel1 = wi.createInstance();
            wheel2 = wi.createInstance();
        }

        @Override
        public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean isSouthOrEast = direction == Direction.SOUTH || direction == Direction.EAST;
            boolean flip = isSouthOrEast && inContraption;

            if (flip) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            float wa = -wheelAngle;

            frame.setTransform(poseStack).setChanged();
            wheel1.setTransform(poseStack).translate(0, 0.905, -dp + ad).rotateXDegrees(wa).setChanged();
            wheel2.setTransform(poseStack).translate(0, 0.905, ad).rotateXDegrees(wa).setChanged();

            if (flip) {
                poseStack.popPose();
            }
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
            consumer.accept(frame);
            consumer.accept(wheel1);
            consumer.accept(wheel2);
        }

        @Override public void delete() {
            frame.delete();
            wheel1.delete();
            wheel2.delete();
        }
    }

    public static class Andesite extends SS8Visual {
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
