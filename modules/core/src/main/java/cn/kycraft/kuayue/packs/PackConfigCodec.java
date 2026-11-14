package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PackConfigCodec {
    private static final Set<String> PACK_CONFIG_KEYS =
            Set.of("format_version", "enabled", "order", "inventory");
    private static final Set<String> REQUIRED_KEYS = Set.of("format_version");

    private PackConfigCodec() {
    }

    static PackConfigStore.LoadResult parse(String json) {
        JsonValue root = StrictJsonEngine.parseObject(json);
        if (!root.has("packs")) {
            return new PackConfigStore.LoadResult(
                    new PackConfigDocument(json, PackConfig.defaults()), List.of());
        }
        JsonValue packsElement = root.get("packs");
        if (!packsElement.isObject()) {
            throw new PackParseException(
                    PackDiagnosticCodes.CONFIG_INVALID, "/packs",
                    "packs must be an object");
        }
        JsonValue packsObject = packsElement;
        JsonProtocol.requireExactKeys(packsObject, PACK_CONFIG_KEYS, "/packs");
        JsonProtocol.requirePresent(packsObject, REQUIRED_KEYS, "/packs");
        int formatVersion = JsonProtocol.requireInteger(
                packsObject, "format_version", "/packs");
        if (formatVersion != PackConfig.FORMAT_VERSION) {
            throw new PackParseException(
                    PackDiagnosticCodes.UNSUPPORTED_FORMAT,
                    "/packs/format_version",
                    "Unsupported packs.format_version: " + formatVersion);
        }

        Map<String, Boolean> enabled = parseEnabled(packsObject);
        List<String> order = parseIdArray(packsObject, "order", "/packs/order");
        List<String> inventory = parseIdArray(
                packsObject, "inventory", "/packs/inventory");
        PackConfig config = new PackConfig(formatVersion, enabled, order, inventory);
        return new PackConfigStore.LoadResult(
                new PackConfigDocument(json, config), List.of());
    }

    static String render(PackConfigDocument document) {
        JsonValue root = StrictJsonEngine.parseObject(document.originalRootJson());
        PackConfig config = document.config();
        JsonValue packs = JsonValue.object();
        packs.put("format_version", config.formatVersion());

        JsonValue enabled = JsonValue.object();
        config.enabled().forEach(enabled::put);
        packs.put("enabled", enabled);

        JsonValue order = JsonValue.array();
        config.order().forEach(order::add);
        packs.put("order", order);

        JsonValue inventory = JsonValue.array();
        config.inventory().forEach(inventory::add);
        packs.put("inventory", inventory);

        root.put("packs", packs);
        return StrictJsonEngine.toJson(root);
    }

    private static Map<String, Boolean> parseEnabled(JsonValue packs) {
        if (!packs.has("enabled")) {
            return Map.of();
        }
        JsonValue element = packs.get("enabled");
        if (!element.isObject()) {
            throw new PackParseException(
                    PackDiagnosticCodes.CONFIG_INVALID, "/packs/enabled",
                    "packs.enabled must be an object");
        }
        LinkedHashMap<String, Boolean> enabled = new LinkedHashMap<>();
        for (var entry : element.entries().entrySet()) {
            if (!PackPathRules.isPackId(entry.getKey())) {
                throw new PackParseException(
                        PackDiagnosticCodes.CONFIG_INVALID,
                        "/packs/enabled/" + JsonProtocol.escape(entry.getKey()),
                        "Invalid pack ID in packs.enabled: " + entry.getKey());
            }
            if (!entry.getValue().isBoolean()) {
                throw new PackParseException(
                        PackDiagnosticCodes.CONFIG_INVALID,
                        "/packs/enabled/" + JsonProtocol.escape(entry.getKey()),
                        "packs.enabled values must be booleans");
            }
            enabled.put(entry.getKey(), entry.getValue().asBoolean());
        }
        return Collections.unmodifiableMap(enabled);
    }

    private static List<String> parseIdArray(JsonValue parent, String field, String pointer) {
        if (!parent.has(field)) {
            return List.of();
        }
        JsonValue element = parent.get(field);
        if (!element.isArray()) {
            throw new PackParseException(
                    PackDiagnosticCodes.CONFIG_INVALID, pointer,
                    pointer + " must be an array");
        }
        JsonValue array = element;
        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            JsonValue child = array.elements().get(index);
            if (!child.isString()) {
                throw new PackParseException(
                        PackDiagnosticCodes.CONFIG_INVALID, pointer + "/" + index,
                        "Pack IDs must be strings");
            }
            String id = child.asString();
            if (!PackPathRules.isPackId(id)) {
                throw new PackParseException(
                        PackDiagnosticCodes.CONFIG_INVALID, pointer + "/" + index,
                        "Invalid pack ID: " + id);
            }
            values.add(id);
        }
        return values;
    }
}
