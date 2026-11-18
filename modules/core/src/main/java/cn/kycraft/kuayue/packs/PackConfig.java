package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Instance-level pack switches, resource order, and installation inventory.
 */
public record PackConfig(
        int formatVersion,
        Map<String, Boolean> enabled,
        List<String> order,
        List<String> inventory
) implements Serializable {
    public static final int FORMAT_VERSION = 1;

    public PackConfig {
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported config format: " + formatVersion);
        }
        Objects.requireNonNull(enabled, "enabled");
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(inventory, "inventory");
        LinkedHashMap<String, Boolean> copiedEnabled = new LinkedHashMap<>();
        for (var entry : enabled.entrySet()) {
            validateConfigId(entry.getKey(), "enabled");
            copiedEnabled.put(entry.getKey(), Objects.requireNonNull(entry.getValue(), "enabled value"));
        }
        enabled = Collections.unmodifiableMap(copiedEnabled);
        order = immutableIds(order, "order");
        inventory = immutableIds(inventory, "inventory");
    }

    public static PackConfig defaults() {
        return new PackConfig(FORMAT_VERSION, Map.of(), List.of(), List.of());
    }

    public ConfigCompletion complete(Set<String> installedExternalIds) {
        return complete(installedExternalIds, Set.of(), Set.of());
    }

    public ConfigCompletion complete(
            Set<String> installedExternalIds,
            Set<String> builtInIds,
            Set<String> requiredIds
    ) {
        Objects.requireNonNull(installedExternalIds, "installedExternalIds");
        Objects.requireNonNull(builtInIds, "builtInIds");
        Objects.requireNonNull(requiredIds, "requiredIds");
        TreeSet<String> sortedInstalled = new TreeSet<>();
        for (String id : installedExternalIds) {
            validateConfigId(id, "installed");
            sortedInstalled.add(id);
        }
        TreeSet<String> sortedBuiltIn = new TreeSet<>();
        for (String id : builtInIds) {
            validateConfigId(id, "built-in");
            sortedBuiltIn.add(id);
        }
        TreeSet<String> sortedRequired = new TreeSet<>();
        for (String id : requiredIds) {
            validateConfigId(id, "required");
            if (!sortedBuiltIn.contains(id)) {
                throw new IllegalArgumentException(
                        "Required pack is not a built-in pack: " + id);
            }
            sortedRequired.add(id);
        }

        LinkedHashMap<String, Boolean> completedEnabled = new LinkedHashMap<>(enabled);
        sortedBuiltIn.forEach(id -> completedEnabled.putIfAbsent(id, true));
        sortedRequired.forEach(id -> completedEnabled.put(id, true));

        LinkedHashSet<String> completedOrder = new LinkedHashSet<>(sortedRequired);
        order.stream()
                .filter(id -> !sortedRequired.contains(id))
                .forEach(completedOrder::add);
        completedOrder.addAll(sortedBuiltIn);
        completedOrder.addAll(sortedInstalled);

        TreeSet<String> externalOnly = new TreeSet<>(sortedInstalled);
        externalOnly.removeAll(sortedBuiltIn);
        List<String> completedInventory = List.copyOf(externalOnly);
        PackConfig completed = new PackConfig(
                FORMAT_VERSION,
                completedEnabled,
                List.copyOf(completedOrder),
                completedInventory);

        Set<String> installed = new TreeSet<>(sortedInstalled);
        installed.addAll(sortedBuiltIn);
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        for (String id : completedEnabled.keySet()) {
            if (!installed.contains(id)) {
                diagnostics.add(PackDiagnostic.warning(
                        PackDiagnosticCodes.UNKNOWN_PACK_ID,
                        PackStage.CONFIG,
                        id,
                        "config",
                        "kuayue/config.json",
                        "/packs/enabled/" + JsonProtocol.escape(id),
                        "Enabled pack ID is not installed"));
            }
        }
        for (String id : completedOrder) {
            if (!installed.contains(id)) {
                diagnostics.add(PackDiagnostic.warning(
                        PackDiagnosticCodes.UNKNOWN_PACK_ID,
                        PackStage.CONFIG,
                        id,
                        "config",
                        "kuayue/config.json",
                        "/packs/order",
                        "Order contains an ID that is not installed"));
            }
        }
        diagnostics.sort(PackDiagnostic::compareTo);
        return new ConfigCompletion(completed, diagnostics);
    }

    public boolean isRequestedEnabled(String id) {
        return enabled.getOrDefault(id, false);
    }

    private static List<String> immutableIds(List<String> values, String name) {
        List<String> copy = new ArrayList<>(values.size());
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            validateConfigId(value, name);
            if (unique.add(value)) {
                copy.add(value);
            }
        }
        return List.copyOf(copy);
    }

    private static void validateConfigId(String value, String field) {
        if (!PackPathRules.isPackId(value)) {
            throw new IllegalArgumentException("Invalid pack ID in " + field + ": " + value);
        }
    }

    public record ConfigCompletion(PackConfig config, List<PackDiagnostic> diagnostics)
            implements Serializable {
        public ConfigCompletion {
            Objects.requireNonNull(config, "config");
            diagnostics = List.copyOf(new ArrayList<>(diagnostics));
        }
    }
}
