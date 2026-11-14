package cn.kycraft.kuayue.parts.bogeys.data;

import java.util.List;

/**
 * Validation helpers shared by the data-driven bogey schema records.
 */
final class BogeySchemaStrings {
    private BogeySchemaStrings() {
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    static String requireId(String value, String field) {
        requireText(value, field);
        String[] parts = value.split(":", -1);
        if (parts.length != 2
                || !parts[0].matches("[a-z0-9_.-]+")
                || !parts[1].matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(
                    field + " must be a namespace-qualified resource location: " + value
            );
        }
        return value;
    }

    static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must be present");
        }
        return value;
    }

    static List<String> immutableIds(List<String> values, String field) {
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            requireId(value, field);
        }
        return List.copyOf(values);
    }
}
