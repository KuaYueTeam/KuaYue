package cn.kycraft.kuayue.parts.core.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.geometry.Box;
import lib.kasuga.geometry.Shape;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import lib.kasuga.registration.minecraft.block.shape.BlockShapePlan;
import lib.kasuga.registration.minecraft.block.shape.BlockStateKey;
import lib.kasuga.registration.minecraft.block.shape.ShapeSelection;
import lib.kasuga.registration.minecraft.block.shape.ShapeSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Installs KasugaLib compiled shape bindings from {@code params.aabb}.
 *
 * <p>{@code params.aabb} maps a normalised state key (for example
 * {@code facing=north,hinge=left,open=false}) to either six pixel coordinates
 * {@code [x1,y1,z1,x2,y2,z2]} or an empty array for an empty shape. Keys are
 * generated from the blockstate variants of the source models, with each
 * variant's model boxes already rotated by the blockstate {@code y} value.
 *
 * <p>The library mixin resolves SHAPE, COLLISION, VISUAL and INTERACTION from the
 * installed plan before falling back to the native block, so no block subclass
 * needs to override {@code getShape} or {@code getCollisionShape}. States without
 * an entry keep the factory's original shape.
 */
public final class PanelShapeBindings {
    /** Property order used both when generating data and when rebuilding the key at runtime. */
    private static final List<String> KEY_PROPERTIES = List.of("facing", "hinge", "open", "lit");

    private PanelShapeBindings() {
    }

    public static void install(Block block, JsonObject params) {
        Map<String, List<double[]>> boxes = parse(params);
        if (boxes.isEmpty()) {
            return;
        }
        // Optional second map: parts whose open pose should be walk-through (doors) still need a
        // clickable outline, so the selection/outline slots can keep using the closed profile.
        Map<String, List<double[]>> interaction = parse(params, "interaction_aabb");
        BlockShapePlan.Builder builder = BlockShapePlan.builder();
        Map<String, Shape> geometries = new LinkedHashMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            List<double[]> stateBoxes = resolve(boxes, state);
            if (stateBoxes == null) {
                continue;
            }
            // A shared geometry selection aliased into every query slot keeps the
            // visible shape, selection outline, interaction box and collision box identical.
            ShapeSelection selection = selectionOf(stateBoxes, geometries);
            ShapeSelection outline = interaction.isEmpty()
                    ? ShapeSelection.alias(ShapeSlot.SHAPE)
                    : selectionOf(resolve(interaction, state), geometries);
            Map<ShapeSlot, ShapeSelection> selections = new EnumMap<>(ShapeSlot.class);
            selections.put(ShapeSlot.SHAPE, selection);
            selections.put(ShapeSlot.COLLISION, ShapeSelection.alias(ShapeSlot.SHAPE));
            selections.put(ShapeSlot.VISUAL, outline);
            selections.put(ShapeSlot.INTERACTION, outline);
            builder.state(BlockStateKey.of(state), selections);
        }
        BlockShapePlan plan = builder.build();
        if (!plan.isEmpty()) {
            BlockShapeBindings.install(block, plan);
        }
    }

    /** Shared geometry cache keyed by the box signature; an empty or missing list means no shape. */
    private static ShapeSelection selectionOf(List<double[]> stateBoxes, Map<String, Shape> geometries) {
        if (stateBoxes == null || stateBoxes.isEmpty()) {
            return ShapeSelection.empty();
        }
        StringBuilder signature = new StringBuilder();
        for (double[] box : stateBoxes) {
            for (double value : box) {
                signature.append(value).append('/');
            }
            signature.append(';');
        }
        Shape shape = geometries.computeIfAbsent(signature.toString(), ignored -> {
            List<Box> geometry = new ArrayList<>(stateBoxes.size());
            for (double[] box : stateBoxes) {
                geometry.add(Box.fromPixels(box[0], box[1], box[2], box[3], box[4], box[5]));
            }
            return Shape.of(geometry);
        });
        return ShapeSelection.geometry(shape);
    }

    /**
     * Reads {@code params.aabb}. A value may be an empty array (empty shape), six numbers
     * (single box) or an array of six-number arrays (a union of boxes, used to keep holes).
     */
    private static Map<String, List<double[]>> parse(JsonObject params) {
        return parse(params, "aabb");
    }

    /** Same reader for a named map, e.g. the optional {@code interaction_aabb} overlay. */
    private static Map<String, List<double[]>> parse(JsonObject params, String field) {
        if (params == null || !params.has(field) || !params.get(field).isJsonObject()) {
            return Map.of();
        }
        Map<String, List<double[]>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : params.getAsJsonObject(field).entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            JsonArray array = entry.getValue().getAsJsonArray();
            List<double[]> boxes = new ArrayList<>();
            if (array.isEmpty()) {
                result.put(normalise(entry.getKey()), boxes);
            } else if (isNumberArray(array)) {
                boxes.add(readBox(array));
                result.put(normalise(entry.getKey()), boxes);
            } else {
                boolean valid = true;
                for (JsonElement element : array) {
                    if (!element.isJsonArray() || !isNumberArray(element.getAsJsonArray())) {
                        valid = false;
                        break;
                    }
                    boxes.add(readBox(element.getAsJsonArray()));
                }
                if (valid && !boxes.isEmpty()) {
                    result.put(normalise(entry.getKey()), boxes);
                }
            }
        }
        return result;
    }

    private static boolean isNumberArray(JsonArray array) {
        if (array.size() != 6) {
            return false;
        }
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                return false;
            }
        }
        return true;
    }

    private static double[] readBox(JsonArray array) {
        double[] box = new double[6];
        for (int i = 0; i < 6; i++) {
            box[i] = array.get(i).getAsDouble();
        }
        return box;
    }

    /**
     * Blockstate variant keys may declare fewer properties than the live state
     * (for example a ladder only varies by facing). Fall back from the full state
     * key to shorter prefixes and finally to the model's default key.
     */
    private static List<double[]> resolve(Map<String, List<double[]>> boxes, BlockState state) {
        List<String> parts = new ArrayList<>();
        for (String name : KEY_PROPERTIES) {
            String value = propertyValue(state, name);
            if (value != null) {
                parts.add(name + "=" + value);
            }
        }
        for (int size = parts.size(); size >= 0; size--) {
            List<double[]> result = boxes.get(String.join(",", parts.subList(0, size)));
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValue(BlockState state, String name) {
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            Property<?> property = entry.getKey();
            if (property.getName().equals(name)) {
                return ((Property) property).getName((Comparable) entry.getValue());
            }
        }
        return null;
    }

    private static String normalise(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        Map<String, String> pairs = new LinkedHashMap<>();
        for (String part : key.split(",")) {
            int separator = part.indexOf('=');
            if (separator > 0) {
                pairs.put(part.substring(0, separator).trim(), part.substring(separator + 1).trim());
            }
        }
        StringBuilder result = new StringBuilder();
        for (String name : KEY_PROPERTIES) {
            if (!pairs.containsKey(name)) {
                continue;
            }
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(name).append('=').append(pairs.get(name));
        }
        return result.toString();
    }
}
