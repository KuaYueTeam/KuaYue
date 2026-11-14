package cn.kycraft.kuayue.packs;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

final class JsonProtocol {
    private JsonProtocol() {
    }

    static JsonValue requireObject(JsonValue parent, String name, String pointer) {
        JsonValue element = parent.get(name);
        if (element == null || !element.isObject()) {
            throw invalid(pointer + "/" + name, "Field must be an object: " + name);
        }
        return element;
    }

    static JsonValue requireArray(JsonValue parent, String name, String pointer) {
        JsonValue element = parent.get(name);
        if (element == null || !element.isArray()) {
            throw invalid(pointer + "/" + name, "Field must be an array: " + name);
        }
        return element;
    }

    static String requireString(JsonValue parent, String name, String pointer) {
        JsonValue element = parent.get(name);
        if (element == null || !element.isString() || element.asString().isEmpty()) {
            throw invalid(pointer + "/" + name, "Field must be a non-empty string: " + name);
        }
        return element.asString();
    }

    static String optionalString(JsonValue parent, String name, String pointer) {
        if (!parent.has(name)) {
            return null;
        }
        return requireString(parent, name, pointer);
    }

    static int requireInteger(JsonValue parent, String name, String pointer) {
        JsonValue element = parent.get(name);
        if (element == null || !element.isNumber()) {
            throw invalid(pointer + "/" + name, "Field must be an integer: " + name);
        }
        BigDecimal number = element.asNumber();
        try {
            return number.stripTrailingZeros().scale() > 0
                    ? throwInvalid(pointer + "/" + name, "Field must be an integer: " + name)
                    : number.intValueExact();
        } catch (ArithmeticException exception) {
            throw invalid(pointer + "/" + name, "Integer is outside the supported range: " + name);
        }
    }

    static void requireExactKeys(JsonValue object, Set<String> allowed, String pointer) {
        for (String key : object.entries().keySet()) {
            if (!allowed.contains(key)) {
                throw invalid(pointer + "/" + escape(key), "Unknown field: " + key);
            }
        }
    }

    static void requirePresent(JsonValue object, Set<String> required, String pointer) {
        for (String key : required) {
            if (!object.has(key)) {
                throw invalid(pointer + "/" + escape(key), "Missing required field: " + key);
            }
        }
    }

    static void requireUniqueStrings(
            Iterable<String> values, String code, String pointer, String description
    ) {
        Set<String> seen = new HashSet<>();
        for (String value : values) {
            if (!seen.add(value)) {
                throw new PackParseException(
                        code, pointer, "Duplicate " + description + ": " + value);
            }
        }
    }

    static PackParseException invalid(String pointer, String message) {
        return new PackParseException(PackDiagnosticCodes.INVALID_MANIFEST, pointer, message);
    }

    private static int throwInvalid(String pointer, String message) {
        throw invalid(pointer, message);
    }

    static String escape(String field) {
        return field.replace("~", "~0").replace("/", "~1");
    }
}
