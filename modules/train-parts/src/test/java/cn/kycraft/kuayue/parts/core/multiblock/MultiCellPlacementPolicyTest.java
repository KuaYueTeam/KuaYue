package cn.kycraft.kuayue.parts.core.multiblock;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiCellPlacementPolicyTest {
    private static final MultiCellPlacementPolicy.Cell ANCHOR =
            new MultiCellPlacementPolicy.Cell(8, 64, -3);

    @Test
    void targetPositionsIncludeTheAnchorAndEveryCompanion() {
        List<MultiCellPlacementPolicy.Cell> offsets = List.of(
                cell(1, 0, 0),
                cell(0, 0, 1)
        );

        assertEquals(
                cells(
                        ANCHOR,
                        offset(ANCHOR, offsets.get(0)),
                        offset(ANCHOR, offsets.get(1))
                ),
                MultiCellPlacementPolicy.targetPositions(ANCHOR, offsets)
        );
    }

    @Test
    void rejectsAPartialOverlapWithoutIncludingTheAnchor() {
        List<MultiCellPlacementPolicy.Cell> offsets = List.of(
                cell(1, 0, 0),
                cell(0, 0, 1)
        );
        Set<MultiCellPlacementPolicy.Cell> targets =
                MultiCellPlacementPolicy.targetPositions(ANCHOR, offsets);
        Set<MultiCellPlacementPolicy.Cell> replaceable = new HashSet<>(targets);
        MultiCellPlacementPolicy.Cell overlappingCell = offset(ANCHOR, offsets.getFirst());
        Map<MultiCellPlacementPolicy.Cell, MultiCellPlacementPolicy.Cell> owners = Map.of(
                overlappingCell,
                offset(ANCHOR, cell(4, 0, 0))
        );

        assertFalse(MultiCellPlacementPolicy.canPlace(
                ANCHOR,
                targets,
                replaceable::contains,
                owners::get
        ));
    }

    @Test
    void rejectsADifferentComponentInTheAnchorCell() {
        Set<MultiCellPlacementPolicy.Cell> targets =
                MultiCellPlacementPolicy.targetPositions(ANCHOR, List.of(cell(1, 0, 0)));
        Set<MultiCellPlacementPolicy.Cell> replaceable = new HashSet<>(targets);

        assertFalse(MultiCellPlacementPolicy.canPlace(
                ANCHOR,
                targets,
                replaceable::contains,
                pos -> pos.equals(ANCHOR) ? cell(ANCHOR.x(), ANCHOR.y() + 1, ANCHOR.z()) : null
        ));
    }

    @Test
    void allowsExistingCellsOwnedByTheSameAnchorDuringStateRevalidation() {
        Set<MultiCellPlacementPolicy.Cell> targets =
                MultiCellPlacementPolicy.targetPositions(
                        ANCHOR,
                        List.of(cell(1, 0, 0), cell(0, 0, 1))
                );
        Set<MultiCellPlacementPolicy.Cell> replaceable = Set.of(ANCHOR);

        assertTrue(MultiCellPlacementPolicy.canPlace(
                ANCHOR,
                targets,
                replaceable::contains,
                pos -> pos.equals(ANCHOR) ? null : ANCHOR
        ));
    }

    @Test
    void allowsNonOverlappingAdjacency() {
        List<MultiCellPlacementPolicy.Cell> offsets = List.of(
                cell(1, 0, 0),
                cell(0, 0, 1)
        );
        Set<MultiCellPlacementPolicy.Cell> targets =
                MultiCellPlacementPolicy.targetPositions(ANCHOR, offsets);
        Set<MultiCellPlacementPolicy.Cell> replaceable = new HashSet<>(targets);
        Map<MultiCellPlacementPolicy.Cell, MultiCellPlacementPolicy.Cell> owners = Map.of(
                offset(ANCHOR, cell(1, 0, 1)),
                offset(ANCHOR, cell(3, 0, 3))
        );

        assertTrue(MultiCellPlacementPolicy.canPlace(
                ANCHOR,
                targets,
                replaceable::contains,
                owners::get
        ));
    }

    private static MultiCellPlacementPolicy.Cell offset(
            MultiCellPlacementPolicy.Cell anchor,
            MultiCellPlacementPolicy.Cell offset
    ) {
        return cell(
                anchor.x() + offset.x(),
                anchor.y() + offset.y(),
                anchor.z() + offset.z()
        );
    }

    private static MultiCellPlacementPolicy.Cell cell(int x, int y, int z) {
        return new MultiCellPlacementPolicy.Cell(x, y, z);
    }

    private static Set<MultiCellPlacementPolicy.Cell> cells(
            MultiCellPlacementPolicy.Cell... cells
    ) {
        return Set.of(cells);
    }
}
