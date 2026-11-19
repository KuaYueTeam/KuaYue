package cn.kycraft.kuayue.packs.history;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Minecraft-independent world history value used by comparisons.
 */
public record PackHistoryState(
        boolean initialized,
        List<PackHistoryEntry> packs
) {
    public static final String DATA_NAME = "kuayue_packs";

    public PackHistoryState {
        Objects.requireNonNull(packs, "packs");
        packs = List.copyOf(packs);
        if (!initialized && !packs.isEmpty()) {
            throw new IllegalArgumentException("Missing history cannot contain packs");
        }

        Set<String> ids = new HashSet<>();
        for (PackHistoryEntry pack : packs) {
            Objects.requireNonNull(pack, "pack");
            if (!ids.add(pack.id())) {
                throw new IllegalArgumentException(
                        "Duplicate pack ID in world history: " + pack.id());
            }
        }
    }

    public static PackHistoryState missing() {
        return new PackHistoryState(false, List.of());
    }

    public static PackHistoryState known(List<PackHistoryEntry> packs) {
        return new PackHistoryState(true, new ArrayList<>(packs));
    }
}
