package cn.kycraft.kuayue.packs.state;

import cn.kycraft.kuayue.packs.PackConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackRuntimeStateTest {
    @AfterEach
    void reset() {
        PackRuntimeState.resetForTests();
    }

    @Test
    void freezesPackConfigurationBeforeRegistrationSelection() {
        PackRuntimeState.install(new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("running-gear", true, "passenger", false),
                List.of("running-gear", "passenger"),
                List.of()
        ));
        assertTrue(PackRuntimeState.current().orElseThrow()
                .isRequestedEnabled("running-gear"));
        assertTrue(!PackRuntimeState.current().orElseThrow()
                .isRequestedEnabled("passenger"));
        assertTrue(!PackRuntimeState.isRegistrationSelectionFinalized());
    }

    @Test
    void registrationSelectionTracksRejectedPacks() {
        PackRuntimeState.install(
                PackConfig.defaults(),
                List.of("running-gear", "broken")
        );
        PackRuntimeState.updateRegistrationSelection(Set.of("running-gear"));

        assertEquals(Set.of("running-gear"), PackRuntimeState.registeredPackIds());
        assertTrue(PackRuntimeState.isRegistrationSelectionFinalized());
    }
}
