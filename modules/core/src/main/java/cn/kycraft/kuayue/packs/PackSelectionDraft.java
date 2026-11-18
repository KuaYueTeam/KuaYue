package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Pure next-start pack selection draft used by the settings screen. */
public final class PackSelectionDraft {
    private final PackConfig baseline;
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
        this.selectableIds = Set.copyOf(selectableIds);
        this.lockedIds = Set.copyOf(lockedIds);
        TreeSet<String> universe = new TreeSet<>(knownIds);
        universe.addAll(baseline.inventory());
        universe.addAll(baseline.enabled().keySet());
        universe.addAll(baseline.order());
        LinkedHashSet<String> order = new LinkedHashSet<>(baseline.order());
        order.addAll(universe);
        allOrder = new ArrayList<>(order);
        for (String id : allOrder) {
            if (baseline.isRequestedEnabled(id)) selected.add(id);
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

    public void move(String id, int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("delta must be -1 or 1");
        }
        if (lockedIds.contains(id)) {
            return;
        }
        List<String> visible = selectedIds();
        int index = visible.indexOf(id);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= visible.size()) return;
        int sourceSlot = allOrder.indexOf(id);
        int targetSlot = allOrder.indexOf(visible.get(target));
        Collections.swap(allOrder, sourceSlot, targetSlot);
    }

    /** Moves one selected ID in the user-facing order, where negative means upward. */
    public void moveByPriority(String id, int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("delta must be -1 or 1");
        }
        move(id, -delta);
    }

    public PackConfig toConfig() {
        LinkedHashMap<String, Boolean> enabled = new LinkedHashMap<>(baseline.enabled());
        for (String id : touched) enabled.put(id, selected.contains(id));
        return new PackConfig(
                baseline.formatVersion(),
                enabled,
                allOrder,
                baseline.inventory()
        );
    }
}
