package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data-driven definition of a KuaYue bogey block.
 *
 * @param id            stable block resource location
 * @param size          referenced {@code bogey_sizes} entry
 * @param registryGroup optional block-property registry group
 * @param defaultStyle  referenced {@code bogey_styles} entry
 * @param properties    optional block-property overrides
 * @param physics       Create bogey movement and pathfinding parameters
 */
public record BogeyBlockDefinition(
        String id,
        String size,
        @SerializedName("registry_group") String registryGroup,
        @SerializedName("default_style") String defaultStyle,
        JsonObject properties,
        Physics physics
) {
    /**
     * Creates and validates a bogey block definition.
     */
    public BogeyBlockDefinition {
        id = BogeySchemaStrings.requireId(id, "bogey_blocks[].id");
        size = BogeySchemaStrings.requireId(size, "bogey_blocks[].size");
        registryGroup = registryGroup == null
                ? null
                : BogeySchemaStrings.requireId(registryGroup, "bogey_blocks[].registry_group");
        defaultStyle = BogeySchemaStrings.requireId(defaultStyle, "bogey_blocks[].default_style");
        properties = properties == null ? new JsonObject() : properties.deepCopy();
        physics = BogeySchemaStrings.requireValue(physics, "bogey_blocks[].physics");
    }

    /**
     * Create runtime parameters carried by {@code CommonBogeyBlock}.
     *
     * @param primaryTrackType         primary Create track type
     * @param validTrackTypes          track types accepted during pathfinding
     * @param wheelPointSpacing        distance between wheel contact points
     * @param connectorAnchorOffset    three-dimensional connector anchor offset
     * @param allowSingleBogeyCarriage whether a carriage may use one bogey
     */
    public record Physics(
            @SerializedName("primary_track_type") String primaryTrackType,
            @SerializedName("valid_track_types") List<String> validTrackTypes,
            @SerializedName("wheel_point_spacing") double wheelPointSpacing,
            @SerializedName("connector_anchor_offset") List<Double> connectorAnchorOffset,
            @SerializedName("allow_single_bogey_carriage") boolean allowSingleBogeyCarriage
    ) {
        /**
         * Creates and validates bogey runtime parameters.
         */
        public Physics {
            primaryTrackType = primaryTrackType == null
                    ? "standard"
                    : BogeySchemaStrings.requireText(primaryTrackType,
                            "bogey_blocks[].physics.primary_track_type");
            validTrackTypes = BogeySchemaStrings.immutableIds(
                    validTrackTypes,
                    "bogey_blocks[].physics.valid_track_types[]"
            );
            if (validTrackTypes.isEmpty()) {
                validTrackTypes = List.of(primaryTrackType);
            }
            if (!Double.isFinite(wheelPointSpacing) || wheelPointSpacing <= 0.0) {
                throw new IllegalArgumentException(
                        "bogey_blocks[].physics.wheel_point_spacing must be finite and positive"
                );
            }
            connectorAnchorOffset = connectorAnchorOffset == null
                    ? List.of(0.0, 0.21875, 1.0)
                    : List.copyOf(connectorAnchorOffset);
            if (connectorAnchorOffset.size() != 3) {
                throw new IllegalArgumentException(
                        "bogey_blocks[].physics.connector_anchor_offset must contain three numbers"
                );
            }
            for (double coordinate : connectorAnchorOffset) {
                if (!Double.isFinite(coordinate)) {
                    throw new IllegalArgumentException(
                            "bogey_blocks[].physics.connector_anchor_offset must be finite"
                    );
                }
            }
        }
    }
}
