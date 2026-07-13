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

public class TKZ1Visual implements BogeyVisual {

    private final TransformedInstance frame;
    private final TransformedInstance wheel1;
    private final TransformedInstance wheel2;

    public TKZ1Visual(VisualizationContext ctx, float partialTick, boolean inContraption) {
        frame = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ1Renderer.TKZ1_FRAME))
                .createInstance();

        var wheelInstancer = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(TKZ1Renderer.TKZ1_WHEEL));
        wheel1 = wheelInstancer.createInstance();
        wheel2 = wheelInstancer.createInstance();
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        frame.setTransform(poseStack).translate(0, 0.14, 0).setChanged();
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
}
