package cn.kycraft.kuayue.packs;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Stable, serializable machine-readable startup report.
 */
public record PackStartupReport(
        int formatVersion,
        Map<String, String> environment,
        List<PackInstallationStatus> installations,
        List<ResolvedPack> effectivePacks,
        List<PackDiagnostic> diagnostics
) implements Serializable {
    public static final int FORMAT_VERSION = 1;

    public PackStartupReport {
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported report format: " + formatVersion);
        }
        environment = Collections.unmodifiableMap(new TreeMap<>(environment));
        installations = List.copyOf(new ArrayList<>(installations));
        effectivePacks = List.copyOf(new ArrayList<>(effectivePacks));
        diagnostics = List.copyOf(new ArrayList<>(diagnostics))
                .stream().sorted().toList();
    }

    public static PackStartupReport create(
            PackRuntimeEnvironment environment,
            PackConfig config,
            CorePackDescriptor corepack,
            PackCatalog catalog,
            PackSelection selection,
            List<PackDiagnostic> diagnostics
    ) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(corepack, "corepack");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(selection, "selection");

        Map<String, ResolvedPack> effectiveById = new LinkedHashMap<>();
        for (ResolvedPack pack : selection.effectivePacks()) {
            effectiveById.put(pack.id(), pack);
        }
        List<PackInstallationStatus> installations = new ArrayList<>();
        boolean coreListed = false;
        for (PackInstallation installation : catalog.installations()) {
            String packId = installation.packId();
            boolean requested = packId != null && config.isRequestedEnabled(packId);
            boolean effective = packId != null && effectiveById.containsKey(packId);
            installations.add(new PackInstallationStatus(
                    installation.sourceId(),
                    installation.sourceKind(),
                    installation.containerName(),
                    packId,
                    installation.packVersion(),
                    requested,
                    effective,
                    installation.valid(),
                    installation.diagnosticCodes()));
            coreListed |= PackManifest.CORE_PACK_ID.equals(packId);
        }
        if (!coreListed) {
            boolean coreRequested = config.isRequestedEnabled(PackManifest.CORE_PACK_ID);
            installations.add(new PackInstallationStatus(
                    "core",
                    PackSourceKind.CORE,
                    "built-in",
                    PackManifest.CORE_PACK_ID,
                    corepack.version(),
                    coreRequested,
                    effectiveById.containsKey(PackManifest.CORE_PACK_ID),
                    true,
                    List.of()));
        }
        installations.sort((left, right) -> {
            String leftId = left.packId() == null ? "" : left.packId();
            String rightId = right.packId() == null ? "" : right.packId();
            int result = leftId.compareTo(rightId);
            return result != 0 ? result : left.sourceId().compareTo(right.sourceId());
        });
        return new PackStartupReport(
                FORMAT_VERSION,
                environment.versions(),
                installations,
                selection.effectivePacks(),
                diagnostics);
    }

    public PackStartupReport withRegistrationState(
            Set<String> effectivePackIds,
            List<PackDiagnostic> registrationDiagnostics
    ) {
        Objects.requireNonNull(effectivePackIds, "effectivePackIds");
        Objects.requireNonNull(registrationDiagnostics, "registrationDiagnostics");
        Map<String, List<String>> registrationCodes = new LinkedHashMap<>();
        for (PackDiagnostic diagnostic : registrationDiagnostics) {
            if (diagnostic.blocking() && diagnostic.ownerId() != null) {
                registrationCodes.computeIfAbsent(
                        diagnostic.ownerId(), ignored -> new ArrayList<>())
                        .add(diagnostic.code());
            }
        }
        List<PackInstallationStatus> updatedInstallations = installations.stream()
                .map(installation -> {
                    boolean effective = installation.packId() != null
                            && effectivePackIds.contains(installation.packId());
                    List<String> codes = new ArrayList<>(installation.diagnosticCodes());
                    if (installation.packId() != null) {
                        codes.addAll(registrationCodes.getOrDefault(
                                installation.packId(), List.of()));
                    }
                    return new PackInstallationStatus(
                            installation.sourceId(),
                            installation.sourceKind(),
                            installation.containerName(),
                            installation.packId(),
                            installation.packVersion(),
                            installation.requestedEnabled(),
                            effective,
                            installation.valid()
                                    && (!installation.requestedEnabled() || effective),
                            codes
                    );
                })
                .toList();
        List<ResolvedPack> updatedPacks = effectivePacks.stream()
                .filter(pack -> effectivePackIds.contains(pack.id()))
                .toList();
        List<PackDiagnostic> updatedDiagnostics =
                new ArrayList<>(diagnostics.size() + registrationDiagnostics.size());
        updatedDiagnostics.addAll(diagnostics);
        updatedDiagnostics.addAll(registrationDiagnostics);
        return new PackStartupReport(
                formatVersion,
                environment,
                updatedInstallations,
                updatedPacks,
                updatedDiagnostics
        );
    }

    public String toJson() {
        return PackStartupReportCodec.encode(this);
    }

    public void write(Path path) throws IOException {
        PackStartupReportCodec.write(path, this);
    }
}
