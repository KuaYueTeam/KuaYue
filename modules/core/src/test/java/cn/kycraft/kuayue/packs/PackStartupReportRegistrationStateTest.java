package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackStartupReportRegistrationStateTest {

    @Test
    void rejectedRegistrationPackIsMarkedInvalidAndIneffective() {
        PackStartupReport report = new PackStartupReport(
                PackStartupReport.FORMAT_VERSION,
                Map.of(),
                List.of(
                        installation("broken", true),
                        installation("sample", true)
                ),
                List.of(
                        resolved("broken"),
                        resolved("sample")
                ),
                List.of()
        );
        PackDiagnostic diagnostic = PackDiagnostic.error(
                "UNKNOWN_FACTORY",
                PackStage.CONTENT,
                "broken",
                "broken",
                "data/broken/kasuga_lib/data_driven/demo.json",
                "/blocks/0/type",
                "unknown factory"
        );

        PackStartupReport updated = report.withRegistrationState(
                Set.of("sample"),
                List.of(diagnostic)
        );

        PackInstallationStatus broken = updated.installations().stream()
                .filter(status -> "broken".equals(status.packId()))
                .findFirst()
                .orElseThrow();
        assertFalse(broken.effective());
        assertFalse(broken.valid());
        assertTrue(broken.diagnosticCodes().contains("UNKNOWN_FACTORY"));
        assertEquals(List.of("sample"), updated.effectivePacks().stream()
                .map(ResolvedPack::id)
                .toList());
    }

    private static PackInstallationStatus installation(String id, boolean requested) {
        return new PackInstallationStatus(
                "directory:" + id,
                PackSourceKind.DIRECTORY,
                id,
                id,
                SemanticVersion.parse("1.0.0"),
                requested,
                true,
                true,
                List.of()
        );
    }

    private static ResolvedPack resolved(String id) {
        return new ResolvedPack(
                id,
                SemanticVersion.parse("1.0.0"),
                PackSourceKind.DIRECTORY,
                "directory:" + id
        );
    }
}
