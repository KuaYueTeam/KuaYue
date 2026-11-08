package cn.kycraft.kuayue.content.techtree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raw reader for the NeoKuayue tech tree JSON format.
 *
 * <p>The schema is intentionally untouched so existing {@code carriage_blueprint_group.json} /
 * {@code overhead_line_blueprint_group.json} files keep working; only the runtime representation,
 * the wire format and the UI are new.</p>
 */
final class TechTreeJson {

    private TechTreeJson() {
    }

    static String version(JsonObject json) {
        return str(json, "version", "");
    }

    static ResourceLocation id(JsonObject json) {
        JsonElement id = json.get("id");
        if (id == null || !id.isJsonPrimitive()) {
            throw new IllegalArgumentException("tech tree group has no id");
        }
        return ResourceLocation.parse(id.getAsString());
    }

    static String str(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }

    static JsonArray array(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    static JsonObject object(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    static List<String> strings(JsonObject json, String key) {
        List<String> out = new ArrayList<>();
        for (JsonElement child : array(json, key)) {
            out.add(child.getAsString());
        }
        return out;
    }

    /** {@code {"item": count}} pairs, used for cost / reward / blueprint blocks. */
    static Map<String, Integer> itemCounts(JsonObject json, String key) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object(json, key).entrySet()) {
            JsonElement count = entry.getValue();
            out.put(entry.getKey(), count != null && count.isJsonPrimitive() ? count.getAsInt() : 1);
        }
        return out;
    }
}
