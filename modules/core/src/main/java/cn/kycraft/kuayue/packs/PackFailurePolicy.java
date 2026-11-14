package cn.kycraft.kuayue.packs;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure pack-level rejection rules shared by bootstrap and registration.
 */
public final class PackFailurePolicy {
    private PackFailurePolicy() {
    }

    /**
     * External manifests and content are isolated. Only an invalid instance configuration is fatal.
     */
    public static boolean isFatalDiscoveryDiagnostic(PackDiagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        return diagnostic.blocking() && diagnostic.stage() == PackStage.CONFIG;
    }

    /**
     * Expands rejected pack IDs through direct and transitive required dependents.
     */
    public static Set<String> includeDependents(
            Set<String> rejected,
            Collection<String> activePackIds,
            Map<String, Set<String>> dependencies
    ) {
        Objects.requireNonNull(rejected, "rejected");
        Objects.requireNonNull(activePackIds, "activePackIds");
        Objects.requireNonNull(dependencies, "dependencies");
        Set<String> blocked = new LinkedHashSet<>(rejected);
        boolean changed;
        do {
            changed = false;
            for (String packId : activePackIds) {
                if (blocked.contains(packId)) {
                    continue;
                }
                if (dependencies.getOrDefault(packId, Set.of()).stream()
                        .anyMatch(blocked::contains)) {
                    changed |= blocked.add(packId);
                }
            }
        } while (changed);
        return Set.copyOf(blocked);
    }
}
