package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Strict K1 pack.json representation.
 */
public record PackManifest(
        int formatVersion,
        String id,
        SemanticVersion version,
        List<String> namespaces,
        String nameKey,
        String descriptionKey,
        Dependencies dependencies,
        List<String> parcels,
        SaveCompatibility saveCompatibility
) implements Serializable {
    public static final int FORMAT_VERSION = 1;
    public static final String CORE_PACK_ID = "kuayue-core";
    public static final Set<String> HOST_MODS =
            Set.of("minecraft", "neoforge", "kuayue", "kasuga_lib", "create");

    public PackManifest {
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported pack format: " + formatVersion);
        }
        if (!PackPathRules.isPackId(id)) {
            throw new IllegalArgumentException("Invalid pack ID: " + id);
        }
        Objects.requireNonNull(version, "version");
        namespaces = List.copyOf(new ArrayList<>(namespaces));
        if (namespaces.isEmpty()) {
            throw new IllegalArgumentException("namespaces must not be empty");
        }
        for (String namespace : namespaces) {
            if (!PackPathRules.isNamespace(namespace)) {
                throw new IllegalArgumentException("Invalid namespace: " + namespace);
            }
        }
        if (nameKey == null || nameKey.isBlank()) {
            throw new IllegalArgumentException("name_key must not be empty");
        }
        if (descriptionKey != null && descriptionKey.isBlank()) {
            throw new IllegalArgumentException("description_key must not be empty");
        }
        Objects.requireNonNull(dependencies, "dependencies");
        if (dependencies.packs().containsKey(id)) {
            throw new IllegalArgumentException("A pack cannot depend on itself");
        }
        parcels = List.copyOf(new ArrayList<>(parcels));
        if (parcels.isEmpty()) {
            throw new IllegalArgumentException("parcels must not be empty");
        }
        for (String parcel : parcels) {
            if (!PackPathRules.isParcelManifestPath(parcel)) {
                throw new IllegalArgumentException("Invalid parcel manifest path: " + parcel);
            }
        }
    }

    public static PackManifest parse(String json) {
        return new PackManifestParser().parse(json, false);
    }

    public static PackManifest parseCore(String json) {
        return new PackManifestParser().parse(json, true);
    }

    public record Dependencies(
            Map<String, VersionRange> mods,
            Map<String, VersionRange> packs
    ) implements Serializable {
        public Dependencies {
            Objects.requireNonNull(mods, "mods");
            Objects.requireNonNull(packs, "packs");
            mods = Collections.unmodifiableMap(new LinkedHashMap<>(mods));
            packs = Collections.unmodifiableMap(new LinkedHashMap<>(packs));
        }
    }
}
