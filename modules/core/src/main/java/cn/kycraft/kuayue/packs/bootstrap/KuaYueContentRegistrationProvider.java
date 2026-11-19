package cn.kycraft.kuayue.packs.bootstrap;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.packs.PackDiagnostic;
import cn.kycraft.kuayue.packs.PackFailurePolicy;
import cn.kycraft.kuayue.packs.PackRuntimeIds;
import cn.kycraft.kuayue.packs.PackSeverity;
import cn.kycraft.kuayue.packs.PackStage;
import cn.kycraft.kuayue.packs.runtime.PackResourceSnapshotter;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.track.TrackMaterial;
import lib.kasuga.registration.data_driven.TypeHandlerRegistry;
import lib.kasuga.registration.data_driven.builder.JsonTreeBuilder;
import lib.kasuga.registration.data_driven.context.JsonRegistryGroup;
import lib.kasuga.registration.data_driven.pipeline.BuildOptions;
import lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic;
import lib.kasuga.registration.data_driven.pipeline.BuildResult;
import lib.kasuga.registration.data_driven.pipeline.DataDrivenBuildHooks;
import lib.kasuga.registration.data_driven.pipeline.DataDrivenBuildPipeline;
import lib.kasuga.registration.data_driven.pipeline.PreparedContent;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaimSnapshot;
import lib.kasuga.registration.data_driven.pipeline.RegistrationTarget;
import lib.kasuga.registration.data_driven.reload.ManagedReloadAllowlist;
import lib.kasuga.registration.data_driven.source.ContentSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds one strict registration session from the frozen KuaYue pack snapshot.
 */
public final class KuaYueContentRegistrationProvider {
    private static final Logger LOGGER =
            LoggerFactory.getLogger("KuaYue/Packs/Registration");
    private static final String RUNTIME_OWNER_ID = "kuayue-runtime";

    private KuaYueContentRegistrationProvider() {
    }

    public static JsonRegistryGroup build(String hostModId) {
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot()
                .orElseThrow(() -> new IllegalStateException("KuaYue pack snapshot is not initialized"));
        JsonTreeBuilder.ensureHandlersRegistered();

        DataDrivenBuildPipeline pipeline = new DataDrivenBuildPipeline(
                hostModId,
                TypeHandlerRegistry.all(),
                DataDrivenBuildHooks.finalizersFor(hostModId),
                RegistrationTarget.standard(),
                List.of(),
                reservedRegistryClaims()
        );
        List<StartupPackSnapshot.ManagedPackRoot> activeRoots =
                new ArrayList<>(snapshot.resourceRoots());
        Set<String> protectedOwners = new LinkedHashSet<>();
        protectedOwners.add(RUNTIME_OWNER_ID);
        snapshot.resourceRoots().stream()
                .filter(root -> root.manifest().required())
                .map(StartupPackSnapshot.ManagedPackRoot::id)
                .forEach(protectedOwners::add);
        List<BuildDiagnostic> rejectedDiagnostics = new ArrayList<>();
        PreparedContent prepared;
        try {
            while (true) {
                List<ContentSource> sources = openSources(activeRoots);
                try {
                    sources.add(0, snapshot.runtimeRoot().contentSource());
                    prepared = pipeline.prepare(
                            sources,
                            BuildOptions.STRICT,
                            activeDependencies(
                                    activeRoots,
                                    snapshot.dependencies(),
                                    protectedOwners
                            ),
                            Set.of(RUNTIME_OWNER_ID)
                    );
                } finally {
                    closeSources(sources);
                }

                Set<String> rejected = blockingOwners(prepared, protectedOwners);
                if (rejected.isEmpty()) {
                    break;
                }
                Set<String> blocked = PackFailurePolicy.includeDependents(
                        rejected, activeIds(activeRoots), snapshot.dependencies());
                if (blocked.stream().anyMatch(protectedOwners::contains)
                        || !activeIds(activeRoots).containsAll(blocked)) {
                    throw new IllegalStateException(
                            "KuaYue runtime or required preset failed strict preflight: "
                                    + prepared.diagnostics());
                }

                rejectedDiagnostics.addAll(prepared.diagnostics().stream()
                        .filter(BuildDiagnostic::blocking)
                        .filter(diagnostic -> diagnostic.ownerId() != null
                                && blocked.contains(diagnostic.ownerId()))
                        .toList());
                activeRoots.removeIf(root -> blocked.contains(root.id()));
                LOGGER.warn("Rejected KuaYue packs after strict preflight: {}", blocked);
            }

            LOGGER.info(
                    "Prepared {} definitions across {} pack sources and {} registry claims",
                    prepared.definitions().size(),
                    prepared.sources().size(),
                    prepared.claims().size()
            );
            if (prepared.hasBlockingDiagnostics()) {
                throw new IllegalStateException(
                        "KuaYue pack strict preflight failed: " + prepared.diagnostics());
            }
            BuildResult result = pipeline.build(prepared);
            if (!result.successful()) {
                throw new IllegalStateException(
                        "KuaYue pack registration build failed: " + result.diagnostics(),
                        result.cause()
                );
            }
            Map<String, String> projectedSourcePackIds = new LinkedHashMap<>();
            for (StartupPackSnapshot.ManagedPackRoot root : activeRoots) {
                for (PackResourceSnapshotter.CapturedRoot captured : root.capture().roots()) {
                    var descriptor = captured.source().descriptor();
                    projectedSourcePackIds.put(
                            descriptor.sourceId() + "/" + descriptor.rootId(),
                            KuaYueMcPackProjection.childPackId(descriptor)
                    );
                }
            }
            ManagedReloadAllowlist.install(
                    hostModId,
                    prepared.sources(),
                    projectedSourcePackIds,
                    prepared.indexFiles(),
                    prepared.reloadFiles()
            );
            Set<String> activePackIds = activeIds(activeRoots);
            PackRuntimeState.updateRegistrationSelection(activePackIds, prepared.claims());
            List<BuildDiagnostic> reportDiagnostics = new ArrayList<>(rejectedDiagnostics);
            reportDiagnostics.addAll(prepared.diagnostics());
            try {
                snapshot.report()
                        .withRegistrationState(activePackIds, reportDiagnostics.stream()
                                .map(KuaYueContentRegistrationProvider::toPackDiagnostic)
                                .toList())
                        .write(snapshot.gameDirectory()
                                .resolve("kuayue/reports/packs/startup.json"));
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Unable to update KuaYue pack startup report", exception);
            }
            LOGGER.info("Applied KuaYue pack registration session");
            return result.rootGroup();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open KuaYue pack content", exception);
        }
    }

    private static List<ContentSource> openSources(
            List<StartupPackSnapshot.ManagedPackRoot> roots
    ) throws IOException {
        List<ContentSource> sources = new ArrayList<>(roots.size());
        try {
            for (StartupPackSnapshot.ManagedPackRoot root : roots) {
                for (PackResourceSnapshotter.CapturedRoot captured : root.capture().roots()) {
                    sources.add(captured.contentSource());
                }
            }
            return sources;
        } catch (IOException | RuntimeException failure) {
            closeSources(sources);
            throw failure;
        }
    }

    private static void closeSources(List<ContentSource> sources) {
        for (ContentSource source : sources) {
            try {
                source.close();
            } catch (IOException ignored) {
                // Preparation has already copied every definition into immutable memory.
            }
        }
    }

    private static Map<String, Set<String>> activeDependencies(
            List<StartupPackSnapshot.ManagedPackRoot> roots,
            Map<String, Set<String>> dependencies,
            Set<String> protectedOwners
    ) {
        Map<String, Set<String>> active = new LinkedHashMap<>();
        Set<String> runtimeDependencies = roots.stream()
                .map(StartupPackSnapshot.ManagedPackRoot::id)
                .filter(protectedOwners::contains)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        active.put(RUNTIME_OWNER_ID, runtimeDependencies);
        for (StartupPackSnapshot.ManagedPackRoot root : roots) {
            active.put(root.id(), Set.copyOf(
                    dependencies.getOrDefault(root.id(), Set.of())));
        }
        return Map.copyOf(active);
    }

    static Set<String> blockingOwners(
            PreparedContent prepared,
            Set<String> protectedOwners
    ) {
        Map<String, String> ownerBySourceId = new LinkedHashMap<>();
        prepared.sources().forEach(source ->
                ownerBySourceId.put(source.sourceId(), source.ownerId()));

        Set<String> owners = new LinkedHashSet<>();
        for (BuildDiagnostic diagnostic : prepared.diagnostics()) {
            if (!diagnostic.blocking()) {
                continue;
            }
            String owner = diagnostic.ownerId();
            if (owner == null && diagnostic.sourceId() != null) {
                owner = ownerBySourceId.get(diagnostic.sourceId());
            }
            if (diagnostic.code() == BuildDiagnostic.Code.CLAIM_CONFLICT) {
                if (owner != null && !protectedOwners.contains(owner)) {
                    owners.add(owner);
                }
                continue;
            }
            if (owner == null) {
                throw new IllegalStateException(
                        "KuaYue strict preflight reported a session-wide failure: "
                                + diagnostic);
            }
            owners.add(owner);
        }

        Map<RegistryClaim.RegistryKey, Set<String>> claimOwners = new LinkedHashMap<>();
        for (RegistryClaim claim : prepared.claims()) {
            claimOwners.computeIfAbsent(
                    claim.registryKey(), ignored -> new LinkedHashSet<>())
                    .add(claim.source().ownerId());
        }
        claimOwners.values().stream()
                .filter(claimants -> claimants.size() > 1)
                .forEach(claimants -> {
                    if (claimants.stream().anyMatch(protectedOwners::contains)) {
                        claimants.stream()
                                .filter(owner -> !protectedOwners.contains(owner))
                                .forEach(owners::add);
                    } else {
                        owners.addAll(claimants);
                    }
                });
        return Set.copyOf(owners);
    }

    private static Set<String> activeIds(
            List<StartupPackSnapshot.ManagedPackRoot> roots
    ) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        roots.forEach(root -> ids.add(root.id()));
        return Set.copyOf(ids);
    }

    private static PackDiagnostic toPackDiagnostic(BuildDiagnostic diagnostic) {
        String message = diagnostic.suggestion() == null
                ? diagnostic.message()
                : diagnostic.message() + " Suggestion: " + diagnostic.suggestion();
        return new PackDiagnostic(
                diagnostic.code().name(),
                PackSeverity.valueOf(diagnostic.severity().name()),
                PackStage.CONTENT,
                diagnostic.ownerId(),
                diagnostic.sourceId(),
                null,
                diagnostic.logicalPath(),
                diagnostic.jsonPointer(),
                List.of(),
                message
        );
    }

    private static Set<RegistryClaim.RegistryKey> reservedRegistryClaims() {
        Set<RegistryClaim.RegistryKey> reserved =
                new LinkedHashSet<>(RegistryClaimSnapshot.collect(KuaYue.REGISTRY));
        BuiltInRegistries.BLOCK.keySet().forEach(id ->
                reserved.add(new RegistryClaim.RegistryKey("minecraft:block", id)));
        BuiltInRegistries.ITEM.keySet().forEach(id ->
                reserved.add(new RegistryClaim.RegistryKey("minecraft:item", id)));
        BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().forEach(id ->
                reserved.add(new RegistryClaim.RegistryKey("minecraft:block_entity_type", id)));
        TrackMaterial.ALL.keySet().forEach(id ->
                reserved.add(new RegistryClaim.RegistryKey("create:track_material", id)));
        BogeySizes.all().keySet().forEach(id ->
                reserved.add(new RegistryClaim.RegistryKey("create:bogey_size", id)));
        AllBogeyStyles.BOGEY_STYLES.keySet().forEach(id ->
                reserved.add(new RegistryClaim.RegistryKey("create:bogey_style", id)));
        reserved.add(new RegistryClaim.RegistryKey(
                "minecraft:block_entity_type",
                PackRuntimeIds.BOGEY_BLOCK_ENTITY_ID
        ));
        return Set.copyOf(reserved);
    }
}
