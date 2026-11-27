package cn.kycraft.kuayue.packs.shapes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.geometry.Shape;
import lib.kasuga.geometry.Transform;
import lib.kasuga.registration.data_driven.shape.ShapeCatalog;
import lib.kasuga.registration.minecraft.block.shape.BlockShapePlan;
import lib.kasuga.registration.minecraft.block.shape.BlockStateKey;
import lib.kasuga.registration.minecraft.block.shape.BlockStateShapeBinding;
import lib.kasuga.registration.minecraft.block.shape.ShapeSelection;
import lib.kasuga.registration.minecraft.block.shape.ShapeSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * KuaYue's adapter from the data-driven block binding JSON to the immutable lib plan.
 *
 * <p>Bindings live beside the producing factory in {@code params.shape_bindings}. The adapter
 * expands partial state selectors against the actual state definition after the block object
 * exists, which keeps factory-owned properties such as {@code edit_type} out of vehicle resource
 * files.</p>
 */
public final class KuaYueShapeBindingSpec {
    static final String PARAMS_FIELD = "shape_bindings";
    private static final String BINDING_PATH = "params." + PARAMS_FIELD;
    private static final Set<String> ROOT_FIELDS = Set.of(
            "shape", "collision", "visual", "interaction", "support", "occlusion", "states"
    );
    private static final Set<String> STATE_FIELDS = Set.of(
            "when", "shape", "collision", "visual", "interaction", "support", "occlusion"
    );
    private static final Set<String> REF_FIELDS = Set.of("ref", "transform");
    private static final Set<String> TRANSFORM_FIELDS = Set.of(
            "scale", "rotate_y", "pivot", "translate"
    );
    private static final Set<String> ALIAS_FIELDS = Set.of("from");
    private static final Set<String> INLINE_FIELDS = Set.of("nodes", "output");
    public static final KuaYueShapeBindingSpec NONE =
            new KuaYueShapeBindingSpec(Map.of(), List.of());

    private final Map<ShapeSlot, ShapeSelection> defaults;
    private final List<StateRule> states;

    private KuaYueShapeBindingSpec(
            Map<ShapeSlot, ShapeSelection> defaults,
            List<StateRule> states
    ) {
        this.defaults = defaults;
        this.states = states;
    }

    public boolean isEmpty() {
        return defaults.isEmpty() && states.isEmpty();
    }

    public static KuaYueShapeBindingSpec parse(JsonElement value, ShapeCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        if (value == null || value.isJsonNull()) return NONE;
        JsonObject root = requireObject(value, BINDING_PATH);
        requireOnly(root, ROOT_FIELDS, BINDING_PATH);

        Map<ShapeSlot, ShapeSelection> defaults = parseSelections(root, BINDING_PATH, catalog);
        List<StateRule> states = new ArrayList<>();
        if (root.has("states")) {
            JsonElement stateValue = root.get("states");
            if (!stateValue.isJsonArray()) {
                throw invalid(BINDING_PATH + ".states", "must be an array");
            }
            int index = 0;
            for (JsonElement entry : stateValue.getAsJsonArray()) {
                states.add(parseStateRule(
                        entry,
                        BINDING_PATH + ".states[" + index + "]",
                        catalog
                ));
                index++;
            }
        }
        return defaults.isEmpty() && states.isEmpty()
                ? NONE
                : new KuaYueShapeBindingSpec(
                        Collections.unmodifiableMap(new EnumMap<>(defaults)),
                        List.copyOf(states)
                );
    }

    public BlockShapePlan buildPlan(Block block) {
        Objects.requireNonNull(block, "block");
        validateConditions(block);

        BlockShapePlan.Builder plan = BlockShapePlan.builder();
        List<BlockState> possibleStates = block.getStateDefinition().getPossibleStates();
        for (BlockState state : possibleStates) {
            Map<String, String> values = BlockStateKey.of(state).values();
            EnumMap<ShapeSlot, ShapeSelection> selections = resolveSelections(values, possibleStates.size());
            if (selections.isEmpty()) continue;

            plan.state(
                    new BlockStateKey(values),
                    new BlockStateShapeBinding(selections)
            );
        }
        return plan.build();
    }

    private EnumMap<ShapeSlot, ShapeSelection> resolveSelections(
            Map<String, String> values,
            int stateCount
    ) {
        EnumMap<ShapeSlot, ShapeSelection> result = new EnumMap<>(ShapeSlot.class);
        for (ShapeSlot slot : ShapeSlot.values()) {
            StateRule winner = null;
            int specificity = -1;
            for (StateRule rule : states) {
                if (!rule.selections().containsKey(slot) || !rule.matches(values)) continue;
                if (rule.when().size() > specificity) {
                    winner = rule;
                    specificity = rule.when().size();
                } else if (rule.when().size() == specificity) {
                    throw invalid(
                            BINDING_PATH + ".states",
                            "ambiguous " + slot.name().toLowerCase()
                                    + " rules for state " + values + " (state count " + stateCount + ")"
                    );
                }
            }
            ShapeSelection selection = winner == null
                    ? defaults.get(slot)
                    : winner.selections().get(slot);
            if (selection != null) result.put(slot, selection);
        }
        validateAliases(result);
        return result;
    }

    private void validateConditions(Block block) {
        Map<String, Set<String>> possible = new LinkedHashMap<>();
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            Set<String> values = new HashSet<>();
            for (Comparable<?> value : property.getPossibleValues()) {
                values.add(propertyValue(property, value));
            }
            possible.put(property.getName(), Set.copyOf(values));
        }
        for (StateRule rule : states) {
            for (Map.Entry<String, String> condition : rule.when().entrySet()) {
                Set<String> values = possible.get(condition.getKey());
                if (values == null) {
                    throw invalid(
                            BINDING_PATH + ".states",
                            "unknown block state property '" + condition.getKey() + "'"
                    );
                }
                if (!values.contains(condition.getValue())) {
                    throw invalid(
                            BINDING_PATH + ".states",
                            "invalid value '" + condition.getValue() + "' for state property '"
                                    + condition.getKey() + "'"
                    );
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValue(Property property, Comparable value) {
        return property.getName(value);
    }

    private static void validateAliases(Map<ShapeSlot, ShapeSelection> selections) {
        for (ShapeSlot slot : selections.keySet()) {
            resolveGeometry(selections, slot);
        }
    }

    private static Shape resolveGeometry(
            Map<ShapeSlot, ShapeSelection> selections,
            ShapeSlot wanted
    ) {
        return resolveGeometry(selections, wanted, new HashSet<>());
    }

    private static Shape resolveGeometry(
            Map<ShapeSlot, ShapeSelection> selections,
            ShapeSlot wanted,
            Set<ShapeSlot> visiting
    ) {
        if (!visiting.add(wanted)) {
            throw invalid(
                    BINDING_PATH,
                    "cyclic shape-slot alias involving " + wanted.name().toLowerCase()
            );
        }
        try {
            return switch (selections.get(wanted)) {
                case ShapeSelection.Geometry geometry -> geometry.shape();
                case ShapeSelection.Empty ignored -> Shape.empty();
                case ShapeSelection.Alias alias ->
                        resolveGeometry(selections, alias.target(), visiting);
                case ShapeSelection.Native ignored -> null;
                case null -> null;
                case ShapeSelection.Missing ignored -> null;
            };
        } finally {
            visiting.remove(wanted);
        }
    }

    private static StateRule parseStateRule(JsonElement value, String path, ShapeCatalog catalog) {
        JsonObject object = requireObject(value, path);
        requireOnly(object, STATE_FIELDS, path);
        if (!object.has("when")) {
            throw invalid(path, "requires when");
        }
        JsonObject when = requireObject(object.get("when"), path + ".when");
        if (when.isEmpty()) {
            throw invalid(path + ".when", "must contain at least one state property");
        }
        Map<String, String> conditions = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : when.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()
                    || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw invalid(
                        path + ".when." + entry.getKey(),
                        "must be a serialized string value"
                );
            }
            conditions.put(entry.getKey(), entry.getValue().getAsString());
        }
        return new StateRule(
                Map.copyOf(conditions),
                parseSelections(object, path, catalog)
        );
    }

    private static Map<ShapeSlot, ShapeSelection> parseSelections(
            JsonObject object,
            String path,
            ShapeCatalog catalog
    ) {
        EnumMap<ShapeSlot, ShapeSelection> result = new EnumMap<>(ShapeSlot.class);
        for (ShapeSlot slot : ShapeSlot.values()) {
            String name = slot.name().toLowerCase();
            if (object.has(name)) {
                result.put(
                        slot,
                        parseSelection(object.get(name), path + "." + name, catalog)
                );
            }
        }
        return result;
    }

    private static ShapeSelection parseSelection(
            JsonElement value,
            String path,
            ShapeCatalog catalog
    ) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return switch (value.getAsString()) {
                case "native" -> ShapeSelection.nativeBinding();
                case "empty" -> ShapeSelection.empty();
                default -> throw invalid(path, "must be native, empty, a ref, from, or inline graph");
            };
        }
        JsonObject object = requireObject(value, path);
        if (object.has("ref")) {
            requireOnly(object, REF_FIELDS, path);
            String id = requireString(object.get("ref"), path + ".ref");
            Shape shape = catalog.get(id);
            if (shape == null) {
                throw invalid(path + ".ref", "shape '" + id + "' is not present in the catalog");
            }
            if (object.has("transform")) {
                shape = shape.transform(parseTransform(object.get("transform"), path + ".transform"));
            }
            return ShapeSelection.geometry(shape);
        }
        if (object.has("from")) {
            requireOnly(object, ALIAS_FIELDS, path);
            String target = requireString(object.get("from"), path + ".from");
            try {
                return ShapeSelection.alias(ShapeSlot.valueOf(target.toUpperCase()));
            } catch (IllegalArgumentException failure) {
                throw invalid(path + ".from", "unknown shape slot '" + target + "'");
            }
        }
        if (object.has("nodes") || object.has("output")) {
            requireOnly(object, INLINE_FIELDS, path);
            throw invalid(path, "inline graphs are not used by KuaYue vehicle bindings");
        }
        throw invalid(path, "unrecognized binding object");
    }

    private static Transform parseTransform(JsonElement value, String path) {
        JsonObject object = requireObject(value, path);
        requireOnly(object, TRANSFORM_FIELDS, path);
        float[] scale = floats(object, "scale", path, new float[]{1f, 1f, 1f}, 3);
        int rotation = integer(object, "rotate_y", path, 0);
        float[] pivot = floats(object, "pivot", path, new float[]{8f, 0f, 8f}, 3);
        float[] translate = floats(object, "translate", path, new float[]{0f, 0f, 0f}, 3);
        return Transform.fromPixels(
                scale[0], scale[1], scale[2],
                rotation,
                pivot[0], pivot[1], pivot[2],
                translate[0], translate[1], translate[2]
        );
    }

    private static float[] floats(
            JsonObject object,
            String field,
            String path,
            float[] fallback,
            int length
    ) {
        if (!object.has(field)) return fallback;
        JsonArray array = requireArray(object.get(field), path + "." + field, length);
        float[] values = new float[length];
        for (int index = 0; index < length; index++) {
            JsonElement entry = array.get(index);
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isNumber()) {
                throw invalid(path + "." + field + "[" + index + "]", "must be a number");
            }
            values[index] = entry.getAsFloat();
            if (!Float.isFinite(values[index])) {
                throw invalid(path + "." + field + "[" + index + "]", "must be finite");
            }
        }
        return values;
    }

    private static int integer(JsonObject object, String field, String path, int fallback) {
        if (!object.has(field)) return fallback;
        JsonElement value = object.get(field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw invalid(path + "." + field, "must be an integer");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number)) {
            throw invalid(path + "." + field, "must be an integer");
        }
        return (int) number;
    }

    private static JsonObject requireObject(JsonElement value, String path) {
        if (value == null || !value.isJsonObject()) {
            throw invalid(path, "must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonElement value, String path, int length) {
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() != length) {
            throw invalid(path, "must be an array of " + length + " values");
        }
        return value.getAsJsonArray();
    }

    private static String requireString(JsonElement value, String path) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw invalid(path, "must be a non-empty string");
        }
        return value.getAsString();
    }

    private static void requireOnly(JsonObject object, Set<String> allowed, String path) {
        for (String field : object.keySet()) {
            if (!allowed.contains(field)) {
                throw invalid(path + "." + field, "is not allowed");
            }
        }
    }

    private static IllegalArgumentException invalid(String path, String message) {
        return new IllegalArgumentException(path + ": " + message);
    }

    private record StateRule(
            Map<String, String> when,
            Map<ShapeSlot, ShapeSelection> selections
    ) {
        private StateRule {
            when = Map.copyOf(when);
            selections = Collections.unmodifiableMap(new EnumMap<>(selections));
        }

        private boolean matches(Map<String, String> values) {
            for (Map.Entry<String, String> condition : when.entrySet()) {
                if (!condition.getValue().equals(values.get(condition.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
