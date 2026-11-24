package cn.kycraft.kuayue.parts.tracks.data;

import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;

import java.util.Set;

/**
 * Parses and validates the KuaYue-specific track schema.
 *
 * <p>This layer deliberately has no Minecraft registration dependencies so the JSON contract can
 * be tested without starting a game. It rejects unknown fields at every supported object level and
 * leaves runtime resource resolution to {@link TrackTypeHandler}.</p>
 */
public final class TrackDefinitionParser {
    /** Factory name for KuaYue's standard simple track block. */
    public static final String SIMPLE_TRACK = "simple_track";

    private static final Set<String> TRACK_FIELDS = Set.of(
            "id", "type", "registry_group", "default_bogey",
            "properties", "item_properties", "material"
    );
    private static final Set<String> MATERIAL_FIELDS = Set.of(
            "id", "name", "particle", "half_distance", "sleepers", "models"
    );
    private static final Set<String> MODEL_FIELDS = Set.of("tie", "left", "right");

    /**
     * Parses one {@code tracks} array element.
     *
     * @param json track object from a data-driven content file
     * @return the validated track definition
     * @throws IllegalArgumentException when the object contains unknown fields, malformed nested
     *                                  objects, invalid field values, or an unsupported factory type
     */
    public TrackDefinition parse(JsonObject json) {
        rejectUnknownFields(json, TRACK_FIELDS, "tracks[]");
        JsonObject material = requireObject(json, "material", "tracks[]");
        rejectUnknownFields(material, MATERIAL_FIELDS, "tracks[].material");
        rejectUnknownFields(
                requireObject(material, "models", "tracks[].material"),
                MODEL_FIELDS,
                "tracks[].material.models"
        );

        TrackDefinition definition = DataJson.GSON.fromJson(json, TrackDefinition.class);
        if (!SIMPLE_TRACK.equals(definition.type())) {
            throw new IllegalArgumentException("Unsupported track type '" + definition.type()
                    + "' for " + definition.id() + "; supported type is '" + SIMPLE_TRACK + "'");
        }
        return definition;
    }

    /**
     * Requires a field to contain a JSON object.
     *
     * @param parent parent object
     * @param field field name
     * @param path diagnostic path of the parent object
     * @return the nested object
     * @throws IllegalArgumentException when the field is absent or not an object
     */
    private static JsonObject requireObject(JsonObject parent, String field, String path) {
        if (!parent.has(field) || !parent.get(field).isJsonObject()) {
            throw new IllegalArgumentException(path + "." + field + " must be an object");
        }
        return parent.getAsJsonObject(field);
    }

    /**
     * Rejects fields that are not part of the versioned schema.
     *
     * @param json object to inspect
     * @param allowed supported field names
     * @param path diagnostic object path
     * @throws IllegalArgumentException when an unsupported field is present
     */
    private static void rejectUnknownFields(JsonObject json, Set<String> allowed, String path) {
        for (String field : json.keySet()) {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException(path + " contains unsupported field '" + field
                        + "'; supported fields are " + allowed);
            }
        }
    }
}
