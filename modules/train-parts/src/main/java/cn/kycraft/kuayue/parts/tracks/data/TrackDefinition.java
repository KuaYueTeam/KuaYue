package cn.kycraft.kuayue.parts.tracks.data;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Complete data-driven definition for one KuaYue track block and its corresponding item.
 *
 * <p>{@code type} selects the KuaYue track factory. Currently only {@code simple_track} is
 * supported. Block and item properties are inherited from {@code registry_group} and can be
 * overridden by the optional fields on this definition.</p>
 *
 * @param id             unique resource location for the track block and item
 * @param type           track factory name; currently {@code simple_track}
 * @param registryGroup  existing registry group providing inherited block and item properties
 * @param defaultBogey   bogey block resource location resolved lazily when a bogey anchor is created
 * @param properties     optional block property overrides
 * @param itemProperties optional item property and creative tab overrides
 * @param material       Create track material definition
 */
public record TrackDefinition(
        String id,
        String type,
        @SerializedName("registry_group") String registryGroup,
        @SerializedName("default_bogey") String defaultBogey,
        JsonObject properties,
        @SerializedName("item_properties") JsonObject itemProperties,
        TrackMaterialDefinition material
) {
    /**
     * Creates and validates a track definition.
     *
     * @throws IllegalArgumentException when a required field is absent, blank, or malformed
     */
    public TrackDefinition {
        id = TrackStrings.requireId(id, "tracks[].id");
        type = TrackStrings.requireText(type, "tracks[].type");
        registryGroup = TrackStrings.requireId(registryGroup, "tracks[].registry_group");
        defaultBogey = TrackStrings.requireId(defaultBogey, "tracks[].default_bogey");
        properties = properties == null ? new JsonObject() : properties.deepCopy();
        itemProperties = itemProperties == null ? new JsonObject() : itemProperties.deepCopy();
        material = TrackStrings.requireValue(material, "tracks[].material");
    }

    /**
     * Create track model paths used for the sleeper, left rail segment, and right rail segment.
     *
     * <p>Each path is relative to the {@code models/} directory of its namespace.</p>
     *
     * @param tie   sleeper model
     * @param left  left rail segment model
     * @param right right rail segment model
     */
    public record Models(String tie, String left, String right) {
        /**
         * Creates and validates the three required track model paths.
         *
         * @throws IllegalArgumentException when any model path is missing or blank
         */
        public Models {
            tie = TrackStrings.requireId(tie, "tracks[].material.models.tie");
            left = TrackStrings.requireId(left, "tracks[].material.models.left");
            right = TrackStrings.requireId(right, "tracks[].material.models.right");
        }
    }
}
