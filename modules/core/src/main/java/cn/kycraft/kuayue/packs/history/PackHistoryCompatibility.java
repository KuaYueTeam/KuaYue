package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.PackDiagnostic;
import cn.kycraft.kuayue.packs.PackDiagnosticCodes;
import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.PackPathRules;
import cn.kycraft.kuayue.packs.PackStage;
import cn.kycraft.kuayue.packs.SaveCompatibility;
import cn.kycraft.kuayue.packs.SemanticVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure comparison between persisted world history and one startup pack snapshot.
 */
public final class PackHistoryCompatibility {
    public enum Outcome {
        MISSING,
        COMPATIBLE,
        INCOMPATIBLE
    }

    private PackHistoryCompatibility() {
    }

    public static Result compare(
            PackHistoryState history,
            Collection<CurrentPack> currentPacks
    ) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(currentPacks, "currentPacks");

        if (!history.initialized()) {
            return new Result(
                    Outcome.MISSING,
                    List.of(error(
                            PackHistoryDiagnosticCodes.HISTORY_MISSING,
                            null,
                            "The world has no initialized KuaYue pack history")));
        }

        List<CurrentPack> current = List.copyOf(currentPacks);
        Map<String, CurrentPack> currentById = new LinkedHashMap<>();
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        Set<String> duplicateIds = new HashSet<>();
        for (CurrentPack pack : current) {
            Objects.requireNonNull(pack, "currentPack");
            if (currentById.putIfAbsent(pack.id(), pack) != null && duplicateIds.add(pack.id())) {
                diagnostics.add(error(
                        PackDiagnosticCodes.DUPLICATE_PACK_ID,
                        pack.id(),
                        "Duplicate pack ID in current startup snapshot: " + pack.id()));
            }
        }

        for (PackHistoryEntry historical : history.packs()) {
            CurrentPack active = currentById.get(historical.id());
            if (active == null) {
                diagnostics.add(error(
                        PackDiagnosticCodes.UNKNOWN_PACK_ID,
                        historical.id(),
                        "Pack '" + historical.id()
                                + "' was present in the world history but is not enabled now"));
                continue;
            }

            int comparison = historical.version().compareTo(active.version());
            if (comparison > 0) {
                diagnostics.add(error(
                        PackHistoryDiagnosticCodes.SAVE_VERSION_DOWNGRADE,
                        historical.id(),
                        "Pack '" + historical.id() + "' would be downgraded from "
                                + historical.version() + " to " + active.version()));
                continue;
            }
            if (comparison < 0
                    && (active.saveCompatibility() == null
                    || !active.saveCompatibility().allowsUpgradeFrom(
                    historical.version(), active.version()))) {
                diagnostics.add(error(
                        PackDiagnosticCodes.SAVE_VERSION_INCOMPATIBLE,
                        historical.id(),
                        "Pack '" + historical.id() + "' version " + historical.version()
                                + " cannot upgrade directly to " + active.version()));
            }
        }

        diagnostics.sort(PackDiagnostic::compareTo);
        Outcome outcome = diagnostics.stream().anyMatch(PackDiagnostic::blocking)
                ? Outcome.INCOMPATIBLE
                : Outcome.COMPATIBLE;
        return new Result(outcome, diagnostics);
    }

    public static Result compare(
            PackHistorySavedData history,
            Collection<CurrentPack> currentPacks
    ) {
        Objects.requireNonNull(history, "history");
        return compare(history.state(), currentPacks);
    }

    private static PackDiagnostic error(String code, String ownerId, String message) {
        return PackDiagnostic.error(
                code,
                PackStage.COMPATIBILITY,
                ownerId,
                PackHistoryState.DATA_NAME,
                "data/" + PackHistoryState.DATA_NAME + ".dat",
                null,
                message);
    }

    /**
     * One pack in the effective startup snapshot.
     */
    public record CurrentPack(
            String id,
            SemanticVersion version,
            int formatVersion,
            SaveCompatibility saveCompatibility
    ) implements HistoryPack {
        public CurrentPack {
            if (!PackPathRules.isPackId(id)) {
                throw new IllegalArgumentException("Invalid current pack ID: " + id);
            }
            Objects.requireNonNull(version, "version");
            if (formatVersion != PackManifest.FORMAT_VERSION) {
                throw new IllegalArgumentException(
                        "Unsupported current pack format version: " + formatVersion);
            }
        }

        public static CurrentPack from(PackManifest manifest) {
            Objects.requireNonNull(manifest, "manifest");
            return new CurrentPack(
                    manifest.id(),
                    manifest.version(),
                    manifest.formatVersion(),
                    manifest.saveCompatibility());
        }
    }

    /**
     * Missing history has its own outcome because admission policy is unresolved.
     */
    public record Result(Outcome outcome, List<PackDiagnostic> diagnostics) {
        public Result {
            Objects.requireNonNull(outcome, "outcome");
            diagnostics = List.copyOf(diagnostics);
            boolean blocking = diagnostics.stream().anyMatch(PackDiagnostic::blocking);
            if (outcome == Outcome.COMPATIBLE && blocking) {
                throw new IllegalArgumentException(
                        "A compatible result cannot contain blocking diagnostics");
            }
            if (outcome != Outcome.COMPATIBLE && !blocking) {
                throw new IllegalArgumentException(
                        "A non-compatible result requires a blocking diagnostic");
            }
        }

        public boolean compatible() {
            return outcome == Outcome.COMPATIBLE;
        }
    }
}
