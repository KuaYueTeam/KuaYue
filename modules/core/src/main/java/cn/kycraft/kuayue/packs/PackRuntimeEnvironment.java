package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime versions supplied by the host adapter.
 */
public record PackRuntimeEnvironment(Map<String, String> versions) implements Serializable {
    public PackRuntimeEnvironment {
        Objects.requireNonNull(versions, "versions");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        for (String modId : PackManifest.HOST_MODS) {
            String version = versions.get(modId);
            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("Missing runtime version for mod: " + modId);
            }
            copy.put(modId, version);
        }
        versions = Collections.unmodifiableMap(copy);
    }

    public String version(String modId) {
        return versions.get(modId);
    }
}
