package cn.kycraft.kuayue.packs;

import java.util.Objects;

/**
 * Preserves unrelated top-level config data while exposing the strict packs subdomain.
 */
public final class PackConfigDocument {
    private final String originalRootJson;
    private final PackConfig config;

    PackConfigDocument(String originalRootJson, PackConfig config) {
        this.originalRootJson = Objects.requireNonNull(originalRootJson, "originalRootJson");
        this.config = Objects.requireNonNull(config, "config");
    }

    public PackConfig config() {
        return config;
    }

    public PackConfigDocument withConfig(PackConfig replacement) {
        return new PackConfigDocument(originalRootJson, replacement);
    }

    String toJsonText() {
        return PackConfigCodec.render(this);
    }

    String originalRootJson() {
        return originalRootJson;
    }
}
