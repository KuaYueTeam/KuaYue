package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.Objects;

/**
 * One logical file in a discovered pack.
 */
public record PackFileEntry(
        String physicalPath,
        String logicalPath,
        long size,
        PackRootKind rootKind,
        String parcelId
) implements Serializable, Comparable<PackFileEntry> {
    public PackFileEntry {
        if (!PackPathRules.isSafeRelativePath(physicalPath)) {
            throw new IllegalArgumentException("Unsafe physical path: " + physicalPath);
        }
        if (!PackPathRules.isSafeRelativePath(logicalPath)) {
            throw new IllegalArgumentException("Unsafe logical path: " + logicalPath);
        }
        if (size < 0) {
            throw new IllegalArgumentException("File size must be non-negative");
        }
        Objects.requireNonNull(rootKind, "rootKind");
    }

    @Override
    public int compareTo(PackFileEntry other) {
        int result = logicalPath.compareTo(other.logicalPath);
        return result != 0 ? result : physicalPath.compareTo(other.physicalPath);
    }
}
