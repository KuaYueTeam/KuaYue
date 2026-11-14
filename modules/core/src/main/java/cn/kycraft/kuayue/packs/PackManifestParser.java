package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PackManifestParser {
    private static final Set<String> PACK_KEYS = Set.of(
            "format_version", "id", "version", "namespaces", "name_key",
            "description_key", "dependencies", "parcels", "save_compatibility");
    private static final Set<String> REQUIRED_PACK_KEYS = Set.of(
            "format_version", "id", "version", "namespaces", "name_key",
            "dependencies", "parcels");
    private static final Set<String> DEPENDENCY_KEYS = Set.of("mods", "packs");

    PackManifest parse(String json, boolean allowCorePack) {
        JsonValue root;
        try {
            root = StrictJsonEngine.parseObject(json);
        } catch (IllegalArgumentException exception) {
            throw new PackParseException(
                    PackDiagnosticCodes.INVALID_MANIFEST, "",
                    "Invalid pack manifest JSON: " + exception.getMessage(), exception);
        }
        try {
            return parse(root, allowCorePack);
        } catch (PackParseException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new PackParseException(
                    PackDiagnosticCodes.INVALID_MANIFEST, "",
                    "Invalid pack manifest: " + exception.getMessage(), exception);
        }
    }

    private PackManifest parse(JsonValue root, boolean allowCorePack) {
        JsonProtocol.requireExactKeys(root, PACK_KEYS, "");
        JsonProtocol.requirePresent(root, REQUIRED_PACK_KEYS, "");

        int formatVersion = JsonProtocol.requireInteger(root, "format_version", "");
        if (formatVersion != PackManifest.FORMAT_VERSION) {
            throw new PackParseException(
                    PackDiagnosticCodes.UNSUPPORTED_FORMAT, "/format_version",
                    "Unsupported pack format_version: " + formatVersion);
        }
        String id = JsonProtocol.requireString(root, "id", "");
        if (!PackPathRules.isPackId(id)
                || (!allowCorePack && PackManifest.CORE_PACK_ID.equals(id))) {
            throw JsonProtocol.invalid("/id", "Invalid external pack ID: " + id);
        }
        SemanticVersion version;
        try {
            version = SemanticVersion.parse(JsonProtocol.requireString(root, "version", ""));
        } catch (IllegalArgumentException exception) {
            throw new PackParseException(
                    PackDiagnosticCodes.INVALID_MANIFEST, "/version",
                    "Invalid pack version: " + exception.getMessage(), exception);
        }

        List<String> namespaces = parseStringArray(root, "namespaces", "/namespaces",
                PackDiagnosticCodes.INVALID_NAMESPACE);
        if (namespaces.isEmpty()) {
            throw new PackParseException(
                    PackDiagnosticCodes.INVALID_NAMESPACE, "/namespaces",
                    "namespaces must not be empty");
        }
        Set<String> namespaceSet = new LinkedHashSet<>();
        for (int index = 0; index < namespaces.size(); index++) {
            String namespace = namespaces.get(index);
            if (!PackPathRules.isNamespace(namespace)) {
                throw new PackParseException(
                        PackDiagnosticCodes.INVALID_NAMESPACE,
                        "/namespaces/" + index,
                        "Invalid namespace: " + namespace);
            }
            if (!namespaceSet.add(namespace)) {
                throw new PackParseException(
                        PackDiagnosticCodes.INVALID_NAMESPACE,
                        "/namespaces/" + index,
                        "Duplicate namespace: " + namespace);
            }
        }

        String description = JsonProtocol.optionalString(root, "description_key", "");
        PackManifest.Dependencies dependencies = parseDependencies(
                JsonProtocol.requireObject(root, "dependencies", ""), id);
        List<String> parcels = parseStringArray(root, "parcels", "/parcels",
                PackDiagnosticCodes.INVALID_MANIFEST);
        Set<String> parcelSet = new LinkedHashSet<>();
        for (int index = 0; index < parcels.size(); index++) {
            String parcel = parcels.get(index);
            if (!PackPathRules.isParcelManifestPath(parcel)) {
                throw JsonProtocol.invalid(
                        "/parcels/" + index, "Invalid parcel manifest path: " + parcel);
            }
            if (!parcelSet.add(parcel)) {
                throw JsonProtocol.invalid(
                        "/parcels/" + index, "Duplicate parcel manifest path: " + parcel);
            }
        }
        if (parcels.isEmpty()) {
            throw JsonProtocol.invalid("/parcels", "parcels must not be empty");
        }

        SaveCompatibility compatibility = parseSaveCompatibility(root, version);
        return new PackManifest(
                formatVersion, id, version, namespaces,
                JsonProtocol.requireString(root, "name_key", ""),
                description, dependencies, parcels, compatibility);
    }

    private PackManifest.Dependencies parseDependencies(JsonValue root, String packId) {
        JsonProtocol.requireExactKeys(root, DEPENDENCY_KEYS, "/dependencies");
        JsonProtocol.requirePresent(root, DEPENDENCY_KEYS, "/dependencies");

        JsonValue modsObject = JsonProtocol.requireObject(root, "mods", "/dependencies");
        JsonProtocol.requireExactKeys(modsObject, PackManifest.HOST_MODS, "/dependencies/mods");
        JsonProtocol.requirePresent(modsObject, PackManifest.HOST_MODS, "/dependencies/mods");
        Map<String, VersionRange> mods = parseVersionRanges(
                modsObject, "/dependencies/mods");

        JsonValue packsObject = JsonProtocol.requireObject(root, "packs", "/dependencies");
        Map<String, VersionRange> packs = parseVersionRanges(
                packsObject, "/dependencies/packs");
        for (String dependencyId : packs.keySet()) {
            if (!PackPathRules.isPackId(dependencyId)) {
                throw JsonProtocol.invalid(
                        "/dependencies/packs/" + JsonProtocol.escape(dependencyId),
                        "Invalid pack dependency ID: " + dependencyId);
            }
            if (packId.equals(dependencyId)) {
                throw JsonProtocol.invalid(
                        "/dependencies/packs/" + JsonProtocol.escape(dependencyId),
                        "A pack cannot depend on itself");
            }
        }
        return new PackManifest.Dependencies(mods, packs);
    }

    private Map<String, VersionRange> parseVersionRanges(JsonValue root, String pointer) {
        Map<String, VersionRange> ranges = new LinkedHashMap<>();
        for (var entry : root.entries().entrySet()) {
            if (!entry.getValue().isString()) {
                throw JsonProtocol.invalid(
                        pointer + "/" + JsonProtocol.escape(entry.getKey()),
                        "Version range must be a string");
            }
            try {
                ranges.put(entry.getKey(), VersionRange.parse(entry.getValue().asString()));
            } catch (IllegalArgumentException exception) {
                throw new PackParseException(
                        PackDiagnosticCodes.INVALID_MANIFEST,
                        pointer + "/" + JsonProtocol.escape(entry.getKey()),
                        "Invalid version range: " + exception.getMessage(), exception);
            }
        }
        return Collections.unmodifiableMap(ranges);
    }

    private SaveCompatibility parseSaveCompatibility(
            JsonValue root, SemanticVersion currentVersion
    ) {
        if (!root.has("save_compatibility")) {
            return null;
        }
        JsonValue compatibility = JsonProtocol.requireObject(
                root, "save_compatibility", "");
        Set<String> allowed = Set.of("from_versions", "minimum_version");
        JsonProtocol.requireExactKeys(compatibility, allowed, "/save_compatibility");
        if (compatibility.size() != 1) {
            throw JsonProtocol.invalid(
                    "/save_compatibility",
                    "save_compatibility requires exactly one compatibility form");
        }
        if (compatibility.has("minimum_version")) {
            SemanticVersion minimum = parseVersionField(
                    compatibility, "minimum_version", "/save_compatibility");
            if (minimum.compareTo(currentVersion) >= 0) {
                throw JsonProtocol.invalid(
                        "/save_compatibility/minimum_version",
                        "minimum_version must be below the current pack version");
            }
            return new SaveCompatibility(null, minimum);
        }

        List<String> rawVersions = parseStringArray(
                compatibility, "from_versions", "/save_compatibility",
                PackDiagnosticCodes.INVALID_MANIFEST);
        if (rawVersions.isEmpty()) {
            throw JsonProtocol.invalid(
                    "/save_compatibility/from_versions",
                    "from_versions must not be empty");
        }
        List<SemanticVersion> versions = new ArrayList<>();
        Set<SemanticVersion> unique = new LinkedHashSet<>();
        for (int index = 0; index < rawVersions.size(); index++) {
            SemanticVersion historical;
            try {
                historical = SemanticVersion.parse(rawVersions.get(index));
            } catch (IllegalArgumentException exception) {
                throw new PackParseException(
                        PackDiagnosticCodes.INVALID_MANIFEST,
                        "/save_compatibility/from_versions/" + index,
                        "Invalid historical version: " + exception.getMessage(), exception);
            }
            if (historical.compareTo(currentVersion) >= 0) {
                throw JsonProtocol.invalid(
                        "/save_compatibility/from_versions/" + index,
                        "Historical version must be below the current pack version");
            }
            if (!unique.add(historical)) {
                throw JsonProtocol.invalid(
                        "/save_compatibility/from_versions/" + index,
                        "Duplicate historical version: " + historical);
            }
            versions.add(historical);
        }
        return new SaveCompatibility(versions, null);
    }

    private SemanticVersion parseVersionField(
            JsonValue object, String field, String parentPointer
    ) {
        try {
            return SemanticVersion.parse(JsonProtocol.requireString(
                    object, field, parentPointer));
        } catch (IllegalArgumentException exception) {
            throw new PackParseException(
                    PackDiagnosticCodes.INVALID_MANIFEST,
                    parentPointer + "/" + field,
                    "Invalid version: " + exception.getMessage(), exception);
        }
    }

    private List<String> parseStringArray(
            JsonValue root, String field, String pointer, String invalidCode
    ) {
        JsonValue array = JsonProtocol.requireArray(root, field, "");
        List<String> values = new ArrayList<>(array.size());
        Set<String> unique = new LinkedHashSet<>();
        for (int index = 0; index < array.size(); index++) {
            JsonValue element = array.elements().get(index);
            if (!element.isString() || element.asString().isEmpty()) {
                throw new PackParseException(
                        invalidCode, pointer + "/" + index,
                        "Array entry must be a non-empty string");
            }
            String value = element.asString();
            if (!unique.add(value)) {
                throw new PackParseException(
                        invalidCode, pointer + "/" + index,
                        "Duplicate array entry: " + value);
            }
            values.add(value);
        }
        return values;
    }
}
