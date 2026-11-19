package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.PackPathRules;
import cn.kycraft.kuayue.packs.SemanticVersion;

import java.util.Objects;

/**
 * The exact persisted identity of one enabled pack at the last accepted save.
 */
public record PackHistoryEntry(
        String id,
        SemanticVersion version,
        int formatVersion
) implements HistoryPack {
    public PackHistoryEntry {
        if (!PackPathRules.isPackId(id)) {
            throw new IllegalArgumentException("Invalid pack ID in world history: " + id);
        }
        Objects.requireNonNull(version, "version");
        if (formatVersion != PackManifest.FORMAT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported pack format version in world history: " + formatVersion);
        }
    }

    public static PackHistoryEntry from(PackManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return new PackHistoryEntry(
                manifest.id(), manifest.version(), manifest.formatVersion());
    }
}
