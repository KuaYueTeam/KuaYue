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

public class ZK6Visual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;

    public ZK6Visual(dev.engine_room.flywheel.api.visualization.VisualizationContext ctx,
                     float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(ZK6Renderer.FRAME))
                .createInstance();
        var wi = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(ZK6Renderer.WHEEL));
        wheel1 = wi.createInstance();
        wheel2 = wi.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        frame.setTransform(poseStack).translate(0, 0.380, 0).scale(1 - 1 / 512f).setChanged();
        wheel1.setTransform(poseStack).translate(0, 0.745, -0.917).rotateXDegrees(wheelAngle * 1.09f).setChanged();
        wheel2.setTransform(poseStack).translate(0, 0.745, 0.917).rotateXDegrees(wheelAngle * 1.09f).setChanged();
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

    public static class Andesite extends ZK6Visual {
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
