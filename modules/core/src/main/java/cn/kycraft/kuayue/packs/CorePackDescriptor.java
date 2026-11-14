package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public record CorePackDescriptor(
        SemanticVersion version,
        List<String> namespaces
) implements Serializable {
    public CorePackDescriptor {
        Objects.requireNonNull(version, "version");
        namespaces = List.copyOf(namespaces);
        if (namespaces.isEmpty()) {
            throw new IllegalArgumentException("Corepack must provide at least one namespace");
        }
        for (String namespace : namespaces) {
            if (!PackPathRules.isNamespace(namespace)) {
                throw new IllegalArgumentException("Invalid corepack namespace: " + namespace);
            }
        }
    }

    public String id() {
        return PackManifest.CORE_PACK_ID;
    }

    public static CorePackDescriptor forVersion(String version) {
        return new CorePackDescriptor(SemanticVersion.parse(version), List.of("kuayue"));
    }
}
