package cn.kycraft.kuayue.train;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parses one train patch while preserving field presence for later recursive overlay.
 */
public final class TrainPatchParser {
    private static final Set<String> ROOT_FIELDS = Set.of(
            TrainDataDrivenTypes.ID,
            TrainDataDrivenTypes.CATEGORY,
            TrainDataDrivenTypes.NAME_KEY,
            TrainDataDrivenTypes.DESCRIPTION_KEY,
            TrainDataDrivenTypes.MEMBERS,
            TrainDataDrivenTypes.CREATIVE
    );
    private static final Set<String> MEMBER_FIELDS = Set.of(
            TrainDataDrivenTypes.BLOCKS,
            TrainDataDrivenTypes.ITEMS,
            TrainDataDrivenTypes.ITEM_TAGS,
            TrainDataDrivenTypes.BOGEY_SIZES,
            TrainDataDrivenTypes.BOGEY_MOTIONS,
            TrainDataDrivenTypes.BOGEY_MODELS,
            TrainDataDrivenTypes.BOGEY_BLOCKS,
            TrainDataDrivenTypes.BOGEY_STYLES,
            TrainDataDrivenTypes.BOGEY_STYLE_VARIANTS
    );
    private static final Set<String> CREATIVE_FIELDS = Set.of(
            TrainDataDrivenTypes.TAB,
            TrainDataDrivenTypes.ICON_ITEM,
            TrainDataDrivenTypes.FILTER_ORDER
    );

    public TrainPatch parse(JsonObject input) {
        Objects.requireNonNull(input, "input");
        JsonObject root = input.deepCopy();
        requireAllowedFields(root, ROOT_FIELDS, "");
        requireOptionalObject(root, TrainDataDrivenTypes.MEMBERS, MEMBER_FIELDS);
        requireOptionalObject(root, TrainDataDrivenTypes.CREATIVE, CREATIVE_FIELDS);
        JsonElement idElement = root.remove(TrainDataDrivenTypes.ID);
        String id = requireExplicitId(idElement, "/id");

        JsonObject normalized = normalizeObject(root, "").getAsJsonObject();
        return new TrainPatch(id, normalized);
    }

    static ResourceLocation parseExplicitId(String value, String pointer) {
        if (value == null || value.isBlank()) {
            throw invalid(pointer, "expected a non-blank namespaced resource id");
        }
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw invalid(pointer, "resource id must use an explicit namespace:path form: " + value);
        }
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException exception) {
            throw invalid(pointer, "invalid resource id '" + value + "': " + exception.getMessage());
        }
    }

    private static String requireExplicitId(JsonElement element, String pointer) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw invalid(pointer, "expected a namespaced string resource id");
        }
        return parseExplicitId(element.getAsString(), pointer).toString();
    }

    private static JsonElement normalize(
            JsonElement element,
            String pointer,
            String parentField
    ) {
        if (element == null || element.isJsonNull()) {
            return element == null ? null : element.deepCopy();
        }
        if (element.isJsonObject()) {
            return normalizeObject(element.getAsJsonObject(), pointer);
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            Map<String, JsonElement> seen = new LinkedHashMap<>();
            int index = 0;
            for (JsonElement child : element.getAsJsonArray()) {
                JsonElement normalized = normalize(child, pointer + "/" + index, parentField);
                String identity = normalized == null ? "null" : normalized.toString();
                if (seen.containsKey(identity)) {
                    throw invalid(
                            pointer + "/" + index,
                            "duplicate array entry"
                    );
                }
                seen.put(identity, normalized);
                result.add(normalized);
                index++;
            }
            if (parentField != null && TrainDataDrivenTypes.isResourceListField(parentField)) {
                validateResourceList(result, pointer);
            }
            return result;
        }
        if (element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()
                && isResourceScalar(parentField)) {
            return new JsonPrimitive(parseExplicitId(element.getAsString(), pointer).toString());
        }
        return element.deepCopy();
    }

    private static JsonObject normalizeObject(JsonObject object, String pointer) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String field = entry.getKey();
            String childPointer = pointer + "/" + escape(field);
            if (TrainDataDrivenTypes.isResourceListField(field)
                    && !entry.getValue().isJsonArray()) {
                throw invalid(childPointer, "expected an array of namespaced resource ids");
            }
            if (isResourceScalar(field)
                    && (!entry.getValue().isJsonPrimitive()
                    || !entry.getValue().getAsJsonPrimitive().isString())) {
                throw invalid(childPointer, "expected a namespaced string resource id");
            }
            result.add(field, normalize(entry.getValue(), childPointer, field));
        }
        return result;
    }

    private static void validateResourceList(JsonArray array, String pointer) {
        int index = 0;
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw invalid(
                        pointer + "/" + index,
                        "expected a namespaced string resource id"
                );
            }
            parseExplicitId(element.getAsString(), pointer + "/" + index);
            index++;
        }
    }

    private static boolean isResourceScalar(String field) {
        return TrainDataDrivenTypes.TAB.equals(field)
                || TrainDataDrivenTypes.ICON_ITEM.equals(field);
    }

    private static void requireOptionalObject(
            JsonObject root,
            String field,
            Set<String> allowed
    ) {
        JsonElement value = root.get(field);
        if (value == null) {
            return;
        }
        if (!value.isJsonObject()) {
            throw invalid("/" + escape(field), "expected an object");
        }
        requireAllowedFields(value.getAsJsonObject(), allowed, "/" + escape(field));
    }

    private static void requireAllowedFields(
            JsonObject object,
            Set<String> allowed,
            String pointer
    ) {
        for (String field : object.keySet()) {
            if (!allowed.contains(field)) {
                throw invalid(pointer + "/" + escape(field), "unsupported field");
            }
        }
    }

    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    private static IllegalArgumentException invalid(String pointer, String message) {
        return new IllegalArgumentException("train patch at " + pointer + ": " + message);
    }
}
