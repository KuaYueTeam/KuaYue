package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PackCatalog(
        List<DiscoveredPack> packs,
        List<PackInstallation> installations,
        List<PackDiagnostic> diagnostics
) implements Serializable {
    public PackCatalog {
        packs = List.copyOf(new ArrayList<>(packs));
        installations = List.copyOf(new ArrayList<>(installations));
        diagnostics = List.copyOf(new ArrayList<>(diagnostics))
                .stream().sorted().toList();
    }

    public static PackCatalog merge(PackCatalog embedded, PackCatalog external) {
        List<DiscoveredPack> packs = new ArrayList<>(embedded.packs());
        packs.addAll(external.packs());
        Map<String, List<DiscoveredPack>> packsById = new LinkedHashMap<>();
        for (DiscoveredPack pack : packs) {
            packsById.computeIfAbsent(pack.packId(), ignored -> new ArrayList<>())
                    .add(pack);
        }

        Set<String> duplicateIds = new HashSet<>();
        Set<String> embeddedInstallationIds = embedded.installations().stream()
                .map(PackInstallation::packId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        external.installations().stream()
                .map(PackInstallation::packId)
                .filter(java.util.Objects::nonNull)
                .filter(embeddedInstallationIds::contains)
                .forEach(duplicateIds::add);
        List<PackDiagnostic> diagnostics = new ArrayList<>(embedded.diagnostics());
        diagnostics.addAll(external.diagnostics());
        for (var entry : packsById.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            duplicateIds.add(entry.getKey());
            List<String> sourceIds = entry.getValue().stream()
                    .map(DiscoveredPack::sourceId)
                    .sorted()
                    .toList();
            for (DiscoveredPack duplicate : entry.getValue()) {
                diagnostics.add(new PackDiagnostic(
                        PackDiagnosticCodes.DUPLICATE_PACK_ID,
                        PackSeverity.ERROR,
                        PackStage.DISCOVERY,
                        entry.getKey(),
                        duplicate.sourceId(),
                        null,
                        "pack.json",
                        "/id",
                        sourceIds,
                        "Pack ID is installed more than once"
                ));
            }
        }
        for (String duplicateId : duplicateIds) {
            List<DiscoveredPack> matchingPacks = packsById.getOrDefault(
                    duplicateId, List.of());
            if (matchingPacks.size() >= 2) {
                continue;
            }
            List<String> sourceIds = new ArrayList<>();
            embedded.installations().stream()
                    .filter(installation -> duplicateId.equals(installation.packId()))
                    .map(PackInstallation::sourceId)
                    .forEach(sourceIds::add);
            external.installations().stream()
                    .filter(installation -> duplicateId.equals(installation.packId()))
                    .map(PackInstallation::sourceId)
                    .forEach(sourceIds::add);
            diagnostics.add(new PackDiagnostic(
                    PackDiagnosticCodes.DUPLICATE_PACK_ID,
                    PackSeverity.ERROR,
                    PackStage.DISCOVERY,
                    duplicateId,
                    "catalog",
                    null,
                    "pack.json",
                    "/id",
                    sourceIds.stream().sorted().toList(),
                    "Pack ID is installed more than once"
            ));
        }

        List<DiscoveredPack> validPacks = packs.stream()
                .filter(pack -> !duplicateIds.contains(pack.packId()))
                .sorted(java.util.Comparator.comparing(DiscoveredPack::packId)
                        .thenComparing(DiscoveredPack::sourceId))
                .toList();

        List<PackInstallation> installations = new ArrayList<>();
        installations.addAll(embedded.installations());
        installations.addAll(external.installations());
        installations = installations.stream()
                .map(installation -> {
                    if (installation.packId() == null
                            || !duplicateIds.contains(installation.packId())) {
                        return installation;
                    }
                    List<String> codes = new ArrayList<>(installation.diagnosticCodes());
                    if (!codes.contains(PackDiagnosticCodes.DUPLICATE_PACK_ID)) {
                        codes.add(PackDiagnosticCodes.DUPLICATE_PACK_ID);
                    }
                    return new PackInstallation(
                            installation.sourceId(),
                            installation.sourceKind(),
                            installation.containerName(),
                            installation.packId(),
                            installation.packVersion(),
                            false,
                            codes
                    );
                })
                .sorted(java.util.Comparator.comparing(
                                (PackInstallation installation) ->
                                        installation.packId() == null ? "" : installation.packId())
                        .thenComparing(PackInstallation::sourceId))
                .toList();
        return new PackCatalog(validPacks, installations, diagnostics);
    }
}
