package cn.kycraft.kuayue.packs.bootstrap;

import cn.kycraft.kuayue.packs.DiscoveredPack;
import cn.kycraft.kuayue.packs.PackCatalog;
import cn.kycraft.kuayue.packs.PackConfigStore;
import cn.kycraft.kuayue.packs.PackDiagnostic;
import cn.kycraft.kuayue.packs.PackDiagnosticCodes;
import cn.kycraft.kuayue.packs.PackDisplayText;
import cn.kycraft.kuayue.packs.PackDiscovery;
import cn.kycraft.kuayue.packs.PackDiscoveryResult;
import cn.kycraft.kuayue.packs.PackFailurePolicy;
import cn.kycraft.kuayue.packs.PackIcon;
import cn.kycraft.kuayue.packs.PackLimits;
import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.PackRuntimeEnvironment;
import cn.kycraft.kuayue.packs.PackSeverity;
import cn.kycraft.kuayue.packs.PackStage;
import cn.kycraft.kuayue.packs.PackStartupReport;
import cn.kycraft.kuayue.packs.ResolvedPack;
import cn.kycraft.kuayue.packs.VersionComparator;
import cn.kycraft.kuayue.packs.history.PackHistoryCompatibility;
import cn.kycraft.kuayue.packs.history.PackHistoryGuard;
import cn.kycraft.kuayue.packs.origin.PackOriginIndex;
import cn.kycraft.kuayue.packs.origin.SystemOriginTags;
import cn.kycraft.kuayue.packs.runtime.LegacyPackCacheCleaner;
import cn.kycraft.kuayue.packs.runtime.PackCaptureException;
import cn.kycraft.kuayue.packs.runtime.PackResourceSnapshotter;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import lib.kasuga.core.resource.ResourceSystem;
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
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private static volatile AutoCloseable resourceGroup;
    private static volatile PackOriginIndex originIndex;

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
        Path embeddedPacksRoot = findEmbeddedPacksRoot(kuaYueContainer);
        PackCatalog embeddedCatalog = embeddedPacksRoot == null
                ? new PackCatalog(List.of(), List.of(), List.of())
                : PackDiscovery.scan(embeddedPacksRoot, limits);
        PackDiscoveryResult discovery = PackDiscovery.discover(
                gameDirectory,
                environment,
                embeddedCatalog,
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
        Set<String> embeddedIds = embeddedCatalog.packs().stream()
                .map(DiscoveredPack::packId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, PackIcon> packIcons = new LinkedHashMap<>();
        discovery.catalog().packs().stream()
                .filter(pack -> pack.icon() != null)
                .forEach(pack -> packIcons.put(pack.packId(), pack.icon()));
        Map<String, PackDisplayText> packDisplayTexts = new LinkedHashMap<>();
        discovery.catalog().packs().forEach(pack ->
                packDisplayTexts.put(pack.packId(), pack.displayText()));
        Map<String, StartupPackSnapshot.ManagedPackRoot> rootsById = new LinkedHashMap<>();
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        List<PackDiagnostic> captureDiagnostics = new ArrayList<>();
        Set<String> captureFailures = new LinkedHashSet<>();
        int nextRank = 1;
        List<ResolvedPack> selectedPacks = discovery.selection().effectivePacks();
        try {
            for (ResolvedPack resolved : selectedPacks) {
                boolean embedded = embeddedIds.contains(resolved.id());
                Path installation;
                DiscoveredPack pack = discoveredById.get(resolved.id());
                if (pack == null) {
                    throw new IllegalStateException(
                            "Effective pack is missing from catalog: " + resolved.id());
                }
                if (embedded) {
                    if (embeddedPacksRoot == null) {
                        throw new IllegalStateException(
                                "Embedded pack root disappeared while loading " + resolved.id());
                    }
                    installation = embeddedPacksRoot.resolve(pack.containerName());
                } else {
                    installation = PackResourceSnapshotter.installationPath(gameDirectory, pack);
                }
                if (pack.icon() != null) {
                    packIcons.put(resolved.id(), pack.icon());
                }
                dependencies.put(
                        resolved.id(),
                        Set.copyOf(pack.manifest().dependencies().packs().keySet())
                );
                int firstRank = nextRank;
                int rootCount = 1 + pack.manifest().parcels().size();
                try {
                    PackResourceSnapshotter.CapturedPack captured =
                            PackResourceSnapshotter.capture(
                                    installation,
                                    pack,
                                    limits,
                                    firstRank
                            );
                    rootsById.put(resolved.id(), new StartupPackSnapshot.ManagedPackRoot(
                            resolved.id(),
                            pack.manifest().version(),
                            embedded,
                            pack.manifest(),
                            pack.parcels(),
                            installation,
                            captured,
                            Set.copyOf(pack.manifest().namespaces())
                    ));
                    nextRank = captured.nextRank();
                } catch (PackCaptureException failure) {
                    if (embedded || failure.kind() == PackCaptureException.Kind.HOST_FAILURE) {
                        throw failure;
                    }
                    captureFailures.add(resolved.id());
                    captureDiagnostics.add(new PackDiagnostic(
                            PackDiagnosticCodes.CAPTURE_FAILED,
                            PackSeverity.ERROR,
                            PackStage.CONTENT,
                            resolved.id(),
                            pack.sourceId(),
                            null,
                            "pack.json",
                            "",
                            List.of(),
                            failure.getMessage()
                    ));
                    nextRank = Math.addExact(firstRank, rootCount);
                }
            }
        } catch (IOException | RuntimeException failure) {
            closeQuietly(runtimeRoot);
            closeQuietly(rootsById.values());
            throw new IllegalStateException("Unable to capture KuaYue pack resources", failure);
        }

        if (!captureFailures.isEmpty()) {
            Set<String> blocked = PackFailurePolicy.includeDependents(
                    captureFailures,
                    selectedPacks.stream().map(ResolvedPack::id).toList(),
                    dependencies
            );
            for (String id : blocked) {
                StartupPackSnapshot.ManagedPackRoot removed = rootsById.remove(id);
                closeQuietly(removed);
                dependencies.remove(id);
            }
        }
        List<StartupPackSnapshot.ManagedPackRoot> roots = new ArrayList<>(rootsById.values());
        List<PackHistoryCompatibility.CurrentPack> historyPacks = new ArrayList<>(
                roots.stream()
                        .map(root -> PackHistoryCompatibility.CurrentPack.from(root.manifest()))
                        .toList()
        );
        historyPacks.sort(Comparator.comparing(PackHistoryCompatibility.CurrentPack::id));
        PackStartupReport finalReport = discovery.report();
        if (!captureDiagnostics.isEmpty()) {
            finalReport = finalReport.withRegistrationState(
                    rootsById.keySet(),
                    captureDiagnostics
            );
        }
        try {
            finalReport.write(reportPath);
        } catch (IOException failure) {
            closeQuietly(runtimeRoot);
            closeQuietly(rootsById.values());
            throw new IllegalStateException(
                    "Unable to publish captured KuaYue pack report",
                    failure
            );
        }
        Map<String, PackManifest> knownManifests = new LinkedHashMap<>();
        discovery.catalog().packs().forEach(pack ->
                knownManifests.put(pack.packId(), pack.manifest()));
        rootsById.values().forEach(root ->
                knownManifests.put(root.id(), root.manifest()));
        LOGGER.debug("Known pack settings manifests: {}", knownManifests.keySet());

        StartupPackSnapshot snapshot = new StartupPackSnapshot(
                gameDirectory,
                runtimeRoot,
                discovery.configDocument().config(),
                knownManifests,
                packIcons,
                packDisplayTexts,
                roots,
                dependencies,
                historyPacks,
                finalReport
        );
        PackRuntimeState.install(snapshot.config());
        SNAPSHOT.set(snapshot);
        NeoForge.EVENT_BUS.addListener(
                ServerAboutToStartEvent.class,
                PackHistoryGuard::onServerAboutToStart
        );
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, PackHistoryGuard::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Save.class, PackHistoryGuard::onLevelSave);
        NeoForge.EVENT_BUS.addListener(
                ServerStoppedEvent.class,
                PackHistoryGuard::onServerStopped
        );
        LOGGER.info(
                "Frozen {} effective KuaYue packs: {}",
                roots.size(),
                roots.stream().map(StartupPackSnapshot.ManagedPackRoot::id).toList()
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
        SystemOriginTags.Result tagged = null;
        AutoCloseable pending = null;
        try {
            PackOriginIndex currentOrigin = PackOriginIndex.current();
            int nextRank = snapshot.resourceRoots().stream()
                    .flatMap(root -> root.capture().roots().stream())
                    .mapToInt(PackResourceSnapshotter.CapturedRoot::rank)
                    .max()
                    .orElse(0);
            tagged = SystemOriginTags.create(
                    currentOrigin,
                    (long) nextRank + 1L,
                    PackLimits.defaults()
            );
            sources.add(tagged.source());
            pending = resourceSystem.getApprovedPacks().registerOrderedGroup(
                    KuaYueMcPackProjection.create(List.copyOf(sources))
            );
            try {
                tagged.source().close();
            } catch (IOException closeFailure) {
                try {
                    pending.close();
                } catch (Exception cleanup) {
                    closeFailure.addSuppressed(cleanup);
                }
                pending = null;
                throw closeFailure;
            }
            tagged = null;
            resourceSystem.registerManagedCloseable(KuaYuePackBootstrap::closePackResources);
            resourceGroup = pending;
            pending = null;
            originIndex = currentOrigin;
        } catch (IOException failure) {
            cleanupFailedInstall(pending, failure);
            if (tagged != null) cleanupFailedInstall(tagged.source(), failure);
            RESOURCES_INSTALLED.set(false);
            closePackResources();
            throw new IllegalStateException(
                    "Unable to install KuaYue managed resource group",
                    failure
            );
        } catch (RuntimeException failure) {
            cleanupFailedInstall(pending, failure);
            if (tagged != null) cleanupFailedInstall(tagged.source(), failure);
            RESOURCES_INSTALLED.set(false);
            closePackResources();
            throw failure;
        }
    }

    public static Optional<StartupPackSnapshot> snapshot() {
        return Optional.ofNullable(SNAPSHOT.get());
    }

    public static Optional<PackOriginIndex> originIndex() {
        return Optional.ofNullable(originIndex);
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

    private static Path findEmbeddedPacksRoot(ModContainer kuaYueContainer) {
        Path root = ModList.get().getModFiles().stream()
                .filter(file -> file.getMods().stream()
                        .anyMatch(mod -> mod.getModId().equals("kuayue")))
                .map(file -> file.getFile().findResource("kuayue-packs"))
                .filter(java.nio.file.Files::isDirectory)
                .findFirst()
                .orElse(null);
        if (root != null) {
            return root;
        }
        Path fallback = kuaYueContainer.getModInfo().getOwningFile()
                .getFile().findResource("kuayue-packs");
        return java.nio.file.Files.isDirectory(fallback) ? fallback : null;
    }

    private static void closePackResources() {
        AutoCloseable group = resourceGroup;
        resourceGroup = null;
        closeQuietly(group);
        originIndex = null;
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

    private static void cleanupFailedInstall(AutoCloseable pending, Throwable primary) {
        if (pending == null) return;
        try {
            pending.close();
        } catch (Exception cleanup) {
            primary.addSuppressed(cleanup);
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
