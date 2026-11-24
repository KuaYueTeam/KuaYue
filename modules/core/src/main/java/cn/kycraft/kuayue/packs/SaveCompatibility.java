package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A pack's declaration for which of its own previous versions may be upgraded
 * directly into the current version.
 */
public record SaveCompatibility(
        List<SemanticVersion> fromVersions,
        SemanticVersion minimumVersion
) implements Serializable {
    public SaveCompatibility {
        if ((fromVersions == null) == (minimumVersion == null)) {
            throw new IllegalArgumentException(
                    "Exactly one of fromVersions or minimumVersion must be present");
        }
        if (fromVersions != null) {
            fromVersions = List.copyOf(new ArrayList<>(fromVersions));
            if (fromVersions.isEmpty()) {
                throw new IllegalArgumentException("fromVersions must not be empty");
            }
            for (SemanticVersion version : fromVersions) {
                Objects.requireNonNull(version, "fromVersions entry");
            }
            if (fromVersions.stream().distinct().count() != fromVersions.size()) {
                throw new IllegalArgumentException("fromVersions must not contain duplicates");
            }
        }
    }

    public boolean allowsUpgradeFrom(SemanticVersion historicalVersion, SemanticVersion currentVersion) {
        Objects.requireNonNull(historicalVersion, "historicalVersion");
        Objects.requireNonNull(currentVersion, "currentVersion");
        if (historicalVersion.compareTo(currentVersion) == 0) {
            return true;
        }
        if (historicalVersion.compareTo(currentVersion) > 0) {
            return false;
        }
        if (fromVersions != null) {
            return fromVersions.contains(historicalVersion);
        }
        return historicalVersion.compareTo(minimumVersion) >= 0;
    }
}
