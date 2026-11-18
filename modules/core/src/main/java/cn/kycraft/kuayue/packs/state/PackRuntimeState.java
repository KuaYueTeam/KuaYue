package cn.kycraft.kuayue.packs.state;

import cn.kycraft.kuayue.packs.PackConfig;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;

import java.util.Objects;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Frozen instance-level pack state shared by registration and resource setup.
 */
public final class PackRuntimeState {
    private static volatile PackConfig config;
    private static volatile Set<String> registeredPackIds = Set.of();
    private static volatile List<RegistryClaim> registrationClaims = List.of();
    private static volatile boolean registrationSelectionFinalized;

    public static synchronized void install(PackConfig snapshot) {
        config = Objects.requireNonNull(snapshot, "snapshot");
        registeredPackIds = Set.of();
        registrationClaims = List.of();
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

    public static Set<String> registeredPackIds() {
        return registeredPackIds;
    }

    public static boolean isRegistrationSelectionFinalized() {
        return registrationSelectionFinalized;
    }

    public static List<RegistryClaim> registrationClaims() {
        return registrationClaims;
    }

    public static synchronized void updateRegistrationSelection(
            Collection<String> activePackIds
    ) {
        updateRegistrationSelection(activePackIds, List.of());
    }

    public static synchronized void updateRegistrationSelection(
            Collection<String> activePackIds,
            Collection<RegistryClaim> claims
    ) {
        Objects.requireNonNull(activePackIds, "activePackIds");
        Objects.requireNonNull(claims, "claims");
        registeredPackIds = Set.copyOf(new LinkedHashSet<>(activePackIds));
        registrationClaims = List.copyOf(claims);
        registrationSelectionFinalized = true;
    }

    public static synchronized void resetForTests() {
        config = null;
        registeredPackIds = Set.of();
        registrationClaims = List.of();
        registrationSelectionFinalized = false;
    }

    private PackRuntimeState() {
    }
}
