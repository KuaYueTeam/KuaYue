package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.Objects;

public record ResolvedPack(
        String id,
        SemanticVersion version,
        PackSourceKind sourceKind,
        String sourceId
) implements Serializable, Comparable<ResolvedPack> {
    public ResolvedPack {
        if (!PackPathRules.isPackId(id)) {
            throw new IllegalArgumentException("Invalid resolved pack ID: " + id);
        }
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(sourceKind, "sourceKind");
        Objects.requireNonNull(sourceId, "sourceId");
    }

    @Override
    public int compareTo(ResolvedPack other) {
        return id.compareTo(other.id);
    }
}
