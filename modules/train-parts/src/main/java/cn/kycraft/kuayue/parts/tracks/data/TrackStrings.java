package cn.kycraft.kuayue.parts.tracks.data;

/**
 * Shared validation helpers for track schema records.
 *
 * <p>The helpers avoid Minecraft classes so schema validation can run in a plain JVM test.</p>
 */
final class TrackStrings {
    private TrackStrings() {
    }

    /**
     * Requires a non-null, non-blank string.
     *
     * @param value value to validate
     * @param field schema field name used in the error message
     * @return the validated value
     * @throws IllegalArgumentException when the value is missing or blank
     */
    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /**
     * Requires a syntactically valid namespace-qualified resource location.
     */
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

    /**
     * Requires a non-null value.
     *
     * @param value value to validate
     * @param field schema field name used in the error message
     * @param <T>   value type
     * @return the validated value
     * @throws IllegalArgumentException when the value is {@code null}
     */
    static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must be present");
        }
        return value;
    }
}
