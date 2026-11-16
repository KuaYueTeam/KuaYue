package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.StartupPackSnapshot;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads world pack history before chunks are prepared and attaches it to Overworld storage.
 */
public final class PackHistoryGuard {
    private static final Map<MinecraftServer, PendingHistory> PENDING = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, ActiveHistory> ACTIVE = new ConcurrentHashMap<>();
    private static final Set<Path> CONFIRMED_WORLDS =
            java.util.Collections.synchronizedSet(new LinkedHashSet<>());
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("KuaYue/Packs/History");

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot()
                .orElseThrow(() -> new IllegalStateException("KuaYue pack snapshot is not initialized"));
        MinecraftServer server = event.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path dataDirectory = worldRoot.resolve("data");
        Path historyPath = dataDirectory.resolve(PackHistorySavedData.DATA_NAME + ".dat");
        boolean exists;
        try {
            BasicFileAttributes dataAttributes = Files.readAttributes(
                    dataDirectory,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
            if (dataAttributes.isSymbolicLink() || !dataAttributes.isDirectory()) {
                throw new IllegalStateException(
                        "KuaYue world data path is not a real directory: " + dataDirectory);
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    historyPath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
            exists = true;
            if (!attributes.isRegularFile()) {
                throw new IllegalStateException(
                        "KuaYue pack history path is not a regular file: " + historyPath);
            }
        } catch (NoSuchFileException missing) {
            exists = false;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Unable to inspect KuaYue pack history: " + historyPath,
                    exception
            );
        }

        PackHistorySavedData history;
        if (exists) {
            try {
                history = PackHistoryPreflight.readSavedData(historyPath);
            } catch (java.io.IOException | RuntimeException failure) {
                throw new IllegalStateException(
                        "KuaYue pack history is corrupt or unreadable and was not overwritten: "
                                + historyPath,
                        failure
                );
            }
        } else {
            history = new PackHistorySavedData();
        }

        List<PackHistoryCompatibility.CurrentPack> activePacks = currentPacks(snapshot);
        PackHistoryCompatibility.Result compatibility =
                PackHistoryCompatibility.compare(history, activePacks);
        if (compatibility.outcome() == PackHistoryCompatibility.Outcome.INCOMPATIBLE) {
            if (server.isDedicatedServer()) {
                LOGGER.warn(
                        "KuaYue pack set differs from this world's previous record; "
                                + "continuing because this is a headless server: {}",
                        compatibility.diagnostics()
                );
            } else if (!consumeConfirmation(worldRoot)) {
                throw new IllegalStateException(
                        "KuaYue pack set is incompatible with this world: "
                                + compatibility.diagnostics());
            }
        }
        PENDING.put(server, new PendingHistory(history, activePacks));
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
        level.getDataStorage().set(PackHistorySavedData.DATA_NAME, pending.history());
        ACTIVE.put(server, new ActiveHistory(pending.history(), pending.currentPacks()));
    }

    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().isClientSide()
                || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }
        MinecraftServer server = level.getServer();
        ActiveHistory active = ACTIVE.get(server);
        if (active == null) return;
        if (active.history().initialize(active.currentPacks())) {
            // NeoForge posts LevelEvent.Save after vanilla has already flushed level data.
            // Flush once more while the save worker is still waiting for completion.
            level.getChunkSource().getDataStorage().save();
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.remove(event.getServer());
        ACTIVE.remove(event.getServer());
    }

    public static void clearForTests() {
        PENDING.clear();
        ACTIVE.clear();
        CONFIRMED_WORLDS.clear();
    }

    /** Records the one-shot confirmation for a world root selected in the client UI. */
    public static void confirmWorld(Path worldRoot) {
        CONFIRMED_WORLDS.add(worldRoot.toAbsolutePath().normalize());
    }

    public static void cancelConfirmation(Path worldRoot) {
        CONFIRMED_WORLDS.remove(worldRoot.toAbsolutePath().normalize());
    }

    private static boolean consumeConfirmation(Path worldRoot) {
        return CONFIRMED_WORLDS.remove(worldRoot.toAbsolutePath().normalize());
    }

    private static List<PackHistoryCompatibility.CurrentPack> currentPacks(
            StartupPackSnapshot snapshot
    ) {
        if (!PackRuntimeState.isRegistrationSelectionFinalized()) {
            throw new IllegalStateException("Pack registration selection is not finalized");
        }
        return snapshot.historyPacks().stream()
                .filter(pack -> PackRuntimeState.registeredPackIds().contains(pack.id()))
                .toList();
    }

    private record PendingHistory(
            PackHistorySavedData history,
            List<PackHistoryCompatibility.CurrentPack> currentPacks
    ) {
    }

    private record ActiveHistory(
            PackHistorySavedData history,
            List<PackHistoryCompatibility.CurrentPack> currentPacks
    ) {
    }

    private PackHistoryGuard() {
    }
}
