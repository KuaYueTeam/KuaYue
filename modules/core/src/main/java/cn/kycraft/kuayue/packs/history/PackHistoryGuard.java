package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.StartupPackSnapshot;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads world pack history before chunks are prepared and attaches it to Overworld storage.
 */
public final class PackHistoryGuard {
    private static final Map<MinecraftServer, PendingHistory> PENDING = new ConcurrentHashMap<>();

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot()
                .orElseThrow(() -> new IllegalStateException("KuaYue pack snapshot is not initialized"));
        MinecraftServer server = event.getServer();
        Path dataDirectory = server.getWorldPath(LevelResource.ROOT).resolve("data");
        Path historyPath = dataDirectory.resolve(PackHistorySavedData.DATA_NAME + ".dat");
        boolean exists = Files.exists(historyPath);
        if (exists && !Files.isRegularFile(historyPath)) {
            throw new IllegalStateException("KuaYue pack history path is not a file: " + historyPath);
        }

        PackHistorySavedData history;
        if (exists) {
            DimensionDataStorage storage = new DimensionDataStorage(
                    dataDirectory.toFile(),
                    server.getFixerUpper(),
                    server.registryAccess()
            );
            history = storage.get(PackHistorySavedData.FACTORY, PackHistorySavedData.DATA_NAME);
            if (history == null) {
                throw new IllegalStateException(
                        "KuaYue pack history is corrupt and was not overwritten: " + historyPath);
            }
        } else {
            history = new PackHistorySavedData();
        }

        PackHistoryCompatibility.Result compatibility =
                PackHistoryCompatibility.compare(
                        history,
                        snapshot.historyPacks().stream()
                                .filter(pack -> PackRuntimeState.registeredPackIds()
                                        .contains(pack.id()))
                                .toList()
                );
        if (compatibility.outcome() == PackHistoryCompatibility.Outcome.INCOMPATIBLE) {
            throw new IllegalStateException(
                    "KuaYue pack set is incompatible with this world: "
                            + compatibility.diagnostics());
        }
        PENDING.put(server, new PendingHistory(history, compatibility.outcome()));
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()
                || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }
        MinecraftServer server = level.getServer();
        PendingHistory pending = PENDING.remove(server);
        if (pending == null) {
            return;
        }
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot()
                .orElseThrow(() -> new IllegalStateException("KuaYue pack snapshot is not initialized"));
        pending.history().initialize(snapshot.historyPacks().stream()
                .filter(pack -> PackRuntimeState.registeredPackIds().contains(pack.id()))
                .toList());
        level.getDataStorage().set(PackHistorySavedData.DATA_NAME, pending.history());
        if (pending.previousOutcome() == PackHistoryCompatibility.Outcome.MISSING) {
            pending.history().setDirty();
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.remove(event.getServer());
    }

    public static void clearForTests() {
        PENDING.clear();
    }

    private record PendingHistory(
            PackHistorySavedData history,
            PackHistoryCompatibility.Outcome previousOutcome
    ) {
    }

    private PackHistoryGuard() {
    }
}
