package cn.kycraft.kuayue.packs.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

/**
 * Removes the materialized pack cache used by the superseded resource adapter.
 */
public final class LegacyPackCacheCleaner {
    private LegacyPackCacheCleaner() {
    }

    public static void clear(Path cacheRoot) throws IOException {
        Objects.requireNonNull(cacheRoot, "cacheRoot");
        if (!Files.exists(cacheRoot)) {
            return;
        }
        ArrayList<Path> paths = new ArrayList<>();
        try (var stream = Files.walk(cacheRoot)) {
            stream.sorted(Comparator.reverseOrder()).forEach(paths::add);
        }
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
