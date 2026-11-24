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
    void staleConfigIdsAreNotShownAsSelectedPacks() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("kuayue-core", true, "alpha", true),
                List.of("kuayue-core", "alpha"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("alpha"),
                Set.of("alpha")
        );

        assertEquals(List.of("alpha"), draft.selectedIds());
        assertEquals(List.of("alpha"), draft.selectedIdsByPriority());
    }

    @Test
    void requiredPackCannotBeRemovedOrMoved() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("alpha", true),
                List.of("alpha"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("base", "alpha"),
                Set.of("alpha"),
                Set.of("base")
        );

        draft.remove("base");
        draft.moveByPriority("base", 1);

        assertEquals(List.of("base", "alpha"), draft.selectedIds());
        assertEquals(Map.of("alpha", true), draft.toConfig().enabled());
        assertEquals(List.of("alpha"), draft.toConfig().order());
        assertFalse(draft.toConfig().enabled().containsKey("base"));
    }

    @Test
    void requiredPackIsSelectedWhileIgnoringItsPersistedEntries() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("base", false, "alpha", true),
                List.of("base", "alpha"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("base", "alpha"),
                Set.of("alpha"),
                Set.of("base")
        );

        assertEquals(List.of("base", "alpha"), draft.selectedIds());
        assertEquals(List.of("alpha", "base"), draft.selectedIdsByPriority());
        assertEquals(Map.of("alpha", true), draft.toConfig().enabled());
        assertEquals(List.of("alpha"), draft.toConfig().order());
    }

    @Test
    void editsOnlyPendingSelectionAndPreservesUnknownOrderSlots() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of(
                        "base", true,
                        "alpha", true,
                        "missing", true
                ),
                List.of("alpha", "off", "missing", "base"),
                List.of("alpha", "off")
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("alpha", "off", "beta", "missing"),
                Set.of("alpha", "off", "beta")
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
                        "base", true,
                        "alpha", true,
                        "beta", true
                ),
                List.of("alpha", "off", "beta", "base"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("base", "alpha", "off", "beta"),
                Set.of("alpha", "beta")
        );

        draft.move("beta", -1);

        assertEquals(List.of("beta", "off", "alpha", "base"),
                draft.toConfig().order());
        assertEquals(List.of("beta", "alpha", "base"), draft.selectedIds());
    }

    @Test
    void userFacingPriorityOrderIsHighestFirst() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of(
                        "base", true,
                        "alpha", true,
                        "beta", true
                ),
                List.of("alpha", "off", "beta", "base"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("base", "alpha", "off", "beta"),
                Set.of("alpha", "beta")
        );

        assertEquals(
                List.of("base", "beta", "alpha"),
                draft.selectedIdsByPriority()
        );

        draft.moveByPriority("alpha", -1);

        assertEquals(
                List.of("beta", "off", "alpha", "base"),
                draft.toConfig().order()
        );
        assertEquals(
                List.of("base", "alpha", "beta"),
                draft.selectedIdsByPriority()
        );
    }

    @Test
    void requiredPackCannotBeCrossedOrMoved() {
        PackConfig baseline = new PackConfig(
                PackConfig.FORMAT_VERSION,
                Map.of("bottom", true, "middle", true, "top", true),
                List.of("bottom", "middle", "top"),
                List.of()
        );
        PackSelectionDraft draft = new PackSelectionDraft(
                baseline,
                Set.of("bottom", "required", "middle", "top"),
                Set.of("bottom", "required", "middle", "top"),
                Set.of("required")
        );

        assertEquals(
                List.of("top", "middle", "bottom", "required"),
                draft.selectedIdsByPriority()
        );
        assertFalse(draft.canMoveByPriority("bottom", 1));
        assertFalse(draft.canMoveByPriority("top", -1));
        assertFalse(draft.canMoveByPriority("required", 1));
        assertTrue(draft.canMoveByPriority("top", 1));
        assertTrue(draft.canMoveByPriority("middle", -1));

        List<String> before = draft.selectedIdsByPriority();
        draft.moveByPriority("bottom", 1);
        draft.moveByPriority("required", -1);
        assertEquals(before, draft.selectedIdsByPriority());
    }
}
