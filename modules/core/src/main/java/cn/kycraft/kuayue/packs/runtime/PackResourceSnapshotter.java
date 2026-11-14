package cn.kycraft.kuayue.packs.runtime;

import cn.kycraft.kuayue.packs.DiscoveredPack;
import cn.kycraft.kuayue.packs.PackFileEntry;
import cn.kycraft.kuayue.packs.PackLimits;
import cn.kycraft.kuayue.packs.PackSourceKind;
import cn.kycraft.kuayue.packs.ParcelManifest;
import lib.kasuga.core.resource.identity.ResourceUri;
import lib.kasuga.core.resource.source.InputPolicy;
import lib.kasuga.core.resource.source.ResourceSnapshot;
import lib.kasuga.core.resource.source.ResourceSource;
import lib.kasuga.core.resource.source.ResourceSourceDescriptor;
import lib.kasuga.core.resource.source.ResourceSources;
import lib.kasuga.registration.data_driven.source.ContentSource;
import lib.kasuga.registration.data_driven.source.ContentSourceFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Captures one validated pack once and exposes its pack-root and parcel-root layers.
 */
public final class PackResourceSnapshotter {
    private PackResourceSnapshotter() {
    }

    public static Path installationPath(Path gameDirectory, DiscoveredPack pack) throws IOException {
        Path packsRoot = gameDirectory.resolve("kuayue").resolve("packs").normalize();
        Path installation = packsRoot.resolve(pack.containerName()).normalize();
        if (!installation.startsWith(packsRoot)) {
            throw new IOException(
                    "Pack installation escapes kuayue/packs: " + pack.containerName());
        }
        return installation;
    }

    public static CapturedPack capture(
            Path sourceRoot,
            DiscoveredPack pack,
            PackLimits limits,
            int firstRank
    ) throws IOException {
        Objects.requireNonNull(sourceRoot, "sourceRoot");
        Objects.requireNonNull(pack, "pack");
        Objects.requireNonNull(limits, "limits");
        if (firstRank < 0) {
            throw new IllegalArgumentException("firstRank must not be negative");
        }

        InputPolicy policy = new InputPolicy(
                limits.jsonFileBytes(),
                limits.regularFileBytes(),
                limits.packBytes(),
                limits.maxFiles(),
                64,
                1L << 20
        );
        ResourceSourceDescriptor captureDescriptor = new ResourceSourceDescriptor(
                pack.packId(),
                pack.packId(),
                "capture",
                0,
                ResourceSourceDescriptor.Origin.EXTERNAL
        );
        ResourceSource live = pack.sourceKind() == PackSourceKind.ZIP
                ? ResourceSources.archive(captureDescriptor, sourceRoot, policy)
                : ResourceSources.mod(captureDescriptor, sourceRoot, policy);

        ResourceSnapshot source;
        try {
            source = live.capture();
        } finally {
            live.close();
        }

        Map<String, PackFileEntry> inventory = new LinkedHashMap<>();
        pack.files().forEach(file -> inventory.put(file.physicalPath(), file));
        List<RootDefinition> definitions = rootDefinitions(pack, firstRank);
        Map<String, ResourceSnapshot.Builder> builders = new LinkedHashMap<>();
        List<ResourceSource> sources = new ArrayList<>();
        try {
            for (RootDefinition definition : definitions) {
                builders.put(
                        definition.rootId(),
                        new ResourceSnapshot.Builder(definition.descriptor(), policy)
                );
            }
            for (ResourceSnapshot.LocatedResourceFile file : source.files()) {
                PackFileEntry entry = inventory.get(file.descriptor().uri().path());
                if (entry == null || !isMinecraftResource(entry.logicalPath())) {
                    continue;
                }
                RootDefinition definition = definitions.stream()
                        .filter(candidate -> Objects.equals(candidate.parcelId(), entry.parcelId()))
                        .findFirst()
                        .orElseThrow(() -> new IOException(
                                "Pack file is not assigned to a root: " + entry.physicalPath()));
                builders.get(definition.rootId()).add(
                        ResourceUri.file(
                                definition.descriptor().sourceId(),
                                definition.rootId(),
                                file.descriptor().uri().path()
                        ),
                        entry.logicalPath(),
                        file.descriptor().type(),
                        file.descriptor().subtype(),
                        file.descriptor().tags(),
                        file::open
                );
            }

            List<CapturedRoot> roots = new ArrayList<>();
            int nextRank = firstRank + definitions.size();
            for (RootDefinition definition : definitions) {
                if (definition.parcelId() != null) {
                    Set<String> actual = builders.get(definition.rootId()).logicalPaths();
                    for (String index : definition.declaredIndexes()) {
                        if (!actual.contains(index)) {
                            throw new IOException(
                                    "Parcel " + definition.parcelId()
                                            + " declares missing index " + index);
                        }
                    }
                }
                ResourceSnapshot snapshot = builders.get(definition.rootId()).build();
                sources.add(ResourceSources.fixed(definition.descriptor(), snapshot, policy));
                roots.add(new CapturedRoot(
                        pack.packId(),
                        definition.rootId(),
                        definition.parcelId(),
                        definition.rank(),
                        Set.copyOf(pack.manifest().namespaces()),
                        definition.declaredIndexes(),
                        sources.getLast()
                ));
            }
            return new CapturedPack(pack.packId(), List.copyOf(roots), nextRank);
        } catch (IOException | RuntimeException failure) {
            for (ResourceSource captured : sources) {
                try {
                    captured.close();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            throw failure;
        } finally {
            for (ResourceSnapshot.Builder builder : builders.values()) {
                try {
                    builder.close();
                } catch (IOException ignored) {
                    // Successfully built root snapshots are owned by their fixed sources.
                }
            }
            source.close();
        }
    }

    private static List<RootDefinition> rootDefinitions(DiscoveredPack pack, int firstRank)
            throws IOException {
        List<RootDefinition> definitions = new ArrayList<>();
        long nextRank = firstRank;
        int mainRank = checkedRank(nextRank++);
        definitions.add(new RootDefinition(
                "main",
                null,
                mainRank,
                null,
                new ResourceSourceDescriptor(
                        pack.packId(),
                        pack.packId(),
                        "main",
                        mainRank,
                        ResourceSourceDescriptor.Origin.EXTERNAL
                )
        ));
        for (String parcelPath : pack.manifest().parcels()) {
            String prefix = parcelPath.substring(
                    0,
                    parcelPath.length() - "parcel.json".length()
            );
            String folder = parcelPath.split("/")[1];
            String parcelId = pack.files().stream()
                    .filter(file -> file.physicalPath().startsWith(prefix))
                    .map(PackFileEntry::parcelId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new IOException(
                            "Cannot resolve parcel manifest root: " + parcelPath));
            ParcelManifest parcel = pack.parcels().get(parcelId);
            if (parcel == null) {
                throw new IOException("Missing parcel manifest: " + parcelPath);
            }
            String rootId = "parcel-" + folder;
            int rank = checkedRank(nextRank++);
            definitions.add(new RootDefinition(
                    rootId,
                    parcel.id(),
                    rank,
                    List.copyOf(parcel.indexes()),
                    new ResourceSourceDescriptor(
                            pack.packId(),
                            pack.packId(),
                            rootId,
                            rank,
                            ResourceSourceDescriptor.Origin.EXTERNAL
                    )
            ));
        }
        return List.copyOf(definitions);
    }

    private static int checkedRank(long rank) throws IOException {
        if (rank > Integer.MAX_VALUE) {
            throw new IOException("Resource root rank overflow");
        }
        return (int) rank;
    }

    private static boolean isMinecraftResource(String path) {
        return path.equals("pack.mcmeta")
                || path.equals("pack.png")
                || path.startsWith("assets/")
                || path.startsWith("data/");
    }

    private record RootDefinition(
            String rootId,
            String parcelId,
            int rank,
            List<String> declaredIndexes,
            ResourceSourceDescriptor descriptor
    ) {
        private RootDefinition {
            declaredIndexes = declaredIndexes == null ? null : List.copyOf(declaredIndexes);
        }
    }

    public record CapturedRoot(
            String packId,
            String rootId,
            String parcelId,
            int rank,
            Set<String> namespaces,
            List<String> declaredIndexes,
            ResourceSource source
    ) implements AutoCloseable {
        public CapturedRoot {
            Objects.requireNonNull(packId, "packId");
            Objects.requireNonNull(rootId, "rootId");
            Objects.requireNonNull(namespaces, "namespaces");
            declaredIndexes = declaredIndexes == null ? null : List.copyOf(declaredIndexes);
            Objects.requireNonNull(source, "source");
        }

        public ContentSource contentSource() throws IOException {
            ResourceSnapshot snapshot = source.capture();
            ResourceSourceDescriptor descriptor = source.descriptor();
            return ContentSourceFactory.snapshot(
                    snapshot,
                    descriptor,
                    namespaces,
                    "pack " + packId + " root " + rootId,
                    declaredIndexes
            );
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }

    public record CapturedPack(
            String packId,
            List<CapturedRoot> roots,
            int nextRank
    ) implements AutoCloseable {
        public CapturedPack {
            Objects.requireNonNull(packId, "packId");
            roots = List.copyOf(roots);
            if (nextRank < 0) {
                throw new IllegalArgumentException("nextRank must not be negative");
            }
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (CapturedRoot root : roots) {
                try {
                    root.close();
                } catch (IOException exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
