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

public class DF11GRenderer implements BogeyRenderer {

    public static final PartialModel DF11G_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/df11g/df11g_bogey_temple"));
    public static final PartialModel DF11G_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/df11g/df11g_wheel"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        if (isBackward) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        CachedBuffers.partial(DF11G_FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, 0.375, 0)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        for (int side : new int[]{-1, 0, 1}) {
            CachedBuffers.partial(DF11G_WHEEL, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.88, side * 2d)
                    .rotateXDegrees(wheelAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }

        if (isBackward) {
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

            if (!isBackward) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            CachedBuffers.partial(DF11G_FRAME, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.375, 0)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            float negatedAngle = -wheelAngle;
            for (int side : new int[]{-1, 0, 1}) {
                CachedBuffers.partial(DF11G_WHEEL, Blocks.AIR.defaultBlockState())
                        .translate(0, 0.88, side * 2d)
                        .rotateXDegrees(negatedAngle)
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);
            }

            if (!isBackward) {
                poseStack.popPose();
            }
        }
    }

    public static class Andesite extends DF11GRenderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light,
                           int overlay, boolean inContraption) {
            poseStack.pushPose();
            poseStack.scale(1.2F, 1, 1);
            super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);
            poseStack.popPose();
        }
    }
}
