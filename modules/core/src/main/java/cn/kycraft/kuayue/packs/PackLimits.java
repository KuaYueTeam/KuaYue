package cn.kycraft.kuayue.packs;

import java.io.Serializable;

public record PackLimits(
        long jsonFileBytes,
        long regularFileBytes,
        long packBytes,
        int maxFiles
) implements Serializable {
    public static final long DEFAULT_JSON_FILE_BYTES = 8L * 1024 * 1024;
    public static final long DEFAULT_REGULAR_FILE_BYTES = 256L * 1024 * 1024;
    public static final long DEFAULT_PACK_BYTES = 1024L * 1024 * 1024;
    public static final int DEFAULT_MAX_FILES = 50_000;

    public PackLimits {
        if (jsonFileBytes <= 0 || regularFileBytes <= 0 || packBytes <= 0 || maxFiles <= 0) {
            throw new IllegalArgumentException("All pack limits must be positive");
        }
        if (jsonFileBytes > regularFileBytes) {
            throw new IllegalArgumentException("JSON limit cannot exceed regular file limit");
        }
    }

    public static PackLimits defaults() {
        return new PackLimits(
                DEFAULT_JSON_FILE_BYTES,
                DEFAULT_REGULAR_FILE_BYTES,
                DEFAULT_PACK_BYTES,
                DEFAULT_MAX_FILES);
    }
}
