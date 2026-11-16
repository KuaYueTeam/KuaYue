package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackSelectionDraftTest {
    @Test
    void editsOnlyPendingSelectionAndPreservesUnknownOrderSlots() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of(
                        PackManifest.CORE_PACK_ID, true,
                        "alpha", true,
                        "missing", true
                ),
                List.of("alpha", "off", "missing", PackManifest.CORE_PACK_ID),
                List.of("alpha", "off")
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("alpha", "off", "beta", "missing"),
                Set.of("alpha", "off", "beta", PackManifest.CORE_PACK_ID)
        );

        draft.remove("missing");
        draft.add("beta");
        PackConfig result = draft.toConfig();

        assertEquals(Boolean.FALSE, result.enabled().get("missing"));
        assertEquals(Boolean.TRUE, result.enabled().get("beta"));
        assertTrue(result.order().contains("off"));
        assertEquals("beta", result.order().getLast());
        assertFalse(draft.selectedIds().contains("missing"));
    }

    @Test
    void movingSelectedPackDoesNotMoveDisabledSlots() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of(
                        PackManifest.CORE_PACK_ID, true,
                        "alpha", true,
                        "beta", true
                ),
                List.of("alpha", "off", "beta", PackManifest.CORE_PACK_ID),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("alpha", "off", "beta"),
                Set.of("alpha", "beta", PackManifest.CORE_PACK_ID)
        );

        draft.move("beta", -1);

        assertEquals(List.of("beta", "off", "alpha", PackManifest.CORE_PACK_ID),
                draft.toConfig().order());
        assertEquals(List.of("beta", "alpha", PackManifest.CORE_PACK_ID), draft.selectedIds());
    }

    @Test
    void userFacingPriorityOrderIsHighestFirst() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of(
                        PackManifest.CORE_PACK_ID, true,
                        "alpha", true,
                        "beta", true
                ),
                List.of("alpha", "off", "beta", PackManifest.CORE_PACK_ID),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("alpha", "off", "beta"),
                Set.of("alpha", "beta", PackManifest.CORE_PACK_ID)
        );

        assertEquals(
                List.of(PackManifest.CORE_PACK_ID, "beta", "alpha"),
                draft.selectedIdsByPriority()
        );

        draft.moveByPriority("alpha", -1);

        assertEquals(
                List.of("beta", "off", "alpha", PackManifest.CORE_PACK_ID),
                draft.toConfig().order()
        );
        assertEquals(
                List.of(PackManifest.CORE_PACK_ID, "alpha", "beta"),
                draft.selectedIdsByPriority()
        );
    }
}
