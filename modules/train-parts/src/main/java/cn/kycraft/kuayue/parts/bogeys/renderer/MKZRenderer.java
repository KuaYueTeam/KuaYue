package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class MKZRenderer implements BogeyRenderer {

    public static final PartialModel MKZ_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/mkz/mkz_frame"));
    public static final PartialModel MKZ_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/mkz/mkz_wheel"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        CachedBuffers.partial(MKZ_FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, 0.320, 0)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        SuperByteBuffer wheel = CachedBuffers.partial(MKZ_WHEEL, Blocks.AIR.defaultBlockState());
        for (int side : Iterate.positiveAndNegative) {
            wheel.translate(0, 0.695, side * 0.733)
                    .rotateXDegrees(wheelAngle * 1.22f)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }
    }
}
