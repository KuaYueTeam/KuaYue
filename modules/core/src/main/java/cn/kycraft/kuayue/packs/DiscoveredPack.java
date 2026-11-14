package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DiscoveredPack(
        String sourceId,
        PackSourceKind sourceKind,
        String containerName,
        PackManifest manifest,
        Map<String, ParcelManifest> parcels,
        List<PackFileEntry> files
) implements Serializable {
    public DiscoveredPack {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(sourceKind, "sourceKind");
        Objects.requireNonNull(containerName, "containerName");
        Objects.requireNonNull(manifest, "manifest");
        parcels = Collections.unmodifiableMap(new LinkedHashMap<>(parcels));
        files = files.stream().sorted().toList();
    }

    public String packId() {
        return manifest.id();
    }
}
