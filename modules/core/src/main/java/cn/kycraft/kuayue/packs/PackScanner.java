package cn.kycraft.kuayue.packs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

final class PackScanner {
    private static final String PACK_MANIFEST_PATH = "pack.json";

    PackCatalog scan(Path packsDirectory, PackLimits limits) {
        if (!Files.exists(packsDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return new PackCatalog(List.of(), List.of(), List.of());
        }
        if (Files.isSymbolicLink(packsDirectory)
                || !Files.isDirectory(packsDirectory, LinkOption.NOFOLLOW_LINKS)) {
            PackDiagnostic diagnostic = PackDiagnostic.error(
                    PackDiagnosticCodes.UNSAFE_PATH,
                    PackStage.DISCOVERY,
                    null,
                    "packs-directory",
                    "kuayue/packs",
                    "",
                    "Pack installation root must be a real directory");
            return new PackCatalog(List.of(), List.of(), List.of(diagnostic));
        }

        List<Path> children;
        try (var stream = Files.list(packsDirectory)) {
            children = stream.sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            PackDiagnostic diagnostic = PackDiagnostic.error(
                    PackDiagnosticCodes.IO_ERROR,
                    PackStage.DISCOVERY,
                    null,
                    "packs-directory",
                    "kuayue/packs",
                    "",
                    "Unable to list pack installations: " + exception.getMessage());
            return new PackCatalog(List.of(), List.of(), List.of(diagnostic));
        }

        List<PackScanOutcome> outcomes = new ArrayList<>();
        for (Path child : children) {
            String name = child.getFileName().toString();
            if (Files.isSymbolicLink(child)) {
                outcomes.add(PackScanOutcome.failure(
                        sourceId("directory", name), PackSourceKind.DIRECTORY, name,
                        PackDiagnostic.error(
                                PackDiagnosticCodes.UNSAFE_PATH,
                                PackStage.DISCOVERY,
                                null,
                                sourceId("directory", name),
                                "kuayue/packs/" + name,
                                "",
                                "Symbolic-link installations are not allowed")));
                continue;
            }
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                outcomes.add(scanDirectory(child, name, limits, false));
            } else if (Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS)
                    && name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                outcomes.add(scanZip(child, name, limits));
            }
        }
        return finishCatalog(outcomes);
    }

    DiscoveredPack scanCoreRoot(Path root, PackLimits limits) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Corepack root is not a directory: " + root);
        }
        PackScanOutcome outcome = scanDirectory(
                root,
                PackManifest.CORE_PACK_ID,
                limits,
                true
        );
        if (outcome.pack() == null) {
            throw new IllegalStateException(
                    "Invalid KuaYue corepack at " + root + ": " + outcome.diagnostics());
        }
        if (!PackManifest.CORE_PACK_ID.equals(outcome.pack().packId())) {
            throw new IllegalStateException(
                    "Embedded corepack must use ID " + PackManifest.CORE_PACK_ID
                            + ", got " + outcome.pack().packId());
        }
        return outcome.pack();
    }

    PackScanOutcome scanDirectory(
            Path root,
            String containerName,
            PackLimits limits,
            boolean allowCorePack
    ) {
        String sourceId = sourceId("directory", containerName);
        Map<String, Path> files = new TreeMap<>();
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                private long fileCount;
                private long totalBytes;

                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                        throws IOException {
                    if (!directory.equals(root) && attributes.isSymbolicLink()) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.UNSAFE_PATH,
                                "Symbolic links are not allowed: " + relative(root, directory));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                        throws IOException {
                    String relativePath = relative(root, file);
                    if (attributes.isSymbolicLink()) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.UNSAFE_PATH,
                                "Symbolic links are not allowed: " + relativePath);
                    }
                    if (!attributes.isRegularFile()) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.UNSAFE_PATH,
                                "Only regular files are allowed: " + relativePath);
                    }
                    if (!PackPathRules.isSafeRelativePath(relativePath)) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.UNSAFE_PATH,
                                "Unsafe pack path: " + relativePath);
                    }
                    fileCount++;
                    if (fileCount > limits.maxFiles()) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.LIMIT_EXCEEDED,
                                "Pack contains more than " + limits.maxFiles() + " files");
                    }
                    long size = attributes.size();
                    long fileLimit = isJson(relativePath)
                            ? limits.jsonFileBytes() : limits.regularFileBytes();
                    if (size > fileLimit) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.LIMIT_EXCEEDED,
                                "File exceeds its size limit: " + relativePath);
                    }
                    totalBytes = Math.addExact(totalBytes, size);
                    if (totalBytes > limits.packBytes()) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.LIMIT_EXCEEDED,
                                "Pack exceeds the total size limit");
                    }
                    files.put(relativePath, file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (ScanFailure failure) {
            diagnostics.add(failure.diagnostic(
                    sourceId, "kuayue/packs/" + containerName, null));
            return invalidOutcome(sourceId, PackSourceKind.DIRECTORY, containerName, diagnostics);
        } catch (IOException | ArithmeticException exception) {
            diagnostics.add(PackDiagnostic.error(
                    PackDiagnosticCodes.IO_ERROR,
                    PackStage.DISCOVERY,
                    null,
                    sourceId,
                    "kuayue/packs/" + containerName,
                    "",
                    "Unable to scan directory pack: " + exception.getMessage()));
            return invalidOutcome(sourceId, PackSourceKind.DIRECTORY, containerName, diagnostics);
        }
        if (!files.containsKey(PACK_MANIFEST_PATH)) {
            diagnostics.add(PackDiagnostic.error(
                    PackDiagnosticCodes.INVALID_MANIFEST,
                    PackStage.MANIFEST,
                    null,
                    sourceId,
                    PACK_MANIFEST_PATH,
                    "",
                    "Pack directory root must contain pack.json"));
            return invalidOutcome(sourceId, PackSourceKind.DIRECTORY, containerName, diagnostics);
        }

        try {
            byte[] manifestBytes = readFileBounded(
                    files.get(PACK_MANIFEST_PATH), limits.jsonFileBytes());
            PackManifest manifest = parseManifest(manifestBytes, allowCorePack);
            return finishValidPack(
                    sourceId,
                    PackSourceKind.DIRECTORY,
                    containerName,
                    manifest,
                    files,
                    limits,
                    allowCorePack,
                    path -> readFileBounded(files.get(path), limits.jsonFileBytes()));
        } catch (PackParseException exception) {
            diagnostics.add(parseDiagnostic(sourceId, manifestName(containerName),
                    exception, null));
            return invalidOutcome(sourceId, PackSourceKind.DIRECTORY, containerName, diagnostics);
        } catch (ScanFailure failure) {
            diagnostics.add(failure.diagnostic(
                    sourceId, "kuayue/packs/" + containerName, null));
            return invalidOutcome(sourceId, PackSourceKind.DIRECTORY, containerName, diagnostics);
        } catch (IOException exception) {
            diagnostics.add(PackDiagnostic.error(
                    PackDiagnosticCodes.IO_ERROR,
                    PackStage.DISCOVERY,
                    null,
                    sourceId,
                    "kuayue/packs/" + containerName,
                    "",
                    "Unable to read directory pack: " + exception.getMessage()));
            return invalidOutcome(sourceId, PackSourceKind.DIRECTORY, containerName, diagnostics);
        }
    }

    private PackScanOutcome scanZip(Path zipPath, String containerName, PackLimits limits) {
        String sourceId = sourceId("zip", containerName);
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Long> sizes = new TreeMap<>();
        byte[] rootManifest = null;
        try {
            detectZipSymlinks(zipPath);
            try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
                List<ZipEntry> fileEntries = enumerateZipFiles(zip, limits);
                long totalBytes = 0;
                for (ZipEntry entry : fileEntries) {
                    String path = entry.getName();
                    long actual;
                    if (PACK_MANIFEST_PATH.equals(path)) {
                        BoundedRead captured = readZipEntry(
                                zip, entry, limits.jsonFileBytes());
                        actual = captured.size();
                        rootManifest = captured.bytes();
                    } else {
                        actual = readZipEntry(zip, entry,
                                isJson(path) ? limits.jsonFileBytes()
                                        : limits.regularFileBytes()).size();
                    }
                    if (actual > (isJson(path)
                            ? limits.jsonFileBytes() : limits.regularFileBytes())) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.LIMIT_EXCEEDED,
                                "ZIP entry exceeds its size limit: " + path);
                    }
                    totalBytes = Math.addExact(totalBytes, actual);
                    if (totalBytes > limits.packBytes()) {
                        throw new ScanFailure(
                                PackDiagnosticCodes.LIMIT_EXCEEDED,
                                "ZIP pack exceeds the total size limit");
                    }
                    sizes.put(path, actual);
                }
            }
            if (rootManifest == null) {
                diagnostics.add(PackDiagnostic.error(
                        PackDiagnosticCodes.INVALID_ZIP,
                        PackStage.DISCOVERY,
                        null,
                        sourceId,
                        containerName,
                        "",
                        "ZIP root must directly contain pack.json without a wrapper folder"));
                return invalidOutcome(sourceId, PackSourceKind.ZIP, containerName, diagnostics);
            }
            PackManifest manifest = parseManifest(rootManifest, false);
            return finishValidPack(
                    sourceId,
                    PackSourceKind.ZIP,
                    containerName,
                    manifest,
                    sizes,
                    limits,
                    false,
                    path -> {
                        Long expected = sizes.get(path);
                        if (expected == null) {
                            throw new ScanFailure(
                                    PackDiagnosticCodes.INVALID_MANIFEST,
                                    "Listed parcel manifest does not exist: " + path);
                        }
                        BoundedRead read = readStandaloneZipEntry(
                                zipPath, path, limits.jsonFileBytes());
                        if (read.size() != expected) {
                            throw new ScanFailure(
                                    PackDiagnosticCodes.INVALID_ZIP,
                                    "ZIP entry changed while being scanned: " + path);
                        }
                        return read.bytes();
                    });
        } catch (PackParseException exception) {
            diagnostics.add(parseDiagnostic(sourceId, containerName, exception, null));
            return invalidOutcome(sourceId, PackSourceKind.ZIP, containerName, diagnostics);
        } catch (ScanFailure failure) {
            diagnostics.add(failure.diagnostic(
                    sourceId, "kuayue/packs/" + containerName, null));
            return invalidOutcome(sourceId, PackSourceKind.ZIP, containerName, diagnostics);
        } catch (ZipException exception) {
            diagnostics.add(PackDiagnostic.error(
                    PackDiagnosticCodes.INVALID_ZIP,
                    PackStage.DISCOVERY,
                    null,
                    sourceId,
                    "kuayue/packs/" + containerName,
                    "",
                    "Invalid or truncated ZIP: " + exception.getMessage()));
            return invalidOutcome(sourceId, PackSourceKind.ZIP, containerName, diagnostics);
        } catch (IOException | ArithmeticException exception) {
            diagnostics.add(PackDiagnostic.error(
                    PackDiagnosticCodes.INVALID_ZIP,
                    PackStage.DISCOVERY,
                    null,
                    sourceId,
                    "kuayue/packs/" + containerName,
                    "",
                    "Unable to scan ZIP pack: " + exception.getMessage()));
            return invalidOutcome(sourceId, PackSourceKind.ZIP, containerName, diagnostics);
        }
    }

    private List<ZipEntry> enumerateZipFiles(ZipFile zip, PackLimits limits)
            throws ScanFailure {
        Map<String, ZipEntry> files = new LinkedHashMap<>();
        Set<String> paths = new HashSet<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String rawName = entry.getName();
            String logicalName = entry.isDirectory() && rawName.endsWith("/")
                    ? rawName.substring(0, rawName.length() - 1) : rawName;
            if (!PackPathRules.isSafeRelativePath(logicalName)) {
                throw new ScanFailure(
                        PackDiagnosticCodes.UNSAFE_PATH,
                        "Unsafe ZIP entry: " + rawName);
            }
            if (!paths.add(logicalName)) {
                throw new ScanFailure(
                        PackDiagnosticCodes.DUPLICATE_ZIP_ENTRY,
                        "Duplicate ZIP entry: " + logicalName);
            }
            if (!entry.isDirectory()) {
                files.put(logicalName, entry);
                if (files.size() > limits.maxFiles()) {
                    throw new ScanFailure(
                            PackDiagnosticCodes.LIMIT_EXCEEDED,
                            "ZIP contains more than " + limits.maxFiles() + " files");
                }
            }
        }
        for (String file : files.keySet()) {
            String ancestor = parent(file);
            while (ancestor != null) {
                if (files.containsKey(ancestor)) {
                    throw new ScanFailure(
                            PackDiagnosticCodes.INVALID_ZIP,
                            "ZIP entry is both a file and a directory: " + ancestor);
                }
                ancestor = parent(ancestor);
            }
        }
        List<ZipEntry> ordered = new ArrayList<>(files.values());
        ordered.sort(Comparator.comparing(ZipEntry::getName));
        return ordered;
    }

    private BoundedRead readZipEntry(ZipFile zip, ZipEntry entry, long maximum)
            throws IOException, ScanFailure {
        try (InputStream input = zip.getInputStream(entry)) {
            return readBounded(input, maximum, true);
        }
    }

    private BoundedRead readStandaloneZipEntry(Path zipPath, String path, long maximum)
            throws IOException, ScanFailure {
        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry(path);
            if (entry == null || entry.isDirectory()) {
                throw new ScanFailure(
                        PackDiagnosticCodes.INVALID_MANIFEST,
                        "Missing parcel manifest entry: " + path);
            }
            return readZipEntry(zip, entry, maximum);
        }
    }

    private void detectZipSymlinks(Path zipPath) throws IOException, ScanFailure {
        try (FileSystem fileSystem = FileSystems.newFileSystem(zipPath, Map.of())) {
            Path root = fileSystem.getPath("/");
            try (var paths = Files.walk(root)) {
                for (Path path : paths.toList()) {
                    if (!path.equals(root) && Files.isSymbolicLink(path)) {
                        String logical = root.relativize(path).toString().replace('\\', '/');
                        throw new ScanFailure(
                                PackDiagnosticCodes.UNSAFE_PATH,
                                "Symbolic-link ZIP entry is not allowed: " + logical);
                    }
                }
            }
        } catch (ScanFailure exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ZipException("Unable to inspect ZIP entries: " + exception.getMessage());
        }
    }

    private PackScanOutcome finishValidPack(
            String sourceId,
            PackSourceKind sourceKind,
            String containerName,
            PackManifest manifest,
            Map<String, ?> files,
            PackLimits limits,
            boolean allowCorePack,
            ManifestReader reader
    ) throws IOException, ScanFailure {
        Map<String, ParcelManifest> parcels = new LinkedHashMap<>();
        Map<String, String> parcelRoots = new LinkedHashMap<>();
        for (String parcelPath : manifest.parcels()) {
            if (!files.containsKey(parcelPath)) {
                throw new ScanFailure(
                        PackDiagnosticCodes.INVALID_MANIFEST,
                        "Listed parcel manifest does not exist: " + parcelPath);
            }
            byte[] bytes = reader.read(parcelPath);
            if (bytes.length > limits.jsonFileBytes()) {
                throw new ScanFailure(
                        PackDiagnosticCodes.LIMIT_EXCEEDED,
                        "Parcel manifest exceeds JSON size limit: " + parcelPath);
            }
            ParcelManifest parcel;
            try {
                parcel = ParcelManifest.parse(
                        new String(bytes, StandardCharsets.UTF_8), manifest.namespaces());
            } catch (PackParseException exception) {
                throw new PackDiagnosticScanFailure(
                        parseDiagnostic(sourceId, parcelPath, exception, null));
            }
            if (parcels.putIfAbsent(parcel.id(), parcel) != null) {
                throw new ScanFailure(
                        PackDiagnosticCodes.INVALID_MANIFEST,
                        "Duplicate parcel ID: " + parcel.id());
            }
            parcelRoots.put(parcelPath.substring(0, parcelPath.length() - "parcel.json".length()),
                    parcel.id());
        }

        List<PackFileEntry> inventory = new ArrayList<>();
        for (var entry : files.entrySet()) {
            String physical = entry.getKey();
            String parcelId = null;
            String logical = physical;
            PackRootKind rootKind = PackRootKind.PACK_ROOT;
            for (var parcelRoot : parcelRoots.entrySet()) {
                if (physical.startsWith(parcelRoot.getKey())) {
                    rootKind = PackRootKind.PARCEL_ROOT;
                    parcelId = parcelRoot.getValue();
                    logical = physical.substring(parcelRoot.getKey().length());
                    break;
                }
            }
            if (!PackPathRules.isSafeRelativePath(logical)) {
                throw new ScanFailure(
                        PackDiagnosticCodes.UNSAFE_PATH,
                        "Unsafe logical pack path: " + logical);
            }
            long size = entry.getValue() instanceof Long longSize
                    ? longSize : readDirectorySize(entry.getValue());
            inventory.add(new PackFileEntry(physical, logical, size, rootKind, parcelId));
        }
        DiscoveredPack pack = new DiscoveredPack(
                sourceId, sourceKind, containerName, manifest, parcels, inventory);
        PackInstallation installation = new PackInstallation(
                sourceId, sourceKind, containerName, manifest.id(), manifest.version(),
                true, List.of());
        return new PackScanOutcome(installation, pack, List.of());
    }

    private long readDirectorySize(Object path) throws ScanFailure {
        if (!(path instanceof Path filePath)) {
            throw new ScanFailure(
                    PackDiagnosticCodes.IO_ERROR, "Unexpected pack file representation");
        }
        try {
            return Files.size(filePath);
        } catch (IOException exception) {
            throw new ScanFailure(
                    PackDiagnosticCodes.IO_ERROR,
                    "Unable to read file size: " + exception.getMessage());
        }
    }

    private PackCatalog finishCatalog(List<PackScanOutcome> outcomes) {
        Map<String, List<DiscoveredPack>> byId = new TreeMap<>();
        for (PackScanOutcome outcome : outcomes) {
            if (outcome.pack() != null) {
                byId.computeIfAbsent(outcome.pack().packId(), ignored -> new ArrayList<>())
                        .add(outcome.pack());
            }
        }

        Set<String> duplicateIds = new HashSet<>();
        for (var entry : byId.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateIds.add(entry.getKey());
            }
        }

        List<DiscoveredPack> validPacks = new ArrayList<>();
        List<PackInstallation> installations = new ArrayList<>();
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        for (PackScanOutcome outcome : outcomes) {
            if (outcome.pack() != null && !duplicateIds.contains(outcome.pack().packId())) {
                validPacks.add(outcome.pack());
            }
            diagnostics.addAll(outcome.diagnostics());
        }
        for (String duplicateId : duplicateIds) {
            List<DiscoveredPack> duplicates = byId.get(duplicateId);
            List<String> sourceIds = duplicates.stream()
                    .map(DiscoveredPack::sourceId).sorted().toList();
            for (DiscoveredPack duplicate : duplicates) {
                diagnostics.add(new PackDiagnostic(
                        PackDiagnosticCodes.DUPLICATE_PACK_ID,
                        PackSeverity.ERROR,
                        PackStage.DISCOVERY,
                        duplicateId,
                        duplicate.sourceId(),
                        null,
                        PACK_MANIFEST_PATH,
                        "/id",
                        sourceIds,
                        "Pack ID is installed more than once"));
            }
        }

        for (PackScanOutcome outcome : outcomes) {
            PackInstallation installation = outcome.installation();
            if (installation == null) {
                continue;
            }
            if (installation.packId() != null
                    && duplicateIds.contains(installation.packId())) {
                installation = new PackInstallation(
                        installation.sourceId(),
                        installation.sourceKind(),
                        installation.containerName(),
                        installation.packId(),
                        installation.packVersion(),
                        false,
                        List.of(PackDiagnosticCodes.DUPLICATE_PACK_ID));
            }
            installations.add(installation);
        }
        validPacks.sort(Comparator.comparing(DiscoveredPack::packId)
                .thenComparing(DiscoveredPack::sourceId));
        installations.sort(Comparator.comparing(
                        (PackInstallation installation) ->
                                installation.packId() == null ? "" : installation.packId())
                .thenComparing(PackInstallation::sourceId));
        return new PackCatalog(validPacks, installations, diagnostics);
    }

    private PackScanOutcome invalidOutcome(
            String sourceId,
            PackSourceKind sourceKind,
            String containerName,
            List<PackDiagnostic> diagnostics
    ) {
        PackInstallation installation = new PackInstallation(
                sourceId,
                sourceKind,
                containerName,
                null,
                null,
                false,
                diagnostics.stream().map(PackDiagnostic::code).distinct().toList());
        return new PackScanOutcome(installation, null, diagnostics);
    }

    private PackDiagnostic parseDiagnostic(
            String sourceId,
            String logicalPath,
            PackParseException exception,
            String parcelId
    ) {
        return new PackDiagnostic(
                exception.code(),
                PackSeverity.ERROR,
                logicalPath.endsWith("parcel.json") ? PackStage.PARCEL : PackStage.MANIFEST,
                null,
                sourceId,
                parcelId,
                logicalPath,
                exception.jsonPointer(),
                List.of(),
                exception.getMessage());
    }

    private byte[] readFileBounded(Path path, long maximum) throws IOException, ScanFailure {
        if (Files.isSymbolicLink(path)) {
            throw new ScanFailure(
                    PackDiagnosticCodes.UNSAFE_PATH,
                    "Symbolic links are not allowed: " + path.getFileName());
        }
        try (InputStream input = Files.newInputStream(path)) {
            return readBounded(input, maximum, true).bytes();
        }
    }

    private BoundedRead readBounded(InputStream input, long maximum, boolean capture)
            throws IOException, ScanFailure {
        ByteArrayOutputStream output = capture ? new ByteArrayOutputStream() : null;
        byte[] buffer = new byte[16 * 1024];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            total += read;
            if (total > maximum) {
                throw new ScanFailure(
                        PackDiagnosticCodes.LIMIT_EXCEEDED,
                        "File exceeds its size limit while being read");
            }
            if (output != null) {
                output.write(buffer, 0, read);
            }
        }
        return new BoundedRead(total, output == null ? null : output.toByteArray());
    }

    private static String relative(Path root, Path child) {
        return root.relativize(child).toString().replace('\\', '/');
    }

    private static boolean isJson(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static PackManifest parseManifest(byte[] bytes, boolean allowCorePack) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        return allowCorePack ? PackManifest.parseCore(json) : PackManifest.parse(json);
    }

    private static String parent(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? null : path.substring(0, slash);
    }

    private static String sourceId(String kind, String name) {
        return kind + ":" + name;
    }

    private static String manifestName(String containerName) {
        return containerName + "/" + PACK_MANIFEST_PATH;
    }

    private record BoundedRead(long size, byte[] bytes) {
    }

    @FunctionalInterface
    private interface ManifestReader {
        byte[] read(String path) throws IOException, ScanFailure;
    }

    private record PackScanOutcome(
            PackInstallation installation,
            DiscoveredPack pack,
            List<PackDiagnostic> diagnostics
    ) {
        private static PackScanOutcome failure(
                String sourceId,
                PackSourceKind sourceKind,
                String containerName,
                PackDiagnostic diagnostic
        ) {
            PackInstallation installation = new PackInstallation(
                    sourceId, sourceKind, containerName, null, null, false,
                    List.of(diagnostic.code()));
            return new PackScanOutcome(installation, null, List.of(diagnostic));
        }
    }

    private static class ScanFailure extends RuntimeException {
        private final String code;

        ScanFailure(String code, String message) {
            super(message);
            this.code = code;
        }

        PackDiagnostic diagnostic(String sourceId, String logicalPath, String jsonPointer) {
            return PackDiagnostic.error(
                    code,
                    PackStage.DISCOVERY,
                    null,
                    sourceId,
                    logicalPath,
                    jsonPointer,
                    getMessage());
        }
    }

    private static final class PackDiagnosticScanFailure extends ScanFailure {
        private final PackDiagnostic diagnostic;

        PackDiagnosticScanFailure(PackDiagnostic diagnostic) {
            super(diagnostic.code(), diagnostic.message());
            this.diagnostic = diagnostic;
        }

        @Override
        PackDiagnostic diagnostic(String sourceId, String logicalPath, String jsonPointer) {
            return diagnostic;
        }
    }
}
