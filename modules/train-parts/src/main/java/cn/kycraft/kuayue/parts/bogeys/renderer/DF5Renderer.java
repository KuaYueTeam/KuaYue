package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class DF5Renderer implements BogeyRenderer {

    public static final PartialModel DF5_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/df5/df5_frame"));
    public static final PartialModel DF5_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/df5/df5_wheel"));

    private static final float FRAME_TRANS_Y = 1.00f;
    private static final float WHEEL_TRANS_Y = 0.878f;
    private static final float WHEEL_TRANS_Z = 1.995f;

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

        CachedBuffers.partial(DF5_FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, FRAME_TRANS_Y, 0)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        float angle = inContraption ? (isBackward ? wheelAngle : -wheelAngle) : 0;
        renderWheel(poseStack, buffer, 0, angle, light, overlay);
        renderWheel(poseStack, buffer, -WHEEL_TRANS_Z, angle, light, overlay);
        renderWheel(poseStack, buffer, WHEEL_TRANS_Z, angle, light, overlay);

        if (yRot) {
            poseStack.popPose();
        }
    }

    private void renderWheel(PoseStack poseStack, VertexConsumer buffer, float z, float angle, int light, int overlay) {
        CachedBuffers.partial(DF5_WHEEL, Blocks.AIR.defaultBlockState())
                .translate(0, WHEEL_TRANS_Y, z)
                .rotateXDegrees(angle)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);
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

            CachedBuffers.partial(DF5_FRAME, Blocks.AIR.defaultBlockState())
                    .translate(0, FRAME_TRANS_Y, 0)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            float angle = inContraption ? wheelAngle : 0;
            renderWheel(poseStack, buffer, 0, angle, light, overlay);
            renderWheel(poseStack, buffer, -WHEEL_TRANS_Z, angle, light, overlay);
            renderWheel(poseStack, buffer, WHEEL_TRANS_Z, angle, light, overlay);

            if (yRot) {
                poseStack.popPose();
            }
        }

        private void renderWheel(PoseStack poseStack, VertexConsumer buffer, float z, float angle, int light, int overlay) {
            CachedBuffers.partial(DF5_WHEEL, Blocks.AIR.defaultBlockState())
                    .translate(0, WHEEL_TRANS_Y, z)
                    .rotateXDegrees(angle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }
    }
}
