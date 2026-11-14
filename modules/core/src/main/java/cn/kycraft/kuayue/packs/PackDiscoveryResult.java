package cn.kycraft.kuayue.packs;

import java.util.Objects;

public record PackDiscoveryResult(
        PackConfigDocument configDocument,
        PackCatalog catalog,
        PackSelection selection,
        PackStartupReport report
) {
    public PackDiscoveryResult {
        Objects.requireNonNull(configDocument, "configDocument");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(report, "report");
    }
}
