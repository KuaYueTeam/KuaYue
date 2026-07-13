package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class DF21Renderer implements BogeyRenderer {

    public static final PartialModel DF21_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/df21/df21_bogey_temple"));
    public static final PartialModel DF21_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/df21/df21_wheel"));

    private static final float FRAME_TRANS_Y = 0.225f;
    private static final float FRAME_TRANS_Z = 0.17f;
    private static final float WHEEL_TRANS_Y = 0.88f;
    private static final float WHEEL_TRANS_Z = 1.805f;

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        boolean yRot = !inContraption || !isBackward;

        if (yRot) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        CachedBuffers.partial(DF21_FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, FRAME_TRANS_Y, FRAME_TRANS_Z)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        for (int side = -1; side <= 1; side++) {
            CachedBuffers.partial(DF21_WHEEL, Blocks.AIR.defaultBlockState())
                    .translate(0, WHEEL_TRANS_Y, side * WHEEL_TRANS_Z)
                    .rotateXDegrees(wheelAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }

        if (yRot) {
            poseStack.popPose();
        }
    }

    public static class Backward implements BogeyRenderer {

        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            boolean yRot = inContraption && isBackward;

            if (yRot) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            CachedBuffers.partial(DF21_FRAME, Blocks.AIR.defaultBlockState())
                    .translate(0, FRAME_TRANS_Y, FRAME_TRANS_Z)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            for (int side = -1; side <= 1; side++) {
                CachedBuffers.partial(DF21_WHEEL, Blocks.AIR.defaultBlockState())
                        .translate(0, WHEEL_TRANS_Y, side * WHEEL_TRANS_Z)
                        .rotateXDegrees(-wheelAngle)
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);
            }

            if (yRot) {
                poseStack.popPose();
            }
        }
    }
}
