package cn.kycraft.kuayue.packs.state;

import cn.kycraft.kuayue.packs.PackConfig;
import cn.kycraft.kuayue.packs.PackManifest;

import java.util.Objects;
import java.util.Optional;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Frozen instance-level pack state shared by registration and resource setup.
 */
public final class PackRuntimeState {
    private static volatile PackConfig config;
    private static volatile Set<String> registeredPackIds = Set.of();
    private static volatile boolean registrationSelectionFinalized;

    public static synchronized void install(PackConfig snapshot) {
        config = Objects.requireNonNull(snapshot, "snapshot");
        registeredPackIds = Set.of();
        registrationSelectionFinalized = false;
    }

    public static synchronized void install(
            PackConfig snapshot,
            Collection<String> activePackIds
    ) {
        config = Objects.requireNonNull(snapshot, "snapshot");
        updateRegistrationSelection(activePackIds);
    }

    public static Optional<PackConfig> current() {
        return Optional.ofNullable(config);
    }

    public static boolean corepackEnabled() {
        PackConfig current = config;
        return current == null || current.isRequestedEnabled(PackManifest.CORE_PACK_ID);
    }

    public static Set<String> registeredPackIds() {
        return registeredPackIds;
    }

    public static boolean isRegistrationSelectionFinalized() {
        return registrationSelectionFinalized;
    }

    public static synchronized void updateRegistrationSelection(
            Collection<String> activePackIds
    ) {
        Objects.requireNonNull(activePackIds, "activePackIds");
        registeredPackIds = Set.copyOf(new LinkedHashSet<>(activePackIds));
        registrationSelectionFinalized = true;
    }

    public static synchronized void resetForTests() {
        config = null;
        registeredPackIds = Set.of();
        registrationSelectionFinalized = false;
    }

    private PackRuntimeState() {
    }
}
