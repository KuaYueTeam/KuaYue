package cn.kycraft.kuayue.packs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class PackStartupReportCodec {
    private PackStartupReportCodec() {
    }

    static String encode(PackStartupReport report) {
        JsonValue root = JsonValue.object();
        root.put("format_version", report.formatVersion());

        JsonValue environment = JsonValue.object();
        report.environment().forEach(environment::put);
        root.put("environment", environment);

        JsonValue installations = JsonValue.array();
        for (PackInstallationStatus installation : report.installations()) {
            JsonValue object = JsonValue.object();
            object.put("source_id", installation.sourceId());
            object.put("source_kind", installation.sourceKind().name().toLowerCase());
            object.put("container_name", installation.containerName());
            object.put("pack_id", installation.packId());
            object.put("pack_version",
                    installation.packVersion() == null
                            ? null : installation.packVersion().toString());
            object.put("requested_enabled", installation.requestedEnabled());
            object.put("effective", installation.effective());
            object.put("valid", installation.valid());
            JsonValue codes = JsonValue.array();
            installation.diagnosticCodes().forEach(codes::add);
            object.put("diagnostic_codes", codes);
            installations.add(object);
        }
        root.put("installations", installations);

        JsonValue effective = JsonValue.array();
        for (ResolvedPack pack : report.effectivePacks()) {
            JsonValue object = JsonValue.object();
            object.put("id", pack.id());
            object.put("version", pack.version().toString());
            object.put("source_kind", pack.sourceKind().name().toLowerCase());
            object.put("source_id", pack.sourceId());
            effective.add(object);
        }
        root.put("effective_packs", effective);

        JsonValue diagnostics = JsonValue.array();
        for (PackDiagnostic diagnostic : report.diagnostics()) {
            JsonValue object = JsonValue.object();
            object.put("code", diagnostic.code());
            object.put("severity", diagnostic.severity().name().toLowerCase());
            object.put("stage", diagnostic.stage().name().toLowerCase());
            object.put("owner_id", diagnostic.ownerId());
            object.put("source_id", diagnostic.sourceId());
            object.put("parcel_id", diagnostic.parcelId());
            object.put("logical_path", diagnostic.logicalPath());
            object.put("json_pointer", diagnostic.jsonPointer());
            JsonValue relatedIds = JsonValue.array();
            diagnostic.relatedIds().forEach(relatedIds::add);
            object.put("related_ids", relatedIds);
            object.put("message", diagnostic.message());
            diagnostics.add(object);
        }
        root.put("diagnostics", diagnostics);
        return StrictJsonEngine.toJson(root);
    }

    static void write(Path path, PackStartupReport report) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Report path has no parent: " + path);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".kuayue-pack-report-", ".tmp");
        try {
            Files.writeString(
                    temporary, encode(report) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
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
}
