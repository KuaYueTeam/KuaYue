package cn.kycraft.kuayue.packs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Small package-private JSON tree used to keep the K1 protocol independent of optional libraries.
 */
final class JsonValue {
    enum Kind {
        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }

    private final Kind kind;
    private final Map<String, JsonValue> object;
    private final List<JsonValue> array;
    private final String string;
    private final BigDecimal number;
    private final boolean bool;

    private JsonValue(
            Kind kind,
            Map<String, JsonValue> object,
            List<JsonValue> array,
            String string,
            BigDecimal number,
            boolean bool
    ) {
        this.kind = kind;
        this.object = object;
        this.array = array;
        this.string = string;
        this.number = number;
        this.bool = bool;
    }

    static JsonValue object() {
        return new JsonValue(Kind.OBJECT, new LinkedHashMap<>(), null, null, null, false);
    }

    static JsonValue array() {
        return new JsonValue(Kind.ARRAY, null, new ArrayList<>(), null, null, false);
    }

    static JsonValue string(String value) {
        return new JsonValue(Kind.STRING, null, null, Objects.requireNonNull(value), null, false);
    }

    static JsonValue number(BigDecimal value) {
        return new JsonValue(Kind.NUMBER, null, null, null, Objects.requireNonNull(value), false);
    }

    static JsonValue bool(boolean value) {
        return new JsonValue(Kind.BOOLEAN, null, null, null, null, value);
    }

    static JsonValue nullValue() {
        return new JsonValue(Kind.NULL, null, null, null, null, false);
    }

    JsonValue put(String name, JsonValue value) {
        require(Kind.OBJECT);
        object.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
        return this;
    }

    JsonValue put(String name, String value) {
        return put(name, value == null ? nullValue() : string(value));
    }

    JsonValue put(String name, boolean value) {
        return put(name, bool(value));
    }

    JsonValue put(String name, int value) {
        return put(name, number(BigDecimal.valueOf(value)));
    }

    JsonValue add(JsonValue value) {
        require(Kind.ARRAY);
        array.add(Objects.requireNonNull(value));
        return this;
    }

    JsonValue add(String value) {
        return add(string(value));
    }

    boolean isObject() {
        return kind == Kind.OBJECT;
    }

    boolean isArray() {
        return kind == Kind.ARRAY;
    }

    boolean isString() {
        return kind == Kind.STRING;
    }

    boolean isNumber() {
        return kind == Kind.NUMBER;
    }

    boolean isBoolean() {
        return kind == Kind.BOOLEAN;
    }

    boolean isNull() {
        return kind == Kind.NULL;
    }

    boolean has(String name) {
        require(Kind.OBJECT);
        return object.containsKey(name);
    }

    JsonValue get(String name) {
        require(Kind.OBJECT);
        return object.get(name);
    }

    int size() {
        return switch (kind) {
            case OBJECT -> object.size();
            case ARRAY -> array.size();
            default -> throw new IllegalStateException("Value has no size: " + kind);
        };
    }

    Map<String, JsonValue> entries() {
        require(Kind.OBJECT);
        return Collections.unmodifiableMap(object);
    }

    List<JsonValue> elements() {
        require(Kind.ARRAY);
        return Collections.unmodifiableList(array);
    }

    String asString() {
        require(Kind.STRING);
        return string;
    }

    BigDecimal asNumber() {
        require(Kind.NUMBER);
        return number;
    }

    boolean asBoolean() {
        require(Kind.BOOLEAN);
        return bool;
    }

    private void require(Kind expected) {
        if (kind != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + kind);
        }
    }
}
