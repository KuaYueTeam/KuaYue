package cn.kycraft.kuayue.content.tech_tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * One blueprint-cover definition, parsed from {@code data/<ns>/blueprint_cover/*.json}.
 *
 * <p>Schema unchanged from 1.0 {@code BlueprintCover}: a cover is an optional background image, an optional
 * title image, an optional button background and an optional description text, each positioned in the
 * cover's own 242x128 pixel space.</p>
 */
record BlueprintCover(ResourceLocation id, boolean isDefault,
                      @Nullable CoverImage background,
                      @Nullable CoverImage title,
                      @Nullable CoverImage button,
                      @Nullable String descriptionKey) {

    /**
     * One image layer of the cover.
     *
     * @param location texture path without the {@code textures/} prefix or {@code .png} suffix
     * @param u0       UV rect in texture space, 0..1
     * @param x        placement in cover pixels; {@code centeredX}/{@code centeredY} mean "measure from
     *                 the text side", matching 1.0's {@code horizontal_centered} / {@code vertical_centered}
     * @param size     drawn size in cover pixels; 0 means "native texture size"
     */
    record CoverImage(ResourceLocation location,
                      float u0, float v0, float u1, float v1,
                      int x, int y, boolean centeredX, boolean centeredY, boolean positionIsCenter,
                      int width, int height) {
    }

    static BlueprintCover parse(JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(json.get("id").getAsString());
        boolean isDefault = json.has("is_default") && json.get("is_default").getAsBoolean();
        return new BlueprintCover(id, isDefault,
                image(json, "background"),
                image(json, "title"),
                image(json, "button_background"),
                description(json));
    }

    private static @Nullable CoverImage image(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has("location")) return null;

        float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;
        JsonElement uv = obj.get("uv");
        if (uv != null && uv.isJsonObject()) {
            JsonObject u = uv.getAsJsonObject();
            u0 = number(u, "u0", 0f);
            v0 = number(u, "v0", 0f);
            u1 = number(u, "u1", 1f);
            v1 = number(u, "v1", 1f);
        } else if (uv != null && uv.isJsonArray()) {
            JsonArray a = uv.getAsJsonArray();
            if (a.size() >= 4) {
                u0 = a.get(0).getAsFloat();
                v0 = a.get(1).getAsFloat();
                u1 = a.get(2).getAsFloat();
                v1 = a.get(3).getAsFloat();
            }
        }

        int x = 0, y = 0;
        boolean centeredX = false, centeredY = false, positionIsCenter = false;
        JsonElement pos = obj.get("pos");
        if (pos != null && pos.isJsonObject()) {
            JsonObject p = pos.getAsJsonObject();
            centeredX = flag(p, "horizontal_centered");
            centeredY = flag(p, "vertical_centered");
            positionIsCenter = flag(p, "position_is_center");
            JsonElement vec = p.get("vec");
            if (vec != null && vec.isJsonArray()) {
                JsonArray a = vec.getAsJsonArray();
                if (a.size() >= 2) {
                    x = a.get(0).getAsInt();
                    y = a.get(1).getAsInt();
                }
            }
        }

        int width = 0, height = 0;
        JsonElement size = obj.get("size");
        if (size != null && size.isJsonObject()) {
            JsonObject s = size.getAsJsonObject();
            width = s.has("width") ? s.get("width").getAsInt() : 0;
            height = s.has("height") ? s.get("height").getAsInt() : 0;
        }

        return new CoverImage(ResourceLocation.parse(obj.get("location").getAsString()),
                u0, v0, u1, v1, x, y, centeredX, centeredY, positionIsCenter, width, height);
    }

    private static @Nullable String description(JsonObject json) {
        JsonElement element = json.get("description");
        if (element == null || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        return obj.has("textKey") ? obj.get("textKey").getAsString() : null;
    }

    private static float number(JsonObject obj, String key, float fallback) {
        JsonElement e = obj.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsFloat();
    }

    private static boolean flag(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }
}
