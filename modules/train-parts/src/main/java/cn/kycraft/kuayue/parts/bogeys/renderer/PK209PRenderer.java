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

public class PK209PRenderer implements BogeyRenderer {

    public static final PartialModel MAIN = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/pk209p/pk209p_main"));
    public static final PartialModel WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/pk209p/pk209p_wheel"));
    public static final PartialModel WHEEL2 = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/pk209p/pk209p_wheel2"));
    public static final PartialModel MOTOR_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/pk209p/pk209_motorwheel"));
    public static final PartialModel NO_MOTOR = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/pk209p/pk209_nomotor"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        CachedBuffers.partial(MAIN, Blocks.AIR.defaultBlockState())
                .translate(0, 0.91, 0)
                .scale(1 - 1 / 512f)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        SuperByteBuffer wheel = CachedBuffers.partial(WHEEL, Blocks.AIR.defaultBlockState());
        wheel.translate(0, 0.8, 1.2)
                .rotateXDegrees(wheelAngle)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        SuperByteBuffer wheel2Buf = CachedBuffers.partial(WHEEL2, Blocks.AIR.defaultBlockState());
        wheel2Buf.translate(0, 0.8, -1.2)
                .rotateXDegrees(wheelAngle)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        SuperByteBuffer motorWheel = CachedBuffers.partial(MOTOR_WHEEL, Blocks.AIR.defaultBlockState());
        motorWheel.translate(1.117, 0.82, 2.165)
                .rotateXDegrees(wheelAngle * 3.256f)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);
    }

    public static class Backward extends PK209PRenderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light,
                           int overlay, boolean inContraption) {
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            CachedBuffers.partial(MAIN, Blocks.AIR.defaultBlockState())
                    .rotateYDegrees(180)
                    .translate(0, 0.91, 0)
                    .scale(1 - 1 / 512f)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            SuperByteBuffer wheel = CachedBuffers.partial(WHEEL, Blocks.AIR.defaultBlockState());
            wheel.rotateYDegrees(180)
                    .translate(0, 0.8, -1.2)
                    .rotateXDegrees(-wheelAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            SuperByteBuffer wheel2Buf = CachedBuffers.partial(WHEEL2, Blocks.AIR.defaultBlockState());
            wheel2Buf.rotateYDegrees(180)
                    .translate(0, 0.8, 1.2)
                    .rotateXDegrees(-wheelAngle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            SuperByteBuffer motorWheel = CachedBuffers.partial(MOTOR_WHEEL, Blocks.AIR.defaultBlockState());
            motorWheel.rotateYDegrees(180)
                    .translate(-1.117, 0.82, -2.165)
                    .rotateXDegrees(-wheelAngle * 3.256f)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }
    }

    public static class NoMotor extends PK209PRenderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light,
                           int overlay, boolean inContraption) {
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            CachedBuffers.partial(NO_MOTOR, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.91, 0)
                    .scale(1 - 1 / 512f)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            SuperByteBuffer wheel = CachedBuffers.partial(WHEEL2, Blocks.AIR.defaultBlockState());
            for (int side : new int[]{-1, 1}) {
                wheel.translate(0, 0.8, side * 1.2)
                        .rotateXDegrees(wheelAngle)
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);
            }
        }
    }

    public static class Andesite extends PK209PRenderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light,
                           int overlay, boolean inContraption) {
            poseStack.pushPose();
            poseStack.scale(1.2F, 1, 1);
            super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);
            poseStack.popPose();
        }

        public static class Backward extends PK209PRenderer.Backward {
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

        public static class NoMotor extends PK209PRenderer.NoMotor {
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
}
