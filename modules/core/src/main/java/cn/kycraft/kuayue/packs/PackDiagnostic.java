package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record PackDiagnostic(
        String code,
        PackSeverity severity,
        PackStage stage,
        String ownerId,
        String sourceId,
        String parcelId,
        String logicalPath,
        String jsonPointer,
        List<String> relatedIds,
        String message
) implements Serializable, Comparable<PackDiagnostic> {
    private static final Comparator<String> NULLABLE_STRING =
            Comparator.nullsFirst(String::compareTo);
    private static final Comparator<PackDiagnostic> ORDER =
            Comparator.comparing(PackDiagnostic::severity, Comparator.reverseOrder())
                    .thenComparing(PackDiagnostic::stage)
                    .thenComparing(PackDiagnostic::code)
                    .thenComparing(PackDiagnostic::ownerId, NULLABLE_STRING)
                    .thenComparing(PackDiagnostic::sourceId, NULLABLE_STRING)
                    .thenComparing(PackDiagnostic::parcelId, NULLABLE_STRING)
                    .thenComparing(PackDiagnostic::logicalPath, NULLABLE_STRING)
                    .thenComparing(PackDiagnostic::jsonPointer, NULLABLE_STRING)
                    .thenComparing(PackDiagnostic::message);

    public PackDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(message, "message");
        relatedIds = relatedIds == null
                ? List.of()
                : List.copyOf(new ArrayList<>(relatedIds));
    }

    public static PackDiagnostic error(
            String code,
            PackStage stage,
            String ownerId,
            String sourceId,
            String logicalPath,
            String pointer,
            String message
    ) {
        return new PackDiagnostic(
                code, PackSeverity.ERROR, stage, ownerId, sourceId, null,
                logicalPath, pointer, List.of(), message);
    }

    public static PackDiagnostic warning(
            String code,
            PackStage stage,
            String ownerId,
            String sourceId,
            String logicalPath,
            String pointer,
            String message
    ) {
        return new PackDiagnostic(
                code, PackSeverity.WARNING, stage, ownerId, sourceId, null,
                logicalPath, pointer, List.of(), message);
    }

    public PackDiagnostic withParcelId(String newParcelId) {
        return new PackDiagnostic(
                code, severity, stage, ownerId, sourceId, newParcelId,
                logicalPath, jsonPointer, relatedIds, message);
    }

    public PackDiagnostic withRelatedIds(List<String> ids) {
        return new PackDiagnostic(
                code, severity, stage, ownerId, sourceId, parcelId,
                logicalPath, jsonPointer, ids, message);
    }

    public boolean blocking() {
        return severity == PackSeverity.ERROR;
    }

    @Override
    public int compareTo(PackDiagnostic other) {
        return ORDER.compare(this, other);
    }
}
