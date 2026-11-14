package cn.kycraft.kuayue.packs;

/**
 * Host-supplied comparator used for mod dependency versions. The K1 default
 * accepts the same strict semantic subset so tests and standalone validation
 * remain deterministic; NeoForge integration can inject its own comparator.
 */
@FunctionalInterface
public interface VersionComparator {
    int compare(String left, String right);

    static VersionComparator semantic() {
        return SemanticVersion::compareText;
    }
}
