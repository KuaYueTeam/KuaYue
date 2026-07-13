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

public class HXD3DRenderer implements BogeyRenderer {

    public static final PartialModel HXD3D_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/hxd3d/hxd3d_frame"));
    public static final PartialModel HXD3D_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/hxd3d/hxd3d_wheel"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        boolean flip = direction == Direction.NORTH || direction == Direction.WEST || !inContraption;

        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        CachedBuffers.partial(HXD3D_FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, 0.012, 0)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        float renderAngle = flip ? -wheelAngle : wheelAngle;

        CachedBuffers.partial(HXD3D_WHEEL, Blocks.AIR.defaultBlockState())
                .translate(0, 0.905, 0.23)
                .rotateXDegrees(renderAngle)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        CachedBuffers.partial(HXD3D_WHEEL, Blocks.AIR.defaultBlockState())
                .translate(0, 0.905, 2.365)
                .rotateXDegrees(renderAngle)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        CachedBuffers.partial(HXD3D_WHEEL, Blocks.AIR.defaultBlockState())
                .translate(0, 0.905, -1.69)
                .rotateXDegrees(renderAngle)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

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

            boolean flip = (direction == Direction.SOUTH || direction == Direction.EAST) && inContraption;

            if (flip) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            CachedBuffers.partial(HXD3D_FRAME, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.012, 0)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            float renderAngle = flip ? -wheelAngle : wheelAngle;

            CachedBuffers.partial(HXD3D_WHEEL, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.905, 0.23)
                    .rotateXDegrees(renderAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            CachedBuffers.partial(HXD3D_WHEEL, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.905, 2.365)
                    .rotateXDegrees(renderAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            CachedBuffers.partial(HXD3D_WHEEL, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.905, -1.69)
                    .rotateXDegrees(renderAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            if (flip) {
                poseStack.popPose();
            }
        }
    }

    public static class Andesite extends HXD3DRenderer {
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
