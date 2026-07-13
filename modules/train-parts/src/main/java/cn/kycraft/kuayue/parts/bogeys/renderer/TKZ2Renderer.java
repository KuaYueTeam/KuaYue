package cn.kycraft.kuayue.parts.bogeys.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class TKZ2Renderer implements BogeyRenderer {

    public static final PartialModel TKZ2_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/tkz2/tkz2_frame"));
    public static final PartialModel TKZ2_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/tkz2/tkz2_wheel"));
    public static final PartialModel TKZ2_MOTOR_FRAME = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/tkz2/tkz2_motor_frame"));
    public static final PartialModel TKZ2_MOTOR_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/tkz2/tkz2_motor_wheel"));
    public static final PartialModel TKZ2_MOTOR_PONY_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/tkz2/tkz2_motor_pony_wheel"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        CachedBuffers.partial(TKZ2_FRAME, Blocks.AIR.defaultBlockState())
                .translate(0, 0.195, 0)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        SuperByteBuffer wheel = CachedBuffers.partial(TKZ2_WHEEL, Blocks.AIR.defaultBlockState());
        for (int side : Iterate.positiveAndNegative) {
            wheel.translate(0, 0.695, side * 0.97)
                    .rotateXDegrees(wheelAngle * 1.22f)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }
    }

    public static class Motor implements BogeyRenderer {

        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;

            SuperByteBuffer frame = CachedBuffers.partial(TKZ2_MOTOR_FRAME, Blocks.AIR.defaultBlockState());
            SuperByteBuffer wheel = CachedBuffers.partial(TKZ2_WHEEL, Blocks.AIR.defaultBlockState());
            SuperByteBuffer motorWheel = CachedBuffers.partial(TKZ2_MOTOR_WHEEL, Blocks.AIR.defaultBlockState());
            SuperByteBuffer motorPonyWheel = CachedBuffers.partial(TKZ2_MOTOR_PONY_WHEEL, Blocks.AIR.defaultBlockState());

            if (isBackward) {
                poseStack.pushPose();
                poseStack.translate(0, 0, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                frame.translate(0, 0.129, 0)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                wheel.translate(0, 0.695, 0.9761)
                        .rotateXDegrees(-wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorWheel.translate(0, 0.695, -0.9761)
                        .rotateXDegrees(-wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorPonyWheel.translate(0, 0.7235, -1.94)
                        .rotateXDegrees(-wheelAngle * 3.256f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                poseStack.popPose();
            } else {
                frame.translate(0, 0.129, 0)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                wheel.translate(0, 0.695, 0.9761)
                        .rotateXDegrees(wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorWheel.translate(0, 0.695, -0.9761)
                        .rotateXDegrees(wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorPonyWheel.translate(0, 0.7235, -1.94)
                        .rotateXDegrees(wheelAngle * 3.256f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
            }
        }
    }

    public static class MotorBackward implements BogeyRenderer {

        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;

            SuperByteBuffer frame = CachedBuffers.partial(TKZ2_MOTOR_FRAME, Blocks.AIR.defaultBlockState());
            SuperByteBuffer wheel = CachedBuffers.partial(TKZ2_WHEEL, Blocks.AIR.defaultBlockState());
            SuperByteBuffer motorWheel = CachedBuffers.partial(TKZ2_MOTOR_WHEEL, Blocks.AIR.defaultBlockState());
            SuperByteBuffer motorPonyWheel = CachedBuffers.partial(TKZ2_MOTOR_PONY_WHEEL, Blocks.AIR.defaultBlockState());

            if (isBackward) {
                frame.translate(0, 0.129, 0)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                wheel.translate(0, 0.695, 0.9761)
                        .rotateXDegrees(wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorWheel.translate(0, 0.695, -0.9761)
                        .rotateXDegrees(wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorPonyWheel.translate(0, 0.7235, -1.94)
                        .rotateXDegrees(wheelAngle * 3.256f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
            } else {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                frame.translate(0, 0.129, 0)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                wheel.translate(0, 0.695, 0.9761)
                        .rotateXDegrees(-wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorWheel.translate(0, 0.695, -0.9761)
                        .rotateXDegrees(-wheelAngle * 1.22f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                motorPonyWheel.translate(0, 0.7235, -1.94)
                        .rotateXDegrees(-wheelAngle * 3.256f)
                        .light(light).overlay(overlay)
                        .renderInto(poseStack, buffer);
                poseStack.popPose();
            }
        }
    }
}
