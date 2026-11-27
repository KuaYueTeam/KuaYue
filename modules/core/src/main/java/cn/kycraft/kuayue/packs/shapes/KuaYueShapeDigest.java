package cn.kycraft.kuayue.packs.shapes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lib.kasuga.geometry.Box;
import lib.kasuga.registration.data_driven.handler.BlockDef;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.shape.ShapeCatalog;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Deterministic shape-and-binding digest used by the existing configuration packet gate.
 *
 * <p>Catalog definitions and block {@code params.shape_bindings} both contribute.</p>
 */
public final class KuaYueShapeDigest {
    private static final AtomicReference<String> CURRENT =
            new AtomicReference<>("none");
    private static final Gson GSON = new Gson();

    private KuaYueShapeDigest() {
    }

    public static String current() {
        return CURRENT.get();
    }

    public static void install(
            ShapeCatalog catalog,
            List<LocatedDefinition<?>> definitions
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (catalog != null) {
                List<ShapeCatalog.Entry> entries = new ArrayList<>(catalog.entries().values());
                entries.sort(Comparator.comparing(ShapeCatalog.Entry::id));
                for (ShapeCatalog.Entry entry : entries) {
                    text(digest, "shape");
                    text(digest, entry.id());
                    text(digest, entry.output());
                    for (Box box : entry.shape().boxes()) {
                        for (float coordinate : new float[]{
                                box.minX(), box.minY(), box.minZ(),
                                box.maxX(), box.maxY(), box.maxZ()
                        }) {
                            digest.update(ByteBuffer.allocate(Float.BYTES)
                                    .putInt(Float.floatToIntBits(
                                            coordinate == 0.0f ? 0.0f : coordinate
                                    ))
                                    .array());
                        }
                    }
                }
            }

            List<String> bindings = definitions.stream()
                    .map(LocatedDefinition::definition)
                    .filter(BlockDef.class::isInstance)
                    .map(BlockDef.class::cast)
                    .filter(definition -> definition.params() != null
                            && definition.params().has(KuaYueShapeBindingSpec.PARAMS_FIELD))
                    .map(definition -> {
                        String binding = canonical(definition.params().get(
                                KuaYueShapeBindingSpec.PARAMS_FIELD
                        ));
                        if (definition.params().has("multi_cell")) {
                            binding += "\u0000coverage\u0000"
                                    + canonical(definition.params().get("multi_cell"));
                        }
                        return new BindingEntry(definition.id(), binding);
                    })
                    .sorted(Comparator.comparing(BindingEntry::id))
                    .map(entry -> entry.id() + "\u0000" + entry.binding())
                    .toList();
            for (String binding : bindings) {
                text(digest, "binding");
                text(digest, binding);
            }
            CURRENT.set(HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static void resetForTests() {
        CURRENT.set("none");
    }

    private static String canonical(JsonElement value) {
        StringBuilder result = new StringBuilder();
        appendCanonical(value, result);
        return result.toString();
    }

    private static void appendCanonical(JsonElement value, StringBuilder output) {
        if (value == null || value.isJsonNull()) {
            output.append("null");
        } else if (value.isJsonObject()) {
            output.append('{');
            List<Map.Entry<String, JsonElement>> entries =
                    new ArrayList<>(value.getAsJsonObject().entrySet());
            entries.sort(Map.Entry.comparingByKey());
            boolean first = true;
            for (Map.Entry<String, JsonElement> entry : entries) {
                if (!first) output.append(',');
                first = false;
                output.append(GSON.toJson(entry.getKey())).append(':');
                appendCanonical(entry.getValue(), output);
            }
            output.append('}');
        } else if (value.isJsonArray()) {
            output.append('[');
            JsonArray array = value.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                if (index > 0) output.append(',');
                appendCanonical(array.get(index), output);
            }
            output.append(']');
        } else if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                BigDecimal number = primitive.getAsBigDecimal().stripTrailingZeros();
                output.append(number.signum() == 0 ? "0" : number.toPlainString());
            } else if (primitive.isBoolean()) {
                output.append(primitive.getAsBoolean());
            } else {
                output.append(GSON.toJson(primitive.getAsString()));
            }
        }
    }

    private static void text(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private record BindingEntry(String id, String binding) {
    }
}
