package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

final class PackDependencyResolver {
    PackSelection resolve(
            PackConfig config,
            PackCatalog catalog,
            CorePackDescriptor corepack,
            PackRuntimeEnvironment environment,
            VersionComparator modComparator
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(corepack, "corepack");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(modComparator, "modComparator");

        Map<String, DiscoveredPack> packs = new HashMap<>();
        for (DiscoveredPack pack : catalog.packs()) {
            packs.put(pack.packId(), pack);
        }
        Set<String> invalidInstalledIds = new HashSet<>();
        for (PackInstallation installation : catalog.installations()) {
            if (!installation.valid() && installation.packId() != null) {
                invalidInstalledIds.add(installation.packId());
            }
        }

        Set<String> requested = new TreeSet<>();
        for (var entry : config.enabled().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                requested.add(entry.getKey());
            }
        }
        if (config.isRequestedEnabled(PackManifest.CORE_PACK_ID)) {
            requested.add(PackManifest.CORE_PACK_ID);
        }

        Set<String> blocked = new TreeSet<>();
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        for (String packId : requested) {
            if (PackManifest.CORE_PACK_ID.equals(packId)) {
                continue;
            }
            DiscoveredPack pack = packs.get(packId);
            if (pack == null) {
                if (invalidInstalledIds.contains(packId)) {
                    blocked.add(packId);
                    diagnostics.add(new PackDiagnostic(
                            PackDiagnosticCodes.DEPENDENCY_INVALID,
                            PackSeverity.ERROR,
                            PackStage.DEPENDENCY,
                            packId,
                            "catalog",
                            null,
                            "pack.json",
                            "",
                            List.of(),
                            "Enabled pack installation is invalid"));
                } else {
                    blocked.add(packId);
                    diagnostics.add(new PackDiagnostic(
                            PackDiagnosticCodes.DEPENDENCY_MISSING,
                            PackSeverity.ERROR,
                            PackStage.DEPENDENCY,
                            packId,
                            "catalog",
                            null,
                            "pack.json",
                            "",
                            List.of(),
                            "Enabled pack is not installed"));
                }
                continue;
            }
            validateHostDependencies(
                    pack, environment, modComparator, blocked, diagnostics);
            validatePackDependencies(
                    pack, config, packs, invalidInstalledIds, corepack, blocked, diagnostics);
        }

        Set<String> cycleNodes = findCycleNodes(requested, packs);
        for (String cycleNode : cycleNodes) {
            blocked.add(cycleNode);
            diagnostics.add(new PackDiagnostic(
                    PackDiagnosticCodes.DEPENDENCY_CYCLE,
                    PackSeverity.ERROR,
                    PackStage.DEPENDENCY,
                    cycleNode,
                    "catalog",
                    null,
                    "pack.json",
                    "/dependencies/packs",
                    cycleNodes.stream().sorted().toList(),
                    "Pack participates in a required dependency cycle"));
        }

        boolean changed;
        do {
            changed = false;
            for (String packId : requested) {
                if (PackManifest.CORE_PACK_ID.equals(packId)
                        || blocked.contains(packId) || !packs.containsKey(packId)) {
                    continue;
                }
                List<String> blockedDependencies = packs.get(packId)
                        .manifest().dependencies().packs().keySet().stream()
                        .filter(blocked::contains)
                        .sorted()
                        .toList();
                if (!blockedDependencies.isEmpty()) {
                    blocked.add(packId);
                    diagnostics.add(new PackDiagnostic(
                            PackDiagnosticCodes.DEPENDENCY_INVALID,
                            PackSeverity.ERROR,
                            PackStage.DEPENDENCY,
                            packId,
                            packs.get(packId).sourceId(),
                            null,
                            "pack.json",
                            "/dependencies/packs",
                            blockedDependencies,
                            "Pack depends on a blocked pack"));
                    changed = true;
                }
            }
        } while (changed);

        List<ResolvedPack> effective = new ArrayList<>();
        LinkedHashSet<String> orderedCandidates = new LinkedHashSet<>(config.order());
        orderedCandidates.addAll(requested);
        for (String packId : orderedCandidates) {
            if (!requested.contains(packId) || blocked.contains(packId)) {
                continue;
            }
            if (PackManifest.CORE_PACK_ID.equals(packId)) {
                effective.add(new ResolvedPack(
                        packId, corepack.version(), PackSourceKind.CORE, "core"));
                continue;
            }
            DiscoveredPack pack = packs.get(packId);
            if (pack != null) {
                effective.add(new ResolvedPack(
                        pack.packId(),
                        pack.manifest().version(),
                        pack.sourceKind(),
                        pack.sourceId()));
            }
        }
        return new PackSelection(effective, blocked, diagnostics);
    }

    private void validateHostDependencies(
            DiscoveredPack pack,
            PackRuntimeEnvironment environment,
            VersionComparator comparator,
            Set<String> blocked,
            List<PackDiagnostic> diagnostics
    ) {
        for (var dependency : pack.manifest().dependencies().mods().entrySet()) {
            String actualVersion = environment.version(dependency.getKey());
            boolean matches;
            try {
                matches = dependency.getValue().matches(actualVersion, comparator);
            } catch (RuntimeException exception) {
                matches = false;
            }
            if (!matches) {
                blocked.add(pack.packId());
                diagnostics.add(new PackDiagnostic(
                        PackDiagnosticCodes.VERSION_MISMATCH,
                        PackSeverity.ERROR,
                        PackStage.DEPENDENCY,
                        pack.packId(),
                        pack.sourceId(),
                        null,
                        "pack.json",
                        "/dependencies/mods/" + JsonProtocol.escape(dependency.getKey()),
                        List.of(dependency.getKey()),
                        "Required mod version does not satisfy "
                                + dependency.getValue().expression() + ": "
                                + dependency.getKey() + "=" + actualVersion));
            }
        }
    }

    private void validatePackDependencies(
            DiscoveredPack owner,
            PackConfig config,
            Map<String, DiscoveredPack> packs,
            Set<String> invalidInstalledIds,
            CorePackDescriptor corepack,
            Set<String> blocked,
            List<PackDiagnostic> diagnostics
    ) {
        for (var dependency : owner.manifest().dependencies().packs().entrySet()) {
            String dependencyId = dependency.getKey();
            String pointer = "/dependencies/packs/" + JsonProtocol.escape(dependencyId);
            SemanticVersion actualVersion;
            if (PackManifest.CORE_PACK_ID.equals(dependencyId)) {
                actualVersion = corepack.version();
            } else {
                DiscoveredPack dependencyPack = packs.get(dependencyId);
                if (dependencyPack == null) {
                    blocked.add(owner.packId());
                    String missingCode = invalidInstalledIds.contains(dependencyId)
                            ? PackDiagnosticCodes.DEPENDENCY_INVALID
                            : PackDiagnosticCodes.DEPENDENCY_MISSING;
                    diagnostics.add(new PackDiagnostic(
                            missingCode,
                            PackSeverity.ERROR,
                            PackStage.DEPENDENCY,
                            owner.packId(),
                            owner.sourceId(),
                            null,
                            "pack.json",
                            pointer,
                            List.of(dependencyId),
                            "Required dependency is not loadable: " + dependencyId));
                    continue;
                }
                actualVersion = dependencyPack.manifest().version();
            }
            if (!config.isRequestedEnabled(dependencyId)) {
                blocked.add(owner.packId());
                diagnostics.add(new PackDiagnostic(
                        PackDiagnosticCodes.DEPENDENCY_DISABLED,
                        PackSeverity.ERROR,
                        PackStage.DEPENDENCY,
                        owner.packId(),
                        owner.sourceId(),
                        null,
                        "pack.json",
                        pointer,
                        List.of(dependencyId),
                        "Required dependency is disabled: " + dependencyId));
                continue;
            }
            if (!dependency.getValue().matches(actualVersion)) {
                blocked.add(owner.packId());
                diagnostics.add(new PackDiagnostic(
                        PackDiagnosticCodes.VERSION_MISMATCH,
                        PackSeverity.ERROR,
                        PackStage.DEPENDENCY,
                        owner.packId(),
                        owner.sourceId(),
                        null,
                        "pack.json",
                        pointer,
                        List.of(dependencyId),
                        "Dependency version " + actualVersion + " does not satisfy "
                                + dependency.getValue().expression()));
            }
        }
    }

    private Set<String> findCycleNodes(
            Set<String> requested,
            Map<String, DiscoveredPack> packs
    ) {
        Set<String> cycleNodes = new TreeSet<>();
        for (String start : requested) {
            DiscoveredPack pack = packs.get(start);
            if (pack == null) {
                continue;
            }
            if (reaches(
                    start,
                    pack,
                    packs,
                    requested,
                    new HashSet<>(Set.of(start)))) {
                cycleNodes.add(start);
            }
        }
        return cycleNodes;
    }

    private boolean reaches(
            String target,
            DiscoveredPack current,
            Map<String, DiscoveredPack> packs,
            Set<String> requested,
            Set<String> path
    ) {
        for (String dependencyId : current.manifest().dependencies().packs().keySet()) {
            if (!requested.contains(dependencyId)) {
                continue;
            }
            if (dependencyId.equals(target)) {
                return true;
            }
            DiscoveredPack dependency = packs.get(dependencyId);
            if (dependency != null && path.add(dependencyId)) {
                boolean found = reaches(target, dependency, packs, requested, path);
                path.remove(dependencyId);
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }
}
