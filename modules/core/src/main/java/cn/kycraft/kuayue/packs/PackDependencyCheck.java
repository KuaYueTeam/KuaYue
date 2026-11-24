package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves the enabled pack and installed mod dependencies required by one pack.
 */
public final class PackDependencyCheck {
    private PackDependencyCheck() {
    }

    public static Result check(
            PackManifest target,
            Map<String, PackManifest> knownPacks,
            Set<String> enabledPacks,
            Map<String, String> runtimeVersions,
            VersionComparator modComparator
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(knownPacks, "knownPacks");
        Objects.requireNonNull(enabledPacks, "enabledPacks");
        Objects.requireNonNull(runtimeVersions, "runtimeVersions");
        Objects.requireNonNull(modComparator, "modComparator");

        LinkedHashMap<String, VersionRange> missingPacks = new LinkedHashMap<>();
        LinkedHashMap<String, VersionRange> missingMods = new LinkedHashMap<>();
        inspect(
                target,
                target.id(),
                knownPacks,
                enabledPacks,
                runtimeVersions,
                modComparator,
                new LinkedHashSet<>(),
                missingPacks,
                missingMods
        );
        return new Result(
                dependencies(missingPacks),
                dependencies(missingMods)
        );
    }

    /**
     * Returns enabled packs that depend on the target directly or transitively.
     */
    public static Set<String> enabledDependents(
            String targetId,
            Map<String, PackManifest> knownPacks,
            Set<String> enabledPacks
    ) {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(knownPacks, "knownPacks");
        Objects.requireNonNull(enabledPacks, "enabledPacks");
        LinkedHashSet<String> dependents = new LinkedHashSet<>();
        for (String candidateId : enabledPacks) {
            if (candidateId.equals(targetId)) {
                continue;
            }
            PackManifest candidate = knownPacks.get(candidateId);
            if (candidate != null && dependsOn(
                    candidate,
                    targetId,
                    knownPacks,
                    new LinkedHashSet<>()
            )) {
                dependents.add(candidateId);
            }
        }
        return Collections.unmodifiableSet(dependents);
    }

    private static List<MissingDependency> dependencies(Map<String, VersionRange> missing) {
        List<MissingDependency> result = new ArrayList<>(missing.size());
        missing.forEach((id, requirement) ->
                result.add(new MissingDependency(id, requirement)));
        return List.copyOf(result);
    }

    private static void inspect(
            PackManifest current,
            String rootId,
            Map<String, PackManifest> knownPacks,
            Set<String> enabledPacks,
            Map<String, String> runtimeVersions,
            VersionComparator modComparator,
            Set<String> visitedPacks,
            Map<String, VersionRange> missingPacks,
            Map<String, VersionRange> missingMods
    ) {
        if (!visitedPacks.add(current.id())) {
            return;
        }

        for (var dependency : current.dependencies().packs().entrySet()) {
            String id = dependency.getKey();
            PackManifest installed = knownPacks.get(id);
            boolean available = id != null
                    && !id.equals(rootId)
                    && enabledPacks.contains(id)
                    && installed != null
                    && dependency.getValue().matches(installed.version());
            if (!available) {
                missingPacks.putIfAbsent(id, dependency.getValue());
            }
            if (installed != null && !id.equals(rootId)) {
                inspect(
                        installed,
                        rootId,
                        knownPacks,
                        enabledPacks,
                        runtimeVersions,
                        modComparator,
                        visitedPacks,
                        missingPacks,
                        missingMods
                );
            }
        }

        for (var dependency : current.dependencies().mods().entrySet()) {
            String actualVersion = runtimeVersions.get(dependency.getKey());
            boolean available;
            try {
                available = actualVersion != null
                        && dependency.getValue().matches(actualVersion, modComparator);
            } catch (RuntimeException ignored) {
                available = false;
            }
            if (!available) {
                missingMods.putIfAbsent(dependency.getKey(), dependency.getValue());
            }
        }
    }

    private static boolean dependsOn(
            PackManifest current,
            String targetId,
            Map<String, PackManifest> knownPacks,
            Set<String> visited
    ) {
        if (!visited.add(current.id())) {
            return false;
        }
        for (String dependencyId : current.dependencies().packs().keySet()) {
            if (dependencyId.equals(targetId)) {
                return true;
            }
            PackManifest dependency = knownPacks.get(dependencyId);
            if (dependency != null
                    && dependsOn(dependency, targetId, knownPacks, visited)) {
                return true;
            }
        }
        return false;
    }

    public record MissingDependency(String id, VersionRange requirement) {
        public MissingDependency {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(requirement, "requirement");
        }
    }

    public record Result(
            List<MissingDependency> missingPacks,
            List<MissingDependency> missingMods
    ) {
        public Result {
            missingPacks = List.copyOf(missingPacks);
            missingMods = List.copyOf(missingMods);
        }

        public boolean satisfied() {
            return missingPacks.isEmpty() && missingMods.isEmpty();
        }
    }
}
