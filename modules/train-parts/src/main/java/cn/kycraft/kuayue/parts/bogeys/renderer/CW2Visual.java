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

public class CW2Visual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;

    public CW2Visual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(CW2Renderer.CW2_FRAME))
                .createInstance();

        var wheelInstancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(CW2Renderer.CW2_WHEEL));
        wheel1 = wheelInstancer.createInstance();
        wheel2 = wheelInstancer.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        frame.setTransform(poseStack).scale(1 - 1 / 512f).setChanged();
        wheel1.setTransform(poseStack).translate(0, 0.805, -1.18).rotateXDegrees(wheelAngle).setChanged();
        wheel2.setTransform(poseStack).translate(0, 0.805, 1.18).rotateXDegrees(wheelAngle).setChanged();
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

    public static class Andesite extends CW2Visual {
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
