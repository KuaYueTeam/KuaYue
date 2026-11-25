package cn.kycraft.kuayue.train;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * One source's declaration for a train. The object deliberately excludes {@code id}, which is the
 * stable overlay key, and retains only fields that this source explicitly declared.
 */
public final class TrainPatch {
    private final String id;
    private final JsonObject declaredFields;

    TrainPatch(String id, JsonObject declaredFields) {
        this.id = Objects.requireNonNull(id, "id");
        this.declaredFields = Objects.requireNonNull(declaredFields, "declaredFields").deepCopy();
    }

    public String id() {
        return id;
    }

    /**
     * Returns a defensive copy of every explicitly declared field below {@code id}.
     */
    public JsonObject declaredFields() {
        return declaredFields.deepCopy();
    }

    /**
     * Checks whether this patch declares a JSON pointer. Both {@code /members/items} and
     * {@code members.items} are accepted.
     */
    public boolean declares(String pointer) {
        String normalized = Objects.requireNonNull(pointer, "pointer").strip();
        if (normalized.isEmpty() || normalized.equals("/")) {
            return true;
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized.replace('.', '/');
        }

        JsonElement current = declaredFields;
        for (String token : normalized.substring(1).split("/", -1)) {
            if (!current.isJsonObject()) {
                return false;
            }
            current = current.getAsJsonObject().get(unescape(token));
            if (current == null) {
                return false;
            }
        }
        return true;
    }

    private static String unescape(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }
}
