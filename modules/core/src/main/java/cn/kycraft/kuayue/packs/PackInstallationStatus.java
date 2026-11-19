package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PackInstallationStatus(
        String sourceId,
        PackSourceKind sourceKind,
        String containerName,
        String packId,
        SemanticVersion packVersion,
        boolean requestedEnabled,
        boolean effective,
        boolean valid,
        List<String> diagnosticCodes
) implements Serializable {
    public PackInstallationStatus {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(sourceKind, "sourceKind");
        Objects.requireNonNull(containerName, "containerName");
        diagnosticCodes = List.copyOf(new ArrayList<>(diagnosticCodes));
    }
}
