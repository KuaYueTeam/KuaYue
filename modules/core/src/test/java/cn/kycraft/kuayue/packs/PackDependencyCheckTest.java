package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackDependencyCheckTest {
    private static final Map<String, String> RUNTIME = Map.of(
            "minecraft", "1.21.1",
            "neoforge", "21.1.203",
            "kuayue", "2.0.0",
            "kasuga_lib", "1.0.0",
            "create", "6.0.6"
    );

    @Test
    void reportsDisabledDirectPackDependency() {
        PackManifest alpha = manifest("alpha", Map.of("beta", "*"));
        Map<String, PackManifest> known = Map.of(
                "alpha", alpha,
                "beta", manifest("beta", Map.of())
        );

        PackDependencyCheck.Result result = PackDependencyCheck.check(
                alpha, known, Set.of(), RUNTIME, VersionComparator.semantic());

        assertFalse(result.satisfied());
        assertEquals(List.of("beta"), ids(result.missingPacks()));
        assertEquals("*", result.missingPacks().getFirst().requirement().expression());
        assertTrue(result.missingMods().isEmpty());
    }

    @Test
    void reportsTransitiveMissingDependency() {
        PackManifest alpha = manifest("alpha", Map.of("beta", "*"));
        Map<String, PackManifest> known = Map.of(
                "alpha", alpha,
                "beta", manifest("beta", Map.of("gamma", "*")),
                "gamma", manifest("gamma", Map.of())
        );

        PackDependencyCheck.Result result = PackDependencyCheck.check(
                alpha, known, Set.of("beta"), RUNTIME, VersionComparator.semantic());

        assertFalse(result.satisfied());
        assertEquals(List.of("gamma"), ids(result.missingPacks()));
    }

    @Test
    void reportsModVersionMismatch() {
        PackManifest alpha = manifestWithCreateRange("alpha", "[7.0.0,8.0.0)");

        PackDependencyCheck.Result result = PackDependencyCheck.check(
                alpha, Map.of("alpha", alpha), Set.of(), RUNTIME, VersionComparator.semantic());

        assertFalse(result.satisfied());
        assertTrue(result.missingPacks().isEmpty());
        assertEquals(List.of("create"), ids(result.missingMods()));
        assertEquals("[7.0.0,8.0.0)",
                result.missingMods().getFirst().requirement().expression());
    }

    @Test
    void acceptsCompleteEnabledPackClosure() {
        PackManifest alpha = manifest("alpha", Map.of("beta", "*"));
        Map<String, PackManifest> known = Map.of(
                "alpha", alpha,
                "beta", manifest("beta", Map.of("gamma", "*")),
                "gamma", manifest("gamma", Map.of())
        );

        PackDependencyCheck.Result result = PackDependencyCheck.check(
                alpha, known, Set.of("beta", "gamma"), RUNTIME, VersionComparator.semantic());

        assertTrue(result.satisfied());
        assertTrue(result.missingPacks().isEmpty());
        assertTrue(result.missingMods().isEmpty());
    }

    @Test
    void findsOnlyEnabledDirectAndTransitiveDependents() {
        Map<String, PackManifest> known = Map.of(
                "alpha", manifest("alpha", Map.of("beta", "*")),
                "beta", manifest("beta", Map.of("gamma", "*")),
                "gamma", manifest("gamma", Map.of()),
                "delta", manifest("delta", Map.of("gamma", "*"))
        );

        Set<String> dependents = PackDependencyCheck.enabledDependents(
                "gamma",
                known,
                Set.of("alpha", "beta", "gamma")
        );

        assertEquals(Set.of("alpha", "beta"), dependents);
    }

    @Test
    void cascadingDisableRemovesTargetAndEnabledDependents() {
        PackManifest alpha = manifest("alpha", Map.of("beta", "*"));
        PackManifest beta = manifest("beta", Map.of("gamma", "*"));
        PackManifest gamma = manifest("gamma", Map.of());
        Map<String, PackManifest> known = Map.of(
                "alpha", alpha,
                "beta", beta,
                "gamma", gamma,
                "delta", manifest("delta", Map.of("gamma", "*"))
        );
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("alpha", true, "beta", true, "gamma", true, "delta", false),
                List.of("alpha", "beta", "gamma", "delta"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                known.keySet(),
                known.keySet()
        );

        List<String> removed = draft.removeWithEnabledDependents("gamma", known);

        assertEquals(Set.of("alpha", "beta", "gamma"), Set.copyOf(removed));
        assertTrue(draft.selectedIds().isEmpty());
        assertEquals(Boolean.FALSE, draft.toConfig().enabled().get("alpha"));
        assertEquals(Boolean.FALSE, draft.toConfig().enabled().get("beta"));
        assertEquals(Boolean.FALSE, draft.toConfig().enabled().get("gamma"));
        assertEquals(Boolean.FALSE, draft.toConfig().enabled().get("delta"));
    }

    @Test
    void lockedTargetDoesNotRemoveAnyDependents() {
        PackManifest alpha = manifest("alpha", Map.of("gamma", "*"));
        PackManifest gamma = manifest("gamma", Map.of());
        Map<String, PackManifest> known = Map.of("alpha", alpha, "gamma", gamma);
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("alpha", true, "gamma", true),
                List.of("alpha", "gamma"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                known.keySet(),
                known.keySet(),
                Set.of("gamma")
        );

        List<String> removed = draft.removeWithEnabledDependents("gamma", known);

        assertTrue(removed.isEmpty());
        assertEquals(Set.of("alpha", "gamma"), Set.copyOf(draft.selectedIds()));
    }

    private static List<String> ids(List<PackDependencyCheck.MissingDependency> dependencies) {
        return dependencies.stream()
                .map(PackDependencyCheck.MissingDependency::id)
                .toList();
    }

    private static PackManifest manifest(String id, Map<String, String> packDependencies) {
        return manifest(id, packDependencies, "1.21.1", "21.1.203", "2.0.0", "1.0.0", "6.0.6");
    }

    private static PackManifest manifestWithCreateRange(String id, String createRange) {
        return manifest(id, Map.of(), "1.21.1", "21.1.203", "2.0.0", "1.0.0", createRange);
    }

    private static PackManifest manifest(
            String id,
            Map<String, String> packDependencies,
            String minecraft,
            String neoforge,
            String kuayue,
            String kasuga,
            String create
    ) {
        LinkedHashMap<String, VersionRange> mods = new LinkedHashMap<>();
        mods.put("minecraft", VersionRange.parse(minecraft));
        mods.put("neoforge", VersionRange.parse(neoforge));
        mods.put("kuayue", VersionRange.parse(kuayue));
        mods.put("kasuga_lib", VersionRange.parse(kasuga));
        mods.put("create", VersionRange.parse(create));
        LinkedHashMap<String, VersionRange> packs = new LinkedHashMap<>();
        packDependencies.forEach((key, range) -> packs.put(key, VersionRange.parse(range)));
        return new PackManifest(
                1,
                id,
                SemanticVersion.parse("1.0.0"),
                List.of(id.replace('-', '_')),
                "pack." + id + ".name",
                "pack." + id + ".description",
                false,
                new PackManifest.Dependencies(mods, packs),
                List.of("parcels/main/parcel.json"),
                null
        );
    }
}
