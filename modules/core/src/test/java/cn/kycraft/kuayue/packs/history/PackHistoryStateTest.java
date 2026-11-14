package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.SemanticVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackHistoryStateTest {
    @Test
    void missingAndInitializedEmptyHistoryRemainDistinct() {
        PackHistoryState missing = PackHistoryState.missing();
        PackHistoryState empty = PackHistoryState.known(List.of());

        assertFalse(missing.initialized());
        assertTrue(empty.initialized());
        assertEquals(List.of(), missing.packs());
        assertEquals(List.of(), empty.packs());
    }

    @Test
    void rejectsPacksInMissingStateAndDuplicateIds() {
        PackHistoryEntry pack = new PackHistoryEntry(
                "example-railway", SemanticVersion.parse("0.2.0"), 1);

        assertThrows(IllegalArgumentException.class,
                () -> new PackHistoryState(false, List.of(pack)));
        assertThrows(IllegalArgumentException.class,
                () -> PackHistoryState.known(List.of(pack, pack)));
    }
}
