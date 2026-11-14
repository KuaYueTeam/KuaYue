package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Strict K1 parcel.json representation.
 */
public record ParcelManifest(
        int formatVersion,
        String id,
        String nameKey,
        String descriptionKey,
        String icon,
        List<String> indexes
) implements Serializable {
    public ParcelManifest {
        if (formatVersion != 1) {
            throw new IllegalArgumentException("Unsupported parcel format: " + formatVersion);
        }
        if (!PackPathRules.isResourceLocation(id)) {
            throw new IllegalArgumentException("Invalid parcel ID: " + id);
        }
        if (nameKey == null || nameKey.isBlank()) {
            throw new IllegalArgumentException("name_key must not be empty");
        }
        if (descriptionKey != null && descriptionKey.isBlank()) {
            throw new IllegalArgumentException("description_key must not be empty");
        }
        indexes = List.copyOf(new ArrayList<>(indexes));
    }

    public static ParcelManifest parse(String json, Iterable<String> packNamespaces) {
        return new ParcelManifestParser().parse(json, packNamespaces);
    }

    public String namespace() {
        return Objects.requireNonNull(PackPathRules.namespaceOf(id));
    }
}
