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

public class SS8Renderer implements BogeyRenderer {

    public static final PartialModel SS8_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/ss8/ss8_frame"));
    public static final PartialModel SS8_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/ss8/ss8_wheel"));

    private static final double dp = 2.604;
    private static final double ad = 1.304;

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        boolean isSouthOrEast = direction == Direction.SOUTH || direction == Direction.EAST;
        boolean flip = !isSouthOrEast || !inContraption;

        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        CachedBuffers.partial(SS8_FRAME, Blocks.AIR.defaultBlockState())
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        SuperByteBuffer wheel = CachedBuffers.partial(SS8_WHEEL, Blocks.AIR.defaultBlockState());
        for (int side : new int[]{-1, 0}) {
            wheel.translate(0, 0.905, side * dp + ad)
                    .rotateXDegrees(wheelAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }

        if (flip) {
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

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            boolean isSouthOrEast = direction == Direction.SOUTH || direction == Direction.EAST;
            boolean flip = isSouthOrEast && inContraption;

            if (flip) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            CachedBuffers.partial(SS8_FRAME, Blocks.AIR.defaultBlockState())
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            float wa = -wheelAngle;
            SuperByteBuffer wheel = CachedBuffers.partial(SS8_WHEEL, Blocks.AIR.defaultBlockState());
            for (int side : new int[]{-1, 0}) {
                wheel.translate(0, 0.905, side * dp + ad)
                        .rotateXDegrees(wa)
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);
            }

            if (flip) {
                poseStack.popPose();
            }
        }
    }

    public static class Andesite extends SS8Renderer {
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
