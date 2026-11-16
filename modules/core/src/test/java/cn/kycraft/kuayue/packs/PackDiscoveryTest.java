package cn.kycraft.kuayue.packs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackDiscoveryTest {
    private static final byte[] ICON_BYTES = {
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'
    };

    @TempDir
    Path temporaryDirectory;

    @Test
    void directoryPackLoadsOnlyTheRootIconPng() throws Exception {
        Path root = temporaryDirectory.resolve("directory-pack");
        writePack(root);
        Files.createDirectories(root.resolve("parcels/demo/assets/kuayue"));
        Files.write(root.resolve("parcels/demo/assets/kuayue/icon.png"), new byte[]{1});

        DiscoveredPack pack = PackDiscovery.scanCoreRoot(root, PackLimits.defaults());

        assertArrayEquals(ICON_BYTES, pack.icon().bytes());
    }

    @Test
    void zipPackLoadsOnlyTheRootIconPng() throws Exception {
        Path zip = temporaryDirectory.resolve("zip-pack.zip");
        try (OutputStream output = Files.newOutputStream(zip);
             ZipOutputStream archive = new ZipOutputStream(output)) {
            writeEntry(archive, "pack.json", packJson("example-pack", "example"));
            writeEntry(archive, "parcels/demo/parcel.json", parcelJson("example"));
            writeEntry(archive, "icon.png", ICON_BYTES);
            writeEntry(archive, "parcels/demo/assets/kuayue/icon.png", new byte[]{1});
        }

        PackCatalog catalog = PackDiscovery.scan(temporaryDirectory, PackLimits.defaults());

        assertEquals(1, catalog.packs().size());
        assertArrayEquals(ICON_BYTES, catalog.packs().getFirst().icon().bytes());
    }

    @Test
    void runtimeOverrideDisablesCorepackBeforeDependencyResolution() throws Exception {
        Path configPath = temporaryDirectory.resolve("kuayue/config.json");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, """
                {
                  "packs": {
                    "format_version": 1,
                    "enabled": {"kuayue-core": true},
                    "order": ["kuayue-core"],
                    "inventory": []
                  }
                }
                """);
        PackRuntimeEnvironment environment = new PackRuntimeEnvironment(Map.of(
                "minecraft", "1.21.1",
                "neoforge", "21.1.203",
                "kuayue", "2.0.0",
                "kasuga_lib", "1.0.0",
                "create", "6.0.6"
        ));

        PackDiscoveryResult result = PackDiscovery.discover(
                temporaryDirectory,
                environment,
                CorePackDescriptor.forVersion("2.0.0"),
                PackLimits.defaults(),
                VersionComparator.semantic(),
                true
        );

        assertFalse(result.configDocument().config().isRequestedEnabled(PackManifest.CORE_PACK_ID));
        assertTrue(result.selection().effectivePacks().isEmpty());
    }

    @Test
    void duplicateDirectoryAndZipBlockBothSourcesWithoutBecomingUnknown() throws Exception {
        Path packs = temporaryDirectory.resolve("kuayue/packs");
        writePack(packs.resolve("contrib-alpha"), "contrib-alpha", "contrib_alpha");
        Path zip = packs.resolve("contrib-alpha.zip");
        try (OutputStream output = Files.newOutputStream(zip);
             ZipOutputStream archive = new ZipOutputStream(output)) {
            writeEntry(archive, "pack.json",
                    packJson("contrib-alpha", "contrib_alpha"));
            writeEntry(archive, "parcels/demo/parcel.json",
                    parcelJson("contrib_alpha"));
            writeEntry(archive, "icon.png", ICON_BYTES);
        }
        Path configPath = temporaryDirectory.resolve("kuayue/config.json");
        Files.writeString(configPath, """
                {
                  "packs": {
                    "format_version": 1,
                    "enabled": {
                      "kuayue-core": false,
                      "contrib-alpha": true
                    },
                    "order": ["kuayue-core", "contrib-alpha"],
                    "inventory": []
                  }
                }
                """);

        PackDiscoveryResult result = PackDiscovery.discover(
                temporaryDirectory,
                new PackRuntimeEnvironment(Map.of(
                        "minecraft", "1.21.1",
                        "neoforge", "21.1.203",
                        "kuayue", "2.0.0",
                        "kasuga_lib", "1.0.0",
                        "create", "6.0.6"
                )),
                CorePackDescriptor.forVersion("2.0.0"),
                PackLimits.defaults(),
                VersionComparator.semantic(),
                true
        );

        assertEquals(2, result.catalog().installations().size());
        assertTrue(result.catalog().installations().stream().noneMatch(
                PackInstallation::valid));
        assertTrue(result.catalog().installations().stream().allMatch(
                installation -> installation.diagnosticCodes().contains(
                        PackDiagnosticCodes.DUPLICATE_PACK_ID)));
        assertTrue(result.selection().effectivePacks().isEmpty());
        assertTrue(result.configDocument().config().inventory().contains(
                "contrib-alpha"));
        assertFalse(result.report().diagnostics().stream().anyMatch(
                diagnostic -> PackDiagnosticCodes.UNKNOWN_PACK_ID.equals(
                        diagnostic.code())));
        assertEquals(
                List.of("directory:contrib-alpha", "zip:contrib-alpha.zip"),
                result.catalog().installations().stream()
                        .map(PackInstallation::sourceId)
                        .toList()
        );
        assertEquals(
                List.of("directory:contrib-alpha", "zip:contrib-alpha.zip"),
                KuaYueSettingsService.unavailableInstallations(
                        java.util.Set.of(PackManifest.CORE_PACK_ID),
                        result.report()
                ).stream().map(PackInstallationStatus::sourceId).toList()
        );
    }

    @Test
    void unsupportedFormatPreservesInstalledIdentityWithoutMisreportingItAsMissing()
            throws Exception {
        Path pack = temporaryDirectory.resolve("kuayue/packs/format-v2");
        Files.createDirectories(pack);
        Files.writeString(pack.resolve("pack.json"), """
                {
                  "format_version": 2,
                  "id": "format-v2",
                  "version": "1.0.0",
                  "namespaces": ["format_fixture"],
                  "name_key": "pack.format_fixture.name",
                  "dependencies": {"mods": {}, "packs": {}},
                  "parcels": ["parcels/demo/parcel.json"]
                }
                """);
        Path config = temporaryDirectory.resolve("kuayue/config.json");
        Files.writeString(config, """
                {
                  "packs": {
                    "format_version": 1,
                    "enabled": {
                      "kuayue-core": false,
                      "format-v2": true
                    },
                    "order": ["kuayue-core", "format-v2"],
                    "inventory": []
                  }
                }
                """);

        PackDiscoveryResult result = PackDiscovery.discover(
                temporaryDirectory,
                new PackRuntimeEnvironment(Map.of(
                        "minecraft", "1.21.1",
                        "neoforge", "21.1.203",
                        "kuayue", "2.0.0",
                        "kasuga_lib", "1.0.0",
                        "create", "6.0.6"
                )),
                CorePackDescriptor.forVersion("2.0.0"),
                PackLimits.defaults(),
                VersionComparator.semantic(),
                true
        );

        PackInstallation installation = result.catalog().installations().stream()
                .filter(candidate -> "directory:format-v2".equals(candidate.sourceId()))
                .findFirst()
                .orElseThrow();
        assertEquals("format-v2", installation.packId());
        assertEquals(SemanticVersion.parse("1.0.0"), installation.packVersion());
        assertFalse(installation.valid());
        assertEquals(
                List.of(PackDiagnosticCodes.UNSUPPORTED_FORMAT),
                installation.diagnosticCodes()
        );
        assertEquals(
                List.of(
                        PackDiagnosticCodes.DEPENDENCY_INVALID,
                        PackDiagnosticCodes.UNSUPPORTED_FORMAT
                ),
                result.report().diagnostics().stream()
                        .map(PackDiagnostic::code)
                        .sorted()
                        .toList()
        );
        assertFalse(result.report().diagnostics().stream().anyMatch(
                diagnostic -> PackDiagnosticCodes.UNKNOWN_PACK_ID.equals(
                        diagnostic.code())
                        || PackDiagnosticCodes.DEPENDENCY_MISSING.equals(
                        diagnostic.code())));
        assertTrue(result.selection().effectivePacks().isEmpty());
    }

    @Test
    void displayTextIsScopedToOwningPackWhenManifestsShareTranslationKey()
            throws Exception {
        Path packs = temporaryDirectory.resolve("packs");
        writePack(packs.resolve("visual-content"), "visual-content", "visual_fixture");
        writePack(packs.resolve("overlay-low"), "overlay-low", "visual_fixture");
        writePack(packs.resolve("overlay-high"), "overlay-high", "visual_fixture");
        writeLanguage(
                packs.resolve("visual-content/assets/visual_fixture/lang/zh_cn.json"),
                "VISUAL"
        );
        writeLanguage(
                packs.resolve(
                        "overlay-low/parcels/demo/assets/visual_fixture/lang/zh_cn.json"
                ),
                "LOW"
        );
        writeLanguage(
                packs.resolve(
                        "overlay-high/parcels/demo/assets/visual_fixture/lang/zh_cn.json"
                ),
                "HIGH"
        );

        PackCatalog catalog = PackDiscovery.scan(packs, PackLimits.defaults());
        Map<String, String> names = new HashMap<>();
        for (DiscoveredPack pack : catalog.packs()) {
            names.put(pack.packId(), pack.displayText().name("zh_cn"));
        }

        assertEquals("VISUAL", names.get("visual-content"));
        assertEquals("LOW", names.get("overlay-low"));
        assertEquals("HIGH", names.get("overlay-high"));
    }

    private static void writePack(Path root) throws Exception {
        writePack(root, "kuayue-core", "kuayue");
    }

    private static void writePack(Path root, String packId, String namespace)
            throws Exception {
        Files.createDirectories(root.resolve("parcels/demo"));
        Files.writeString(root.resolve("pack.json"), packJson(packId, namespace));
        Files.writeString(
                root.resolve("parcels/demo/parcel.json"),
                parcelJson(namespace)
        );
        Files.write(root.resolve("icon.png"), ICON_BYTES);
    }

    private static void writeEntry(ZipOutputStream archive, String path, String value)
            throws Exception {
        writeEntry(archive, path, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void writeEntry(ZipOutputStream archive, String path, byte[] value)
            throws Exception {
        archive.putNextEntry(new ZipEntry(path));
        archive.write(value);
        archive.closeEntry();
    }

    private static void writeLanguage(Path path, String name) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                {
                  "pack.example.name": "%s"
                }
                """.formatted(name));
    }

    private static String packJson(String packId, String namespace) {
        return """
                {
                  "format_version": 1,
                  "id": "%s",
                  "version": "2.0.0",
                  "namespaces": ["%s"],
                  "name_key": "pack.example.name",
                  "dependencies": {
                    "mods": {
                      "minecraft": "1.21.1",
                      "neoforge": "21.1.203",
                      "kuayue": "2.0.0",
                      "kasuga_lib": "1.0.0",
                      "create": "6.0.6"
                    },
                    "packs": {}
                  },
                  "parcels": ["parcels/demo/parcel.json"]
                }
                """.formatted(packId, namespace);
    }

    private static String parcelJson(String namespace) {
        return """
                {
                  "format_version": 1,
                  "id": "%s:demo",
                  "name_key": "parcel.demo.name",
                  "indexes": []
                }
                """.formatted(namespace);
    }
}
