package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class SW220KRenderer implements BogeyRenderer {

    public static final PartialModel FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/sw220k/sw220k_frame"));
    public static final PartialModel WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/sw220k/sw220k_wheel"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        CachedBuffers.partial(FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, 0.925, 0)
                .scale(1 - 1 / 512f)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        SuperByteBuffer wheel = CachedBuffers.partial(WHEEL, Blocks.AIR.defaultBlockState());
        for (int side : new int[]{-1, 1}) {
            wheel.translate(0, 0.8, side * 1.18)
                    .rotateXDegrees(wheelAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }
    }

    public static class Andesite extends SW220KRenderer {
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
