package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.PackDiagnosticCodes;
import cn.kycraft.kuayue.packs.SaveCompatibility;
import cn.kycraft.kuayue.packs.SemanticVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackHistoryCompatibilityTest {
    @Test
    void missingHistoryIsReportedAsMissingRatherThanCompatible() {
        PackHistoryCompatibility.Result result = PackHistoryCompatibility.compare(
                PackHistoryState.missing(), List.of());

        assertEquals(PackHistoryCompatibility.Outcome.MISSING, result.outcome());
        assertFalse(result.compatible());
        assertEquals(List.of(PackHistoryDiagnosticCodes.HISTORY_MISSING),
                result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
    }

    @Test
    void sameVersionIsAcceptedWithoutContentInput() {
        PackHistoryState history = known(pack("example-railway", "0.2.0"));
        PackHistoryCompatibility.CurrentPack current = current(
                "example-railway", "0.2.0", null);

        PackHistoryCompatibility.Result result =
                PackHistoryCompatibility.compare(history, List.of(current));

        assertEquals(PackHistoryCompatibility.Outcome.COMPATIBLE, result.outcome());
        assertTrue(result.compatible());
        assertEquals(List.of(), result.diagnostics());
    }

    @Test
    void missingPacksAndDowngradesAreRejected() {
        PackHistoryCompatibility.Result missingPack = PackHistoryCompatibility.compare(
                known(pack("example-railway", "0.2.0")), List.of());
        assertEquals(PackHistoryCompatibility.Outcome.INCOMPATIBLE, missingPack.outcome());
        assertEquals(List.of(PackDiagnosticCodes.UNKNOWN_PACK_ID),
                missingPack.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());

        PackHistoryCompatibility.Result downgrade = PackHistoryCompatibility.compare(
                known(pack("example-railway", "0.3.0")),
                List.of(current(
                        "example-railway",
                        "0.2.0",
                        new SaveCompatibility(
                                List.of(SemanticVersion.parse("0.1.0")), null))));
        assertEquals(PackHistoryCompatibility.Outcome.INCOMPATIBLE, downgrade.outcome());
        assertEquals(List.of(PackHistoryDiagnosticCodes.SAVE_VERSION_DOWNGRADE),
                downgrade.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
    }

    @Test
    void crossVersionUpgradeRequiresAnExplicitCompatibilityDeclaration() {
        PackHistoryCompatibility.Result result = PackHistoryCompatibility.compare(
                known(pack("example-railway", "0.1.0")),
                List.of(current("example-railway", "0.2.0", null)));

        assertEquals(PackHistoryCompatibility.Outcome.INCOMPATIBLE, result.outcome());
        assertEquals(List.of(PackDiagnosticCodes.SAVE_VERSION_INCOMPATIBLE),
                result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
    }

    @Test
    void exactCompatibilityAllowsOnlyListedDirectHistory() {
        SaveCompatibility exact = new SaveCompatibility(
                List.of(SemanticVersion.parse("0.1.0")), null);

        assertTrue(PackHistoryCompatibility.compare(
                known(pack("example-railway", "0.1.0")),
                List.of(current("example-railway", "0.2.0", exact))).compatible());

        PackHistoryCompatibility.Result rejected = PackHistoryCompatibility.compare(
                known(pack("example-railway", "0.1.5")),
                List.of(current("example-railway", "0.2.0", exact)));
        assertFalse(rejected.compatible());
        assertEquals(List.of(PackDiagnosticCodes.SAVE_VERSION_INCOMPATIBLE),
                rejected.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
    }

    @Test
    void minimumCompatibilityAllowsTheBoundaryAndRejectsOlderHistory() {
        SaveCompatibility minimum = new SaveCompatibility(
                null, SemanticVersion.parse("0.2.0"));

        assertTrue(PackHistoryCompatibility.compare(
                known(pack("example-railway", "0.2.0")),
                List.of(current("example-railway", "0.3.0", minimum))).compatible());

        PackHistoryCompatibility.Result older = PackHistoryCompatibility.compare(
                known(pack("example-railway", "0.1.0")),
                List.of(current("example-railway", "0.3.0", minimum)));
        assertFalse(older.compatible());
        assertEquals(List.of(PackDiagnosticCodes.SAVE_VERSION_INCOMPATIBLE),
                older.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
    }

    @Test
    void initializedEmptyHistoryHasNoHistoricalPacksToValidate() {
        PackHistoryCompatibility.Result result = PackHistoryCompatibility.compare(
                PackHistoryState.known(List.of()),
                List.of(current("example-railway", "0.2.0", null)));

        assertTrue(result.compatible());
        assertEquals(List.of(), result.diagnostics());
    }

    private static PackHistoryState known(PackHistoryEntry... packs) {
        return PackHistoryState.known(List.of(packs));
    }

    private static PackHistoryEntry pack(String id, String version) {
        return new PackHistoryEntry(id, SemanticVersion.parse(version), 1);
    }

    private static PackHistoryCompatibility.CurrentPack current(
            String id,
            String version,
            SaveCompatibility compatibility
    ) {
        return new PackHistoryCompatibility.CurrentPack(
                id, SemanticVersion.parse(version), 1, compatibility);
    }
}
