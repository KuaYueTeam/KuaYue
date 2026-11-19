package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Pure next-start pack selection draft used by the settings screen. */
public final class PackSelectionDraft {
    private final PackConfig baseline;
    private final Set<String> knownIds;
    private final Set<String> selectableIds;
    private final Set<String> lockedIds;
    private final List<String> allOrder;
    private final Set<String> selected = new LinkedHashSet<>();
    private final Set<String> touched = new LinkedHashSet<>();

    public PackSelectionDraft(
            PackConfig baseline,
            Set<String> knownIds,
            Set<String> selectableIds
    ) {
        this(baseline, knownIds, selectableIds, Set.of());
    }

    public PackSelectionDraft(
            PackConfig baseline,
            Set<String> knownIds,
            Set<String> selectableIds,
            Set<String> lockedIds
    ) {
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.knownIds = Set.copyOf(knownIds);
        this.selectableIds = Set.copyOf(selectableIds);
        this.lockedIds = Set.copyOf(lockedIds);
        TreeSet<String> universe = new TreeSet<>(knownIds);
        universe.addAll(baseline.inventory());
        universe.addAll(baseline.enabled().keySet());
        universe.addAll(baseline.order());
        LinkedHashSet<String> order = new LinkedHashSet<>();
        new TreeSet<>(lockedIds).forEach(order::add);
        baseline.order().stream()
                .filter(id -> !lockedIds.contains(id))
                .forEach(order::add);
        order.addAll(universe);
        allOrder = new ArrayList<>(order);
        selected.addAll(lockedIds.stream().filter(knownIds::contains).toList());
        for (String id : allOrder) {
            if (knownIds.contains(id) && baseline.isRequestedEnabled(id)) {
                selected.add(id);
            }
        }
    }

    public List<String> selectedIds() {
        return allOrder.stream().filter(selected::contains).toList();
    }

    /** Selected IDs in the user-facing order: highest priority first. */
    public List<String> selectedIdsByPriority() {
        return selectedIds().reversed();
    }

    public List<String> availableIds() {
        return allOrder.stream()
                .filter(id -> !selected.contains(id))
                .filter(selectableIds::contains)
                .toList();
    }

    public boolean isSelectable(String id) {
        return selectableIds.contains(id);
    }

    public boolean isLocked(String id) {
        return lockedIds.contains(id);
    }

    public void add(String id) {
        if (!selectableIds.contains(id) || !allOrder.contains(id)) {
            throw new IllegalArgumentException("Not selectable: " + id);
        }
        if (selected.add(id)) {
            touched.add(id);
            allOrder.remove(id);
            allOrder.add(id);
        }
    }

    public void remove(String id) {
        if (lockedIds.contains(id)) {
            return;
        }
        if (selected.remove(id)) {
            touched.add(id);
        }
    }

    /**
     * Disables one selected pack and every enabled pack that depends on it.
     */
    public List<String> removeWithEnabledDependents(
            String id,
            Map<String, PackManifest> knownManifests
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(knownManifests, "knownManifests");
        if (!selected.contains(id) || lockedIds.contains(id)) {
            return List.of();
        }

        Set<String> dependents = PackDependencyCheck.enabledDependents(
                id,
                knownManifests,
                Set.copyOf(selected)
        );
        List<String> removalOrder = new ArrayList<>();
        removalOrder.add(id);
        selectedIdsByPriority().stream()
                .filter(dependents::contains)
                .forEach(removalOrder::add);
        Set<String> before = Set.copyOf(selected);
        removalOrder.forEach(this::remove);
        return removalOrder.stream()
                .filter(before::contains)
                .filter(candidate -> !selected.contains(candidate))
                .toList();
    }

    public void move(String id, int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("delta must be -1 or 1");
        }
        if (!canMove(id, delta)) return;
        List<String> visible = selectedIds();
        int index = visible.indexOf(id);
        int target = index + delta;
        int sourceSlot = allOrder.indexOf(id);
        int targetSlot = allOrder.indexOf(visible.get(target));
        Collections.swap(allOrder, sourceSlot, targetSlot);
    }

    /** Returns whether moving in low-to-high priority order can cross into the target slot. */
    public boolean canMove(String id, int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("delta must be -1 or 1");
        }
        if (lockedIds.contains(id)) {
            return false;
        }
        List<String> visible = selectedIds();
        int index = visible.indexOf(id);
        int target = index + delta;
        return index >= 0
                && target >= 0
                && target < visible.size()
                && !lockedIds.contains(visible.get(target));
    }

    /** Moves one selected ID in the user-facing order, where negative means upward. */
    public void moveByPriority(String id, int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("delta must be -1 or 1");
        }
        move(id, -delta);
    }

    /** Returns whether a pack can move in the user-facing high-to-low priority order. */
    public boolean canMoveByPriority(String id, int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("delta must be -1 or 1");
        }
        return canMove(id, -delta);
    }

    public PackConfig toConfig() {
        LinkedHashMap<String, Boolean> enabled = new LinkedHashMap<>(baseline.enabled());
        enabled.keySet().removeAll(lockedIds);
        for (String id : touched) {
            if (!lockedIds.contains(id)) {
                enabled.put(id, selected.contains(id));
            }
        }
        return new PackConfig(
                baseline.formatVersion(),
                enabled,
                allOrder.stream().filter(id -> !lockedIds.contains(id)).toList(),
                baseline.inventory()
        );
    }
}
