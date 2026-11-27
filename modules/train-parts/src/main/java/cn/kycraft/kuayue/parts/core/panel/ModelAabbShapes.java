package cn.kycraft.kuayue.parts.core.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Model-derived axis-aligned bounding boxes for data-driven panels.
 *
 * <p>The {@code params.aabb} object maps a normalised state key (for example
 * {@code facing=north,hinge=left,open=false}) to either six pixel coordinates
 * {@code [x1,y1,z1,x2,y2,z2]} or an empty array for an empty shape. Keys are
 * generated from the blockstate variants of the source models, with each
 * variant's model boxes already rotated by the blockstate {@code y} value.
 *
 * <p>Vanilla exposes separate selection, interaction and collision queries.
 * KuaYue panel factories normally delegate collision to {@code getShape}, while
 * {@code getInteractionShape} keeps Minecraft's empty default unless a factory
 * overrides it explicitly.
 */
public final class ModelAabbShapes {
    /** Property order used both when generating data and when rebuilding the key at runtime. */
    private static final List<String> KEY_PROPERTIES = List.of("facing", "hinge", "open", "lit");

    public static final Map<String, VoxelShape> NONE = Map.of();

    private ModelAabbShapes() {
    }

    public static Map<String, VoxelShape> parse(JsonObject params) {
        if (params == null || !params.has("aabb") || !params.get("aabb").isJsonObject()) {
            return NONE;
        }
        Map<String, VoxelShape> shapes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : params.getAsJsonObject("aabb").entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonArray()) {
                continue;
            }
            String key = normalise(entry.getKey());
            JsonArray box = value.getAsJsonArray();
            if (box.isEmpty()) {
                shapes.put(key, Shapes.empty());
            } else if (box.size() == 6) {
                shapes.put(key, Block.box(
                        box.get(0).getAsDouble(),
                        box.get(1).getAsDouble(),
                        box.get(2).getAsDouble(),
                        box.get(3).getAsDouble(),
                        box.get(4).getAsDouble(),
                        box.get(5).getAsDouble()
                ));
            }
        }
        return shapes.isEmpty() ? NONE : Map.copyOf(shapes);
    }

    /**
     * @return the model AABB for this state, {@link Shapes#empty()} when the model has no
     * geometry, or {@code null} when this block has no model-derived shapes at all.
     */
    public static VoxelShape lookup(Map<String, VoxelShape> shapes, BlockState state) {
        if (shapes == null || shapes.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String name : KEY_PROPERTIES) {
            String value = propertyValue(state, name);
            if (value != null) {
                parts.add(name + "=" + value);
            }
        }
        // Blockstate variant keys may declare fewer properties than the live state
        // (for example a ladder only varies by facing). Fall back from the full
        // state key to shorter prefixes and finally to the model's default key.
        for (int size = parts.size(); size >= 0; size--) {
            VoxelShape shape = shapes.get(String.join(",", parts.subList(0, size)));
            if (shape != null) {
                return shape;
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
