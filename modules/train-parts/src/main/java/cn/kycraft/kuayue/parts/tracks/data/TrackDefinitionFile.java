package cn.kycraft.kuayue.parts.tracks.data;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Top-level shape of a KuaYue track registration document.
 *
 * <p>{@code registry_groups} uses Kasuga's generic registry group schema. {@code tracks} is
 * consumed by {@link TrackTypeHandler}. Missing arrays are normalized to empty immutable lists.</p>
 *
 * @param registryGroups registry group objects consumed by Kasuga's built-in group handler
 * @param tracks         track definitions consumed by {@link TrackTypeHandler}
 */
public record TrackDefinitionFile(
        @SerializedName("registry_groups") List<JsonObject> registryGroups,
        @SerializedName("tracks") List<TrackDefinition> tracks
) {
    /**
     * Creates an immutable track registration document.
     */
    public TrackDefinitionFile {
        registryGroups = registryGroups == null ? List.of() : List.copyOf(registryGroups);
        tracks = tracks == null ? List.of() : List.copyOf(tracks);
    }
}
