package cn.kycraft.kuayue.parts.core.multiblock;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Placement-only rules shared by footprints and the structure manager.
 */
final class MultiCellPlacementPolicy {
    private MultiCellPlacementPolicy() {
    }

    record Cell(int x, int y, int z) {
    }

    static Set<Cell> targetPositions(Cell anchor, Collection<Cell> relativeCells) {
        Set<Cell> targets = new LinkedHashSet<>();
        targets.add(anchor);
        for (Cell cell : relativeCells) {
            targets.add(new Cell(
                    anchor.x() + cell.x(),
                    anchor.y() + cell.y(),
                    anchor.z() + cell.z()
            ));
        }
        return Set.copyOf(targets);
    }

    static <T> boolean canPlace(
            T anchor,
            Collection<? extends T> targets,
            Predicate<? super T> replaceable,
            Function<? super T, ? extends T> componentOwner
    ) {
        for (T target : targets) {
            T owner = componentOwner.apply(target);
            if (owner != null) {
                if (!anchor.equals(owner)) {
                    return false;
                }
                continue;
            }
            if (!replaceable.test(target)) {
                return false;
            }
        }
        return true;
    }
}
