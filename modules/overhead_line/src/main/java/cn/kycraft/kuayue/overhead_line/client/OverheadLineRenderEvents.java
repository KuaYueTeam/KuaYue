package cn.kycraft.kuayue.overhead_line.client;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineEndpoint;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineSync;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = KuaYue.MODID, value = Dist.CLIENT)
public final class OverheadLineRenderEvents {
    private static final int SEGMENTS = 24;
    private static final double MIN_HORIZONTAL_SPAN = 1.0e-4;

    private OverheadLineRenderEvents() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var connections = OverheadLineSync.clientConnections();
        if (level == null || connections.isEmpty()) return;

        var buffers = minecraft.renderBuffers().bufferSource();
        var type = RenderType.lines();
        var vertices = buffers.getBuffer(type);
        var pose = event.getPoseStack().last();
        var camera = event.getCamera().getPosition();
        for (var connection : connections) {
            Vec3 from = resolve(level, connection.from());
            Vec3 to = resolve(level, connection.to());
            if (from == null || to == null) continue;

            int color = switch (connection.kind()) {
                case OVERHEAD_LINE_WIRE -> 0xFFCD41;
                case ELECTRONIC_WIRE -> 0x50B4FF;
            };
            drawCatenary(vertices, pose, from.subtract(camera), to.subtract(camera), color);
        }
        buffers.endBatch(type);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        OverheadLineSync.clearClient();
    }

    private static Vec3 resolve(ClientLevel level, OverheadLineEndpoint endpoint) {
        if (!level.dimension().equals(endpoint.dimension())) return null;
        var pos = endpoint.blockPos();
        var chunk = new ChunkPos(pos);
        if (!level.getChunkSource().hasChunk(chunk.x, chunk.z)) return null;

        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NormalOverheadSupportBlock support)) return null;
        return support.resolveAnchor(pos, state, endpoint.pointIndex()).orElse(null);
    }

    private static void drawCatenary(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, int rgb) {
        double horizontal = Math.hypot(b.x - a.x, b.z - a.z);
        if (horizontal < MIN_HORIZONTAL_SPAN) return;

        double c = 2.0 * horizontal;
        double u = (b.y - a.y) / (2.0 * c * Math.sinh(horizontal / (2.0 * c)));
        double asinh = Math.copySign(Math.log(Math.abs(u) + Math.hypot(u, 1.0)), u);
        double x0 = horizontal / 2.0 - c * asinh;
        Vec3 previous = a;
        for (int i = 1; i <= SEGMENTS; i++) {
            double t = (double) i / SEGMENTS;
            double x = horizontal * t;
            double y = a.y + 2.0 * c * Math.sinh(x / (2.0 * c))
                    * Math.sinh((x - 2.0 * x0) / (2.0 * c));
            Vec3 next = i == SEGMENTS ? b : new Vec3(
                    a.x + (b.x - a.x) * t, y, a.z + (b.z - a.z) * t);
            Vec3 direction = next.subtract(previous).normalize();
            vertex(out, pose, previous, direction, rgb);
            vertex(out, pose, next, direction, rgb);
            previous = next;
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 pos, Vec3 direction, int rgb) {
        out.addVertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255)
                .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
    }
}
