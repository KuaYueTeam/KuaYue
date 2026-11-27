package cn.kycraft.kuayue.overhead_line.client;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.block.line.OverheadLineConnection;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.render.CachedCurveRenderer;
import cn.kycraft.kuayue.overhead_line.render.OverheadLineCurveGenerator;
import cn.kycraft.kuayue.overhead_line.render.RenderCurve;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineEndpoint;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineSync;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineWireKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = KuaYue.MODID, value = Dist.CLIENT)
public final class OverheadLineRenderEvents {
    private static final Map<OverheadLineConnection, CachedWire> CURVES = new HashMap<>();
    private static ClientLevel renderedLevel;

    private OverheadLineRenderEvents() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (renderedLevel != level) clearGeometry();
        renderedLevel = level;
        if (level == null) return;

        var connections = OverheadLineSync.clientConnections();
        CURVES.keySet().retainAll(connections);
        var buffers = minecraft.renderBuffers().bufferSource();
        var type = CachedCurveRenderer.renderType();
        var pose = event.getPoseStack().last();
        var camera = event.getCamera().getPosition();
        boolean drew = false;
        try {
            for (var connection : connections) {
                if (!level.dimension().equals(connection.from().dimension())) continue;
                var curve = refresh(level, connection);
                if (curve == null || !event.getFrustum().isVisible(curve.bounds())) continue;
                var out = buffers.getBuffer(type);
                drew = true;
                CachedCurveRenderer.render(curve, pose, out, camera, level);
            }
        } finally {
            if (drew) buffers.endBatch(type);
        }
    }

    private static RenderCurve refresh(ClientLevel level, OverheadLineConnection connection) {
        var fromState = loadedState(level, connection.from());
        var toState = loadedState(level, connection.to());
        if (fromState == null || toState == null) {
            CURVES.remove(connection);
            return null;
        }
        var previous = CURVES.get(connection);
        if (previous != null && previous.fromState == fromState && previous.toState == toState) {
            return previous.curve;
        }
        var from = resolve(fromState, connection.from(), connection.kind());
        var to = resolve(toState, connection.to(), connection.kind());
        if (from == null || to == null) {
            CURVES.remove(connection);
            return null;
        }
        RenderCurve curve = previous != null && from.equals(previous.from) && to.equals(previous.to)
                ? previous.curve : OverheadLineCurveGenerator.generate(from, to, connection.kind());
        CURVES.put(connection, new CachedWire(fromState, toState, from, to, curve));
        return curve;
    }

    private static BlockState loadedState(ClientLevel level, OverheadLineEndpoint endpoint) {
        var pos = endpoint.blockPos();
        var chunk = new ChunkPos(pos);
        var loaded = level.getChunkSource().getChunk(chunk.x, chunk.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
        return loaded == null ? null : loaded.getBlockState(pos);
    }

    private static Vec3 resolve(BlockState state, OverheadLineEndpoint endpoint, OverheadLineWireKind kind) {
        if (!(state.getBlock() instanceof NormalOverheadSupportBlock support)) return null;
        return support.resolveAnchor(endpoint.blockPos(), state, endpoint.pointIndex(), kind).orElse(null);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == renderedLevel) clearGeometry();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        OverheadLineSync.clearClient();
        clearGeometry();
    }

    private static void clearGeometry() {
        CURVES.clear();
        renderedLevel = null;
    }

    private record CachedWire(BlockState fromState, BlockState toState, Vec3 from, Vec3 to, RenderCurve curve) {}
}
