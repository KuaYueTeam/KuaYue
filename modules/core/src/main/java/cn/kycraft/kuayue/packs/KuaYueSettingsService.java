package cn.kycraft.kuayue.packs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Data-only settings backend for the client menu. */
public final class KuaYueSettingsService {
    private final Path configPath;
    private final PackLimits limits;

    public KuaYueSettingsService(Path gameDirectory, PackLimits limits) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        this.configPath = gameDirectory.resolve("kuayue").resolve("config.json");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public LoadResult begin(Map<String, PackManifest> knownManifests) {
        Objects.requireNonNull(knownManifests, "knownManifests");
        PackConfigStore.LoadResult loaded = PackConfigStore.load(configPath, limits);
        if (loaded.hasBlockingErrors()) {
            return new LoadResult(null, loaded.diagnostics());
        }
        Set<String> known = Set.copyOf(knownManifests.keySet());
        Set<String> required = Set.copyOf(knownManifests.entrySet().stream()
                .filter(entry -> entry.getValue().required())
                .map(Map.Entry::getKey)
                .toList());
        Set<String> selectable = Set.copyOf(knownManifests.entrySet().stream()
                .filter(entry -> entry.getValue().formatVersion() == PackManifest.FORMAT_VERSION)
                .filter(entry -> !entry.getValue().required())
                .map(Map.Entry::getKey)
                .toList());
        PackSelectionDraft draft = new PackSelectionDraft(
                loaded.document().config(),
                known,
                selectable,
                required
        );
        try {
            return new LoadResult(
                    new EditSession(
                            loaded.document().config(),
                            currentRevision(),
                            knownManifests,
                            draft
                    ),
                    List.of()
            );
        } catch (IOException exception) {
            return new LoadResult(null, List.of(PackDiagnostic.error(
                    PackDiagnosticCodes.IO_ERROR,
                    PackStage.CONFIG,
                    null,
                    "session",
                    "kuayue/config.json",
                    "",
                    "Unable to read configuration revision: " + exception.getMessage()
            )));
        }
    }

    public static List<PackInstallationStatus> unavailableInstallations(
            Set<String> knownPackIds,
            PackStartupReport report
    ) {
        Objects.requireNonNull(knownPackIds, "knownPackIds");
        Objects.requireNonNull(report, "report");
        return report.installations().stream()
                .filter(installation -> installation.packId() == null
                        || !knownPackIds.contains(installation.packId()))
                .toList();
    }

    public SaveResult save(EditSession session, PackSelectionDraft draft) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(draft, "draft");
        String expectedRevision = null;
        try {
            String current = currentRevision();
            PackConfigStore.LoadResult fresh = PackConfigStore.load(configPath, limits);
            if (fresh.hasBlockingErrors()
                    || !fresh.document().config().equals(session.baseline())
                    || !currentRevision().equals(current)) {
                return SaveResult.failed(
                        SavePhase.PRE_COMMIT,
                        PackDiagnosticCodes.CONFIG_INVALID,
                        "Configuration no longer matches the edit baseline"
                );
            }
            PackConfig next = draft.toConfig();
            if (next.equals(session.baseline())) {
                return new SaveResult(SavePhase.SAVED, current, null);
            }
            PackConfigDocument document = fresh.document().withConfig(next);
            expectedRevision = revisionOf(document.toJsonText() + System.lineSeparator());
            PackConfigStore.write(configPath, document);
            String written = currentRevision();
            if (!written.equals(expectedRevision)) {
                return SaveResult.failed(
                        SavePhase.POST_COMMIT_UNCONFIRMED,
                        PackDiagnosticCodes.IO_ERROR,
                        "Configuration write completed but the new revision could not be confirmed"
                );
            }
            return new SaveResult(SavePhase.SAVED, written, null);
        } catch (IOException | RuntimeException exception) {
            SavePhase phase = SavePhase.PRE_COMMIT;
            try {
                if (expectedRevision != null
                        && expectedRevision.equals(currentRevision())) {
                    phase = SavePhase.POST_COMMIT_UNCONFIRMED;
                }
            } catch (IOException | RuntimeException ignored) {
            }
            return SaveResult.failed(
                    phase,
                    PackDiagnosticCodes.IO_ERROR,
                    "Unable to save pack configuration: " + exception.getMessage()
            );
        }
    }

    private String currentRevision() throws IOException {
        try {
            if (Files.size(configPath) > limits.jsonFileBytes()) {
                throw new IOException("Configuration exceeds the JSON size limit");
            }
            byte[] bytes = Files.readAllBytes(configPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchFileException ignored) {
            return "missing";
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String revisionOf(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(text.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record EditSession(
            PackConfig baseline,
            String revision,
            Map<String, PackManifest> knownManifests,
            PackSelectionDraft draft
    ) {
        public EditSession {
            Objects.requireNonNull(baseline, "baseline");
            Objects.requireNonNull(revision, "revision");
            knownManifests = Map.copyOf(knownManifests);
            Objects.requireNonNull(draft, "draft");
        }
    }

    public record LoadResult(EditSession session, List<PackDiagnostic> diagnostics) {
        public LoadResult {
            diagnostics = List.copyOf(diagnostics);
        }

        public boolean successful() {
            return session != null && diagnostics.stream().noneMatch(PackDiagnostic::blocking);
        }
    }

    public enum SavePhase {
        SAVED,
        PRE_COMMIT,
        POST_COMMIT_UNCONFIRMED
    }

    public record SaveResult(SavePhase phase, String revision, PackDiagnostic diagnostic) {
        public static SaveResult failed(SavePhase phase, String code, String message) {
            return new SaveResult(
                    phase,
                    null,
                    PackDiagnostic.error(
                            code,
                            PackStage.CONFIG,
                            null,
                            "settings",
                            "kuayue/config.json",
                            "/packs",
                            message
                    )
            );
        }

        public boolean successful() {
            return phase == SavePhase.SAVED;
        }
    }
}
