package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackFailurePolicyTest {

    @Test
    void registrationOrderReversesLowToHighPriority() {
        assertEquals(
                List.of("high", "low"),
                PackResourceOrder.registrationOrder(List.of("low", "high"))
        );
    }

    @Test
    void onlyConfigurationErrorsAbortStartup() {
        PackDiagnostic configError = PackDiagnostic.error(
                "CONFIG_INVALID", PackStage.CONFIG, null, "config",
                "kuayue/config.json", "", "invalid config"
        );
        PackDiagnostic packError = PackDiagnostic.error(
                "INVALID_MANIFEST", PackStage.MANIFEST, "broken", "directory:broken",
                "pack.json", "", "invalid pack"
        );

        assertTrue(PackFailurePolicy.isFatalDiscoveryDiagnostic(configError));
        assertFalse(PackFailurePolicy.isFatalDiscoveryDiagnostic(packError));
    }

    @Test
    void dependencyClosureRejectsPacksDependingOnRejectedPack() {
        Set<String> blocked = PackFailurePolicy.includeDependents(
                Set.of("foundation"),
                List.of("foundation", "dependent", "independent"),
                Map.of(
                        "foundation", Set.of(),
                        "dependent", Set.of("foundation"),
                        "independent", Set.of()
                )
        );

        assertEquals(Set.of("foundation", "dependent"), blocked);
    }
}
