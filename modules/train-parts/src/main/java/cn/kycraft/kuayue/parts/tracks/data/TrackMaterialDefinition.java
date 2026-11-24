package cn.kycraft.kuayue.parts.tracks.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data-driven definition for a Create {@code TrackMaterial}.
 *
 * @param id           unique resource location for the Create track material
 * @param name         material name used by Create
 * @param particle     particle texture resource location
 * @param halfDistance positive rail offset scale applied to the simple track model
 * @param sleepers     non-empty list of sleeper block resource locations
 * @param models       sleeper and rail segment model paths
 */
public record TrackMaterialDefinition(
        String id,
        String name,
        String particle,
        @SerializedName("half_distance") float halfDistance,
        List<String> sleepers,
        TrackDefinition.Models models
) {
    /**
     * Creates and validates a track material definition.
     *
     * @throws IllegalArgumentException when a resource location is malformed, the model list is
     *                                  missing, {@code halfDistance} is not finite and positive, or
     *                                  the sleeper list is missing or empty
     */
    public TrackMaterialDefinition {
        id = TrackStrings.requireId(id, "tracks[].material.id");
        name = TrackStrings.requireText(name, "tracks[].material.name");
        particle = TrackStrings.requireId(particle, "tracks[].material.particle");
        if (!Float.isFinite(halfDistance) || halfDistance <= 0.0f) {
            throw new IllegalArgumentException("tracks[].material.half_distance must be finite and greater than 0");
        }
        sleepers = sleepers == null ? List.of() : List.copyOf(sleepers);
        if (sleepers.isEmpty()) {
            throw new IllegalArgumentException("tracks[].material.sleepers must not be empty");
        }
        for (String sleeper : sleepers) {
            TrackStrings.requireId(sleeper, "tracks[].material.sleepers[]");
        }
        models = TrackStrings.requireValue(models, "tracks[].material.models");
    }
}
