package cn.kycraft.kuayue.packs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Strict instance config loader/writer. A missing file uses defaults and is not created.
 */
public final class PackConfigStore {
    private PackConfigStore() {
    }

    public static LoadResult load(Path path) {
        return load(path, PackLimits.defaults());
    }

    public static LoadResult load(Path path, PackLimits limits) {
        if (!Files.exists(path)) {
            return new LoadResult(
                    new PackConfigDocument("{}", PackConfig.defaults()), List.of());
        }
        try {
            if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)) {
                return error(PackDiagnosticCodes.CONFIG_INVALID,
                        "Configuration path is not a regular file");
            }
            long size = Files.size(path);
            if (size > limits.jsonFileBytes()) {
                return error(PackDiagnosticCodes.LIMIT_EXCEEDED,
                        "Configuration exceeds the JSON size limit");
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return PackConfigCodec.parse(json);
        } catch (PackParseException exception) {
            return new LoadResult(
                    new PackConfigDocument("{}", PackConfig.defaults()),
                    List.of(PackDiagnostic.error(
                            exception.code(),
                            PackStage.CONFIG,
                            null,
                            "config",
                            "kuayue/config.json",
                            exception.jsonPointer(),
                            exception.getMessage())));
        } catch (IllegalArgumentException | IOException exception) {
            return error(PackDiagnosticCodes.CONFIG_INVALID,
                    "Invalid kuayue/config.json: " + exception.getMessage());
        }
    }

    public static void write(Path path, PackConfigDocument document) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Configuration path has no parent: " + path);
        }
        Files.createDirectories(parent);
        String json = document.toJsonText() + System.lineSeparator();
        Path temporary = Files.createTempFile(parent, ".kuayue-config-", ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static LoadResult error(String code, String message) {
        PackDiagnostic diagnostic = PackDiagnostic.error(
                code, PackStage.CONFIG, null, "config",
                "kuayue/config.json", "", message);
        return new LoadResult(
                new PackConfigDocument("{}", PackConfig.defaults()),
                List.of(diagnostic));
    }

    public record LoadResult(
            PackConfigDocument document,
            List<PackDiagnostic> diagnostics
    ) {
        public LoadResult {
            diagnostics = List.copyOf(new ArrayList<>(diagnostics))
                    .stream().sorted().toList();
        }

        public boolean hasBlockingErrors() {
            return diagnostics.stream().anyMatch(PackDiagnostic::blocking);
        }
    }
}
