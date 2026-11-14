package cn.kycraft.kuayue.packs.bootstrap;

import cn.kycraft.kuayue.packs.PackConfig;
import cn.kycraft.kuayue.packs.PackStartupReport;
import cn.kycraft.kuayue.packs.SemanticVersion;
import cn.kycraft.kuayue.packs.history.PackHistoryCompatibility;
import cn.kycraft.kuayue.packs.runtime.PackResourceSnapshotter;
import lib.kasuga.core.resource.source.ResourceSnapshot;
import lib.kasuga.core.resource.source.ResourceSource;
import lib.kasuga.core.resource.source.ResourceSourceDescriptor;
import lib.kasuga.registration.data_driven.source.ContentSource;
import lib.kasuga.registration.data_driven.source.ContentSourceFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable pack selection and captured resource roots for one process lifetime.
 */
public record StartupPackSnapshot(
        Path gameDirectory,
        ManagedRuntimeRoot runtimeRoot,
        PackConfig config,
        List<ManagedPackRoot> resourceRoots,
        Map<String, Set<String>> dependencies,
        List<PackHistoryCompatibility.CurrentPack> historyPacks,
        PackStartupReport report
) {
    public StartupPackSnapshot {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(runtimeRoot, "runtimeRoot");
        Objects.requireNonNull(config, "config");
        resourceRoots = List.copyOf(new ArrayList<>(resourceRoots));
        dependencies = Map.copyOf(dependencies);
        historyPacks = List.copyOf(new ArrayList<>(historyPacks));
        Objects.requireNonNull(report, "report");
    }

    public List<ManagedPackRoot> externalResourceRoots() {
        return resourceRoots.stream().filter(root -> !root.corepack()).toList();
    }

    public ManagedPackRoot resourceRoot(String packId) {
        return resourceRoots.stream()
                .filter(root -> root.id().equals(packId))
                .findFirst()
                .orElse(null);
    }

    public record ManagedRuntimeRoot(
            ResourceSourceDescriptor descriptor,
            Set<String> namespaces,
            ResourceSource source
    ) implements AutoCloseable {
        public ManagedRuntimeRoot {
            Objects.requireNonNull(descriptor, "descriptor");
            namespaces = Set.copyOf(namespaces);
            if (namespaces.isEmpty()) {
                throw new IllegalArgumentException(
                        "A managed runtime root must declare a namespace");
            }
            Objects.requireNonNull(source, "source");
        }

        public ContentSource contentSource() throws IOException {
            ResourceSnapshot snapshot = source.capture();
            return ContentSourceFactory.snapshot(
                    snapshot,
                    descriptor,
                    namespaces,
                    "KuaYue runtime resources"
            );
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }

    public record ManagedPackRoot(
            String id,
            SemanticVersion version,
            boolean corepack,
            Path installationPath,
            PackResourceSnapshotter.CapturedPack capture,
            Set<String> namespaces
    ) implements AutoCloseable {
        public ManagedPackRoot {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(installationPath, "installationPath");
            Objects.requireNonNull(capture, "capture");
            namespaces = Set.copyOf(namespaces);
            if (namespaces.isEmpty()) {
                throw new IllegalArgumentException("A managed pack must declare a namespace");
            }
        }

        @Override
        public void close() throws IOException {
            capture.close();
        }
    }
}
