package cn.kycraft.kuayue.packs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts low-to-high pack priority into NeoForge pack registration order.
 */
public final class PackResourceOrder {
    private PackResourceOrder() {
    }

    public static <T> List<T> registrationOrder(List<T> priorityOrder) {
        Objects.requireNonNull(priorityOrder, "priorityOrder");
        List<T> reversed = new ArrayList<>(priorityOrder.size());
        for (int index = priorityOrder.size() - 1; index >= 0; index--) {
            reversed.add(priorityOrder.get(index));
        }
        return List.copyOf(reversed);
    }
}
