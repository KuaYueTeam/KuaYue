package cn.kycraft.kuayue.packs.bootstrap;

import cn.kycraft.kuayue.packs.CorePackDescriptor;
import cn.kycraft.kuayue.packs.DiscoveredPack;
import cn.kycraft.kuayue.packs.PackConfigStore;
import cn.kycraft.kuayue.packs.PackDiscovery;
import cn.kycraft.kuayue.packs.PackDiscoveryResult;
import cn.kycraft.kuayue.packs.PackFailurePolicy;
import cn.kycraft.kuayue.packs.PackLimits;
import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.PackRuntimeEnvironment;
import cn.kycraft.kuayue.packs.ResolvedPack;
import cn.kycraft.kuayue.packs.VersionComparator;
import cn.kycraft.kuayue.packs.history.PackHistoryCompatibility;
import cn.kycraft.kuayue.packs.history.PackHistoryGuard;
import cn.kycraft.kuayue.packs.runtime.LegacyPackCacheCleaner;
import cn.kycraft.kuayue.packs.runtime.PackResourceSnapshotter;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import lib.kasuga.core.resource.ResourceSystem;
import lib.kasuga.core.resource.lifecycle.LogicalSide;
import lib.kasuga.core.resource.lifecycle.ResourceEvent;
import lib.kasuga.core.resource.routing.ResourceRouting;
import lib.kasuga.core.resource.source.InputPolicy;
import lib.kasuga.core.resource.source.ResourceSource;
import lib.kasuga.core.resource.source.ResourceSourceDescriptor;
import lib.kasuga.core.resource.source.ResourceSources;
import net.minecraft.SharedConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * KuaYue host adapter for pack selection, registration and resource discovery.
 */
public final class KuaYuePackBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger("KuaYue/Packs");
    private static final String RUNTIME_OWNER_ID = "kuayue-runtime";
    private static final AtomicReference<StartupPackSnapshot> SNAPSHOT = new AtomicReference<>();
    private static final AtomicBoolean RESOURCES_INSTALLED = new AtomicBoolean();
    private static final List<AutoCloseable> RESOURCE_REGISTRATIONS =
            new CopyOnWriteArrayList<>();

    private KuaYuePackBootstrap() {
    }

    public static synchronized StartupPackSnapshot initialize(
            Path gameDirectory,
            ModContainer kuaYueContainer,
            IEventBus modEventBus
    ) {
        StartupPackSnapshot current = SNAPSHOT.get();
        if (current != null) {
            return current;
        }
        PackRuntimeEnvironment environment = runtimeEnvironment();
        Path runtimeSourceRoot = kuaYueContainer.getModInfo().getOwningFile()
                .getFile().getSecureJar().getRootPath();
        PackLimits limits = PackLimits.defaults();
        InputPolicy inputPolicy = inputPolicy(limits);
        CorePackDescriptor corepack = CorePackDescriptor.forVersion(
                kuaYueContainer.getModInfo().getVersion().toString());
        PackDiscoveryResult discovery = PackDiscovery.discover(
                gameDirectory,
                environment,
                corepack,
                limits,
                (left, right) -> new ComparableVersion(left).compareTo(new ComparableVersion(right))
        );

        Path reportPath = gameDirectory.resolve("kuayue/reports/packs/startup.json");
        try {
            discovery.report().write(reportPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write KuaYue pack startup report", exception);
        }

        if (discovery.report().diagnostics().stream()
                .anyMatch(PackFailurePolicy::isFatalDiscoveryDiagnostic)) {
            throw new IllegalStateException(
                    "KuaYue pack configuration is invalid; see " + reportPath);
        }
        Path configPath = gameDirectory.resolve("kuayue/config.json");
        try {
            PackConfigStore.write(configPath, discovery.configDocument());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to update KuaYue pack debug config", exception);
        }

        Path legacyCache = gameDirectory.resolve("kuayue/cache/packs");
        try {
            LegacyPackCacheCleaner.clear(legacyCache);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to clear legacy KuaYue pack cache", exception);
        }

        StartupPackSnapshot.ManagedRuntimeRoot runtimeRoot;
        try {
            RuntimeCapture runtime = captureRuntime(runtimeSourceRoot, inputPolicy);
            runtimeRoot = runtime.root();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to capture KuaYue runtime resources", exception);
        }

        Map<String, DiscoveredPack> discoveredById = new LinkedHashMap<>();
        discovery.catalog().packs().forEach(pack -> discoveredById.put(pack.packId(), pack));
        List<StartupPackSnapshot.ManagedPackRoot> roots = new ArrayList<>();
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        List<PackHistoryCompatibility.CurrentPack> historyPacks = new ArrayList<>();
        int nextRank = 1;
        try {
            for (ResolvedPack resolved : discovery.selection().effectivePacks()) {
                if (PackManifest.CORE_PACK_ID.equals(resolved.id())) {
                    Path corepackRoot = findCorepackRoot(kuaYueContainer);
                    DiscoveredPack corePack = PackDiscovery.scanCoreRoot(corepackRoot, limits);
                    PackResourceSnapshotter.CapturedPack captured =
                            PackResourceSnapshotter.capture(
                                    corepackRoot,
                                    corePack,
                                    limits,
                                    nextRank
                            );
                    roots.add(new StartupPackSnapshot.ManagedPackRoot(
                            resolved.id(),
                            resolved.version(),
                            true,
                            corepackRoot,
                            captured,
                            Set.copyOf(corePack.manifest().namespaces())
                    ));
                    nextRank = captured.nextRank();
                    historyPacks.add(new PackHistoryCompatibility.CurrentPack(
                            resolved.id(),
                            resolved.version(),
                            PackManifest.FORMAT_VERSION,
                            null
                    ));
                    dependencies.put(resolved.id(), Set.of());
                    continue;
                }

                DiscoveredPack pack = discoveredById.get(resolved.id());
                if (pack == null) {
                    throw new IllegalStateException(
                            "Effective pack is missing from catalog: " + resolved.id());
                }
                Path installation = PackResourceSnapshotter.installationPath(gameDirectory, pack);
                PackResourceSnapshotter.CapturedPack captured =
                        PackResourceSnapshotter.capture(
                                installation,
                                pack,
                                limits,
                                nextRank
                        );
                roots.add(new StartupPackSnapshot.ManagedPackRoot(
                        resolved.id(),
                        resolved.version(),
                        false,
                        installation,
                        captured,
                        Set.copyOf(pack.manifest().namespaces())
                ));
                nextRank = captured.nextRank();
                historyPacks.add(PackHistoryCompatibility.CurrentPack.from(pack.manifest()));
                dependencies.put(
                        resolved.id(),
                        Set.copyOf(pack.manifest().dependencies().packs().keySet())
                );
            }
        } catch (IOException | RuntimeException failure) {
            closeQuietly(runtimeRoot);
            closeQuietly(roots);
            throw new IllegalStateException("Unable to capture KuaYue pack resources", failure);
        }
        historyPacks.sort(Comparator.comparing(PackHistoryCompatibility.CurrentPack::id));

        StartupPackSnapshot snapshot = new StartupPackSnapshot(
                gameDirectory,
                runtimeRoot,
                discovery.configDocument().config(),
                roots,
                dependencies,
                historyPacks,
                discovery.report()
        );
        PackRuntimeState.install(snapshot.config());
        SNAPSHOT.set(snapshot);
        NeoForge.EVENT_BUS.addListener(
                ServerAboutToStartEvent.class,
                PackHistoryGuard::onServerAboutToStart
        );
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, PackHistoryGuard::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(
                ServerStoppingEvent.class,
                PackHistoryGuard::onServerStopping
        );
        LOGGER.info(
                "Frozen {} effective KuaYue packs; corepack={}",
                roots.size(),
                PackRuntimeState.corepackEnabled()
        );
        return snapshot;
    }

    /**
     * Installs resource projections only after the registration session has finalized the active
     * pack set. Registration and Minecraft resource consumers share the captured root sources.
     */
    public static synchronized void installResources(ResourceSystem resourceSystem) {
        if (!RESOURCES_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        StartupPackSnapshot snapshot = SNAPSHOT.get();
        if (snapshot == null) {
            RESOURCES_INSTALLED.set(false);
            throw new IllegalStateException("KuaYue pack snapshot is not initialized");
        }
        if (!PackRuntimeState.isRegistrationSelectionFinalized()) {
            RESOURCES_INSTALLED.set(false);
            throw new IllegalStateException(
                    "KuaYue resources cannot be installed before strict registration selection");
        }
        Set<String> activePacks = PackRuntimeState.registeredPackIds();
        List<ResourceSource> sources = new ArrayList<>();
        snapshot.resourceRoots().stream()
                .filter(root -> activePacks.contains(root.id()))
                .forEach(root -> root.capture().roots().stream()
                        .map(PackResourceSnapshotter.CapturedRoot::source)
                        .forEach(sources::add));
        try {
            for (ResourceSource source : sources) {
                ResourceSourceDescriptor descriptor = source.descriptor();
                ResourceRouting routing = new ResourceRouting(List.of(
                        new ResourceRouting.Binding(
                                "binding-" + descriptor.rootId(),
                                descriptor.sourceId(),
                                "",
                                List.of(new ResourceRouting.Output(
                                        "minecraft",
                                        ResourceRouting.MINECRAFT_ENTRY_POINT,
                                        ResourceEvent.RESOURCE_RELOAD,
                                        LogicalSide.BOTH,
                                        ""
                                ))
                        )
                ));
                String registrationId = descriptor.sourceId() + "-" + descriptor.rootId();
                RESOURCE_REGISTRATIONS.add(
                        resourceSystem.getApprovedPacks().register(
                                registrationId,
                                List.of(source),
                                routing
                        )
                );
            }
            resourceSystem.registerManagedCloseable(KuaYuePackBootstrap::closePackResources);
        } catch (RuntimeException failure) {
            closeQuietly(RESOURCE_REGISTRATIONS);
            RESOURCE_REGISTRATIONS.clear();
            RESOURCES_INSTALLED.set(false);
            throw failure;
        }
    }

    public static Optional<StartupPackSnapshot> snapshot() {
        return Optional.ofNullable(SNAPSHOT.get());
    }

    private static RuntimeCapture captureRuntime(Path root, InputPolicy policy)
            throws IOException {
        ResourceSourceDescriptor descriptor = new ResourceSourceDescriptor(
                RUNTIME_OWNER_ID,
                RUNTIME_OWNER_ID,
                "main",
                0,
                ResourceSourceDescriptor.Origin.EXTERNAL
        );
        ResourceSource fixed = ResourceSources.fixed(
                descriptor,
                ResourceSources.mod(descriptor, root, policy),
                policy
        );
        return new RuntimeCapture(new StartupPackSnapshot.ManagedRuntimeRoot(
                descriptor,
                Set.of("kuayue"),
                fixed
        ));
    }

    private static InputPolicy inputPolicy(PackLimits limits) {
        return new InputPolicy(
                limits.jsonFileBytes(),
                limits.regularFileBytes(),
                limits.packBytes(),
                limits.maxFiles(),
                64,
                1L << 20
        );
    }

    private static Path findCorepackRoot(ModContainer kuaYueContainer) {
        Path root = ModList.get().getModFiles().stream()
                .filter(file -> file.getMods().stream()
                        .anyMatch(mod -> mod.getModId().equals("kuayue")))
                .map(file -> file.getFile().findResource("kuayue-packs", "kuayue-core"))
                .filter(java.nio.file.Files::isDirectory)
                .findFirst()
                .orElse(null);
        if (root == null) {
            root = kuaYueContainer.getModInfo().getOwningFile()
                    .getFile().findResource("kuayue-packs", "kuayue-core");
        }
        if (!java.nio.file.Files.isDirectory(root)) {
            throw new IllegalStateException(
                    "Embedded KuaYue corepack was not found at kuayue-packs/kuayue-core");
        }
        return root;
    }

    private static void closePackResources() {
        closeQuietly(RESOURCE_REGISTRATIONS);
        RESOURCE_REGISTRATIONS.clear();
        StartupPackSnapshot snapshot = SNAPSHOT.getAndSet(null);
        if (snapshot != null) {
            closeQuietly(snapshot.runtimeRoot());
            closeQuietly(snapshot.resourceRoots());
        }
        RESOURCES_INSTALLED.set(false);
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception exception) {
            LOGGER.warn("KuaYue resource cleanup failed", exception);
        }
    }

    private static void closeQuietly(Iterable<? extends AutoCloseable> closeables) {
        List<AutoCloseable> reversed = new ArrayList<>();
        closeables.forEach(reversed::add);
        for (int index = reversed.size() - 1; index >= 0; index--) {
            closeQuietly(reversed.get(index));
        }
    }

    private static PackRuntimeEnvironment runtimeEnvironment() {
        Map<String, String> versions = new LinkedHashMap<>();
        versions.put("minecraft", SharedConstants.getCurrentVersion().getName());
        for (String modId : List.of("neoforge", "kuayue", "kasuga_lib", "create")) {
            versions.put(modId, ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElseThrow(() -> new IllegalStateException(
                            "Required runtime mod is not loaded: " + modId)));
        }
        return new PackRuntimeEnvironment(versions);
    }

    private record RuntimeCapture(StartupPackSnapshot.ManagedRuntimeRoot root) {
    }
}
