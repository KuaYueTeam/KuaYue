package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ParcelManifestParser {
    private static final Set<String> KEYS = Set.of(
            "format_version", "id", "name_key", "description_key", "icon", "indexes");
    private static final Set<String> REQUIRED_KEYS =
            Set.of("format_version", "id", "name_key", "indexes");

    ParcelManifest parse(String json, Iterable<String> packNamespaces) {
        JsonValue root;
        try {
            root = StrictJsonEngine.parseObject(json);
        } catch (IllegalArgumentException exception) {
            throw new PackParseException(
                    PackDiagnosticCodes.INVALID_MANIFEST, "",
                    "Invalid parcel manifest JSON: " + exception.getMessage(), exception);
        }
        JsonProtocol.requireExactKeys(root, KEYS, "");
        JsonProtocol.requirePresent(root, REQUIRED_KEYS, "");

        int formatVersion = JsonProtocol.requireInteger(root, "format_version", "");
        if (formatVersion != 1) {
            throw new PackParseException(
                    PackDiagnosticCodes.UNSUPPORTED_FORMAT, "/format_version",
                    "Unsupported parcel format_version: " + formatVersion);
        }

        Set<String> namespaces = new LinkedHashSet<>();
        for (String namespace : packNamespaces) {
            namespaces.add(namespace);
        }
        String id = JsonProtocol.requireString(root, "id", "");
        if (!PackPathRules.isResourceLocation(id)) {
            throw JsonProtocol.invalid("/id", "Invalid parcel ID: " + id);
        }
        String parcelNamespace = PackPathRules.namespaceOf(id);
        if (!namespaces.contains(parcelNamespace)) {
            throw new PackParseException(
                    PackDiagnosticCodes.INVALID_NAMESPACE, "/id",
                    "Parcel ID namespace is not declared by the pack: " + parcelNamespace);
        }

        String description = JsonProtocol.optionalString(root, "description_key", "");
        String icon = null;
        if (root.has("icon")) {
            icon = JsonProtocol.requireString(root, "icon", "");
            if (!PackPathRules.isIconPath(icon)) {
                throw JsonProtocol.invalid(
                        "/icon", "Icon must be a namespaced textures/... image resource");
            }
            String iconNamespace = PackPathRules.namespaceOf(icon);
            if (!namespaces.contains(iconNamespace)) {
                throw new PackParseException(
                        PackDiagnosticCodes.INVALID_NAMESPACE, "/icon",
                        "Icon namespace is not declared by the pack: " + iconNamespace);
            }
        }

        JsonValue indexesArray = JsonProtocol.requireArray(root, "indexes", "");
        List<String> indexes = new ArrayList<>(indexesArray.size());
        Set<String> unique = new LinkedHashSet<>();
        for (int index = 0; index < indexesArray.size(); index++) {
            JsonValue element = indexesArray.elements().get(index);
            if (!element.isString() || element.asString().isEmpty()) {
                throw JsonProtocol.invalid(
                        "/indexes/" + index, "Index entry must be a non-empty string");
            }
            String indexPath = element.asString();
            if (!PackPathRules.isSafeRelativePath(indexPath)) {
                throw new PackParseException(
                        PackDiagnosticCodes.UNSAFE_PATH, "/indexes/" + index,
                        "Unsafe index path: " + indexPath);
            }
            String namespace = indexNamespace(indexPath);
            if (namespace == null || !namespaces.contains(namespace)
                    || !PackPathRules.isKasugaIndexPath(indexPath, namespace)) {
                throw JsonProtocol.invalid(
                        "/indexes/" + index,
                        "Index must be under data/<declared-namespace>/kasuga_lib/data_driven/");
            }
            if (!unique.add(indexPath)) {
                throw JsonProtocol.invalid(
                        "/indexes/" + index, "Duplicate index path: " + indexPath);
            }
            indexes.add(indexPath);
        }

        return new ParcelManifest(
                1, id, JsonProtocol.requireString(root, "name_key", ""),
                description, icon, indexes);
    }

    private String indexNamespace(String path) {
        String prefix = "data/";
        String marker = "/kasuga_lib/data_driven/";
        if (!path.startsWith(prefix)) {
            return null;
        }
        int markerIndex = path.indexOf(marker, prefix.length());
        if (markerIndex <= prefix.length()) {
            return null;
        }
        String namespace = path.substring(prefix.length(), markerIndex);
        return namespace.indexOf('/') >= 0 ? null : namespace;
    }
}
