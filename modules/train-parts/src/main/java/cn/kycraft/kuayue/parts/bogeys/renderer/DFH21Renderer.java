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

public class DFH21Renderer implements BogeyRenderer {

    static final double dp = 2.4;
    static final double ad = 1.2;

    public static final PartialModel DFH21_MAIN = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/dfh21/dfh21_main"));
    public static final PartialModel DFH21_WHEEL = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/dfh21/dfh21_wheel"));
    public static final PartialModel DFH21_TRANSMISSION_ROD = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/dfh21/dfh21_transmission_rod"));
    public static final PartialModel DFH21_MAIN_STANDARD = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/dfh21/dfh21_main_standard"));
    public static final PartialModel DFH21_WHEEL_STANDARD = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/dfh21/dfh21_wheel_standard"));
    public static final PartialModel DFH21_TRANSMISSION_ROD_STANDARD = PartialModel.of(
            ResourceLocation.parse("kuayue:block/bogey/dfh21/dfh21_transmission_rod_standard"));

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                       boolean inContraption) {
        Direction direction = bogeyData.contains("assembly_direction")
                ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                : Direction.NORTH;

        boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;
        boolean flipY = !isBackward || !inContraption;
        boolean negateAngle = isBackward && !inContraption;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

        if (flipY) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        CachedBuffers.partial(DFH21_MAIN, Blocks.AIR.defaultBlockState())
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        float angle = negateAngle ? -wheelAngle : wheelAngle;
        SuperByteBuffer wheel = CachedBuffers.partial(DFH21_WHEEL, Blocks.AIR.defaultBlockState());
        for (int side : new int[]{-1, 0}) {
            wheel.translate(0, 0.9, side * dp + ad)
                    .rotateXDegrees(angle)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);
        }

        CachedBuffers.partial(DFH21_TRANSMISSION_ROD, Blocks.AIR.defaultBlockState())
                .translate(0, 0.85, 0)
                .rotateZDegrees(angle * 3.256f)
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer);

        if (flipY) {
            poseStack.popPose();
        }
    }

    public static class Backward extends DFH21Renderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean isBackward = direction == Direction.NORTH || direction == Direction.WEST;
            boolean flipY = !isBackward || !inContraption;
            boolean negateAngle = isBackward && !inContraption;

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            if (flipY) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            CachedBuffers.partial(DFH21_MAIN, Blocks.AIR.defaultBlockState())
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            float angle = negateAngle ? -wheelAngle : wheelAngle;
            SuperByteBuffer wheel = CachedBuffers.partial(DFH21_WHEEL, Blocks.AIR.defaultBlockState());
            for (int side : new int[]{-1, 0}) {
                wheel.translate(0, 0.9, side * dp + ad)
                        .rotateXDegrees(angle)
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);
            }

            CachedBuffers.partial(DFH21_TRANSMISSION_ROD, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.85, 0)
                    .rotateZDegrees(angle * 3.256f)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            if (flipY) {
                poseStack.popPose();
            }
        }
    }

    public static class Standard extends DFH21Renderer {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            Direction direction = bogeyData.contains("assembly_direction")
                    ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                    : Direction.NORTH;

            boolean isBackward = direction == Direction.SOUTH || direction == Direction.EAST;
            boolean flipY = !isBackward || !inContraption;
            boolean negateAngle = isBackward && !inContraption;

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

            if (flipY) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }

            CachedBuffers.partial(DFH21_MAIN_STANDARD, Blocks.AIR.defaultBlockState())
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            float angle = negateAngle ? -wheelAngle : wheelAngle;
            SuperByteBuffer wheel = CachedBuffers.partial(DFH21_WHEEL_STANDARD, Blocks.AIR.defaultBlockState());
            for (int side : new int[]{-1, 0}) {
                wheel.translate(0, 0.875, side * dp + ad)
                        .rotateXDegrees(angle)
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);
            }

            CachedBuffers.partial(DFH21_TRANSMISSION_ROD_STANDARD, Blocks.AIR.defaultBlockState())
                    .translate(0, 0.85, 0)
                    .rotateZDegrees(angle * 3.256f)
                    .light(light)
                    .overlay(overlay)
                    .renderInto(poseStack, buffer);

            if (flipY) {
                poseStack.popPose();
            }
        }

        public static class Backward extends Standard {
            @Override
            public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                               PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                               boolean inContraption) {
                Direction direction = bogeyData.contains("assembly_direction")
                        ? NBTHelper.readEnum(bogeyData, "assembly_direction", Direction.class)
                        : Direction.NORTH;

                boolean isBackward = direction == Direction.NORTH || direction == Direction.WEST;
                boolean flipY = !isBackward || !inContraption;
                boolean negateAngle = isBackward && !inContraption;

                VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());

                if (flipY) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }

                CachedBuffers.partial(DFH21_MAIN_STANDARD, Blocks.AIR.defaultBlockState())
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);

                float angle = negateAngle ? -wheelAngle : wheelAngle;
                SuperByteBuffer wheel = CachedBuffers.partial(DFH21_WHEEL_STANDARD, Blocks.AIR.defaultBlockState());
                for (int side : new int[]{-1, 0}) {
                    wheel.translate(0, 0.875, side * dp + ad)
                            .rotateXDegrees(angle)
                            .light(light)
                            .overlay(overlay)
                            .renderInto(poseStack, buffer);
                }

                CachedBuffers.partial(DFH21_TRANSMISSION_ROD_STANDARD, Blocks.AIR.defaultBlockState())
                        .translate(0, 0.85, 0)
                        .rotateZDegrees(angle * 3.256f)
                        .light(light)
                        .overlay(overlay)
                        .renderInto(poseStack, buffer);

                if (flipY) {
                    poseStack.popPose();
                }
            }
        }
    }

    public static class Andesite extends Standard {
        @Override
        public void render(CompoundTag bogeyData, float wheelAngle, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay,
                           boolean inContraption) {
            poseStack.pushPose();
            poseStack.scale(1.2F, 1, 1);
            super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);
            poseStack.popPose();
        }

        public static class Backward extends Standard.Backward {
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
}
