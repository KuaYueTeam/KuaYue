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

public class QJGuideRenderer implements BogeyRenderer {

    public static final PartialModel QJ_GUIDE_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/qj/qj_guide_frame"));
    public static final PartialModel QJ_GUIDE_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/qj/qj_guide_wheel"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        boolean flip = direction == Direction.SOUTH || direction == Direction.EAST;
        if (flip) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        CachedBuffers.partial(QJ_GUIDE_FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, 0, 0.3)
                .light(light).overlay(overlay)
                .renderInto(poseStack, buffer);

        CachedBuffers.partial(QJ_GUIDE_WHEEL, Blocks.AIR.defaultBlockState())
                .translate(0, 0.77, -2)
                .rotateXDegrees(wheelAngle)
                .light(light).overlay(overlay)
                .renderInto(poseStack, buffer);

        if (flip) {
            poseStack.popPose();
        }
    }

    public static class Andesite extends QJGuideRenderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            poseStack.pushPose();
            poseStack.scale(1.2F, 1, 1);
            super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);
            poseStack.popPose();
        }
    }
}
