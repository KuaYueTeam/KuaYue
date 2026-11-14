package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
}
