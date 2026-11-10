package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.annotations.SerializedName;

/**
 * Data-driven definition of one Create bogey size.
 *
 * @param id          stable resource location of the existing bogey size
 * @param wheelRadius radius of the bogey wheel in blocks
 */
public record BogeySizeDefinition(
        String id,
        @SerializedName("wheel_radius") float wheelRadius
) {
    /**
     * Creates and validates a bogey size definition.
     */
    public BogeySizeDefinition {
        id = BogeySchemaStrings.requireId(id, "bogey_sizes[].id");
        if (!Float.isFinite(wheelRadius) || wheelRadius <= 0.0f) {
            throw new IllegalArgumentException("bogey_sizes[].wheel_radius must be finite and positive");
        }
    }
}
