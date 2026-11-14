package cn.kycraft.kuayue.packs.state;

import cn.kycraft.kuayue.packs.PackConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackRuntimeStateTest {
    @AfterEach
    void reset() {
        PackRuntimeState.resetForTests();
    }

    @Test
    void defaultsToEnabledBeforeInitialization() {
        assertTrue(PackRuntimeState.corepackEnabled());
    }

    @Test
    void freezesExplicitCorepackDisable() {
        PackRuntimeState.install(new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("kuayue-core", false),
                List.of("kuayue-core"),
                List.of()
        ));
        assertFalse(PackRuntimeState.corepackEnabled());
        assertFalse(PackRuntimeState.isRegistrationSelectionFinalized());
    }

    @Test
    void registrationSelectionTracksRejectedPacks() {
        PackRuntimeState.install(
                PackConfig.defaults(),
                List.of("kuayue-core", "broken")
        );
        PackRuntimeState.updateRegistrationSelection(Set.of("kuayue-core"));

        assertEquals(Set.of("kuayue-core"), PackRuntimeState.registeredPackIds());
        assertTrue(PackRuntimeState.isRegistrationSelectionFinalized());
    }
}
