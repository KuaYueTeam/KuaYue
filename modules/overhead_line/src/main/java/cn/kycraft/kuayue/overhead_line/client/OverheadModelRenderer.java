package cn.kycraft.kuayue.overhead_line.client;

import cn.kycraft.kuayue.overhead_line.block.support.OverheadLineAnchorTransform;
import cn.kycraft.kuayue.overhead_line.block.support.OverheadModelBlockEntity;
import cn.kycraft.kuayue.overhead_line.block.support.variants.ModelledOverheadSupportBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import lib.kasuga.rendering.models.mc.api.McModelHandle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

public final class OverheadModelRenderer implements BlockEntityRenderer<OverheadModelBlockEntity> {
    private static final Map<OverheadModelBlockEntity, Instance> INSTANCES = new IdentityHashMap<>();

    public OverheadModelRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(OverheadModelBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = entity.getBlockState();
        if (entity.isRemoved() || !(state.getBlock() instanceof ModelledOverheadSupportBlock block)) return;
        Instance instance = INSTANCES.computeIfAbsent(entity, key -> {
            var id = ResourceLocation.fromNamespaceAndPath("kuayue", "overhead/" + UUID.randomUUID());
            var handle = McModelHandle.of(block.model(), null, id, null).scheduleVanillaRenderer();
            entity.onClientModelRelease(() -> {
                INSTANCES.remove(entity);
                handle.destroy();
            });
            return new Instance(handle);
        });
        int yaw = block.modelYaw(state);
        if (yaw != instance.yaw) {
            Vec3 center = OverheadLineAnchorTransform.PIVOT;
            float yawRadians = (float) Math.toRadians(-yaw);
            Vec3 rotatedCenter = center.yRot(yawRadians);
            Vec3 modelOffset = block.modelOffset();
            Vec3 localOffset = new Vec3(modelOffset.x, modelOffset.z, -modelOffset.y);
            Vec3 rotatedOffset = localOffset.yRot(yawRadians);
            instance.handle.setPos(Vec3.atLowerCornerOf(entity.getBlockPos())
                    .add(center.subtract(rotatedCenter)).add(rotatedOffset));
            instance.handle.setRotationDegrees(0, -yaw, 0);
            instance.yaw = yaw;
        }
        if (instance.handle.mount()) instance.handle.markRenderedThisFrame();
    }

    @Override
    public boolean shouldRenderOffScreen(OverheadModelBlockEntity entity) {

        return true;
    }

    private static final class Instance {
        final McModelHandle handle;
        int yaw = -1;

        Instance(McModelHandle handle) {
            this.handle = handle;
        }
    }
}
