package cn.kycraft.kuayue.overhead_line.wire;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.block.line.OverheadLineConnection;
import cn.kycraft.kuayue.overhead_line.save.OverheadLineSavedData.AddResult;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineBatch.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static cn.kycraft.kuayue.overhead_line.save.OverheadLineSavedData.STORAGE;

@EventBusSubscriber(modid = KuaYue.MODID)
public class OverheadLineSync {
    private static final Set<OverheadLineConnection> CONNECTIONS = new HashSet<>();

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        STORAGE.onLoad(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STORAGE.onUnload();
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .playToClient(OverheadLineConnection.TYPE, OverheadLineConnection.STREAM_CODEC,
                        (link, context) -> upsertClient(link))
                .playToClient(OverheadLineBatch.TYPE, OverheadLineBatch.STREAM_CODEC,
                        (batch, context) -> applyBatch(batch));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSnapshot(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSnapshot(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSnapshot(player);
    }

    private static void sendSnapshot(ServerPlayer player) {
        var dimension = player.serverLevel().dimension();
        var links = STORAGE.get(dimension).connections(dimension).iterator();
        PacketDistributor.sendToPlayer(player, new OverheadLineBatch(Action.RESET, List.of()));
        sendBatches(Action.APPEND, links, packet -> PacketDistributor.sendToPlayer(player, packet));
    }

    public static AddResult add(ServerLevel level, OverheadLineConnection link, int fromMax, int toMax) {
        var result = STORAGE.get(level.dimension()).add(link, fromMax, toMax);
        if (result == AddResult.ADDED) PacketDistributor.sendToPlayersInDimension(level, link);
        return result;
    }

    public static void removeAt(ServerLevel level, BlockPos pos) {
        var removed = STORAGE.get(level.dimension()).removeAt(level.dimension(), pos);
        sendBatches(Action.REMOVE, removed.iterator(), packet -> PacketDistributor.sendToPlayersInDimension(level, packet));
    }

    private static void sendBatches(Action action, Iterator<OverheadLineConnection> links,
                                    Consumer<OverheadLineBatch> send) {
        while (links.hasNext()) {
            var batch = new ArrayList<OverheadLineConnection>(OverheadLineBatch.MAX_LINKS);
            while (links.hasNext() && batch.size() < OverheadLineBatch.MAX_LINKS) batch.add(links.next());
            send.accept(new OverheadLineBatch(action, batch));
        }
    }

    static void applyBatch(OverheadLineBatch batch) {
        switch (batch.action()) {
            case RESET -> clearClient();
            case APPEND -> CONNECTIONS.addAll(batch.links());
            case REMOVE -> CONNECTIONS.removeAll(batch.links());
        }
    }

    public static void upsertClient(OverheadLineConnection link) {
        CONNECTIONS.add(link);
    }

    public static void updateClient(OverheadLineConnection previous, OverheadLineConnection replacement) {
        if (!previous.equals(replacement)) CONNECTIONS.remove(previous);
        upsertClient(replacement);
    }

    public static void removeClient(OverheadLineConnection link) {
        CONNECTIONS.remove(link);
    }

    public static Set<OverheadLineConnection> clientConnections() {
        return Collections.unmodifiableSet(CONNECTIONS);
    }

    public static void clearClient() {
        CONNECTIONS.clear();
    }
}
