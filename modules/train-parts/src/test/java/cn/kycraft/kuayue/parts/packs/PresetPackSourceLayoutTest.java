package cn.kycraft.kuayue.parts.packs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetPackSourceLayoutTest {
    private static final Path PACKS = Path.of("resources/packs");
    private static final Path CORE_RESOURCES = Path.of("../core/resources");
    private static final String RUNNING_GEAR = "kuayue-preset-running-gear";
    private static final String CARRIAGE_TAB = "kuayue:kuayue_carriage";
    private static final String LOCO_TAB = "kuayue:kuayue_loco";
    private static final Map<String, String> EXPECTED_TRAIN_TABS = Map.ofEntries(
            Map.entry("kuayue:c22", CARRIAGE_TAB),
            Map.entry("kuayue:c25", CARRIAGE_TAB),
            Map.entry("kuayue:c25b", CARRIAGE_TAB),
            Map.entry("kuayue:c25g", CARRIAGE_TAB),
            Map.entry("kuayue:c25k", CARRIAGE_TAB),
            Map.entry("kuayue:c25t", CARRIAGE_TAB),
            Map.entry("kuayue:c25z", CARRIAGE_TAB),
            Map.entry("kuayue:cr200j", CARRIAGE_TAB),
            Map.entry("kuayue:freight", CARRIAGE_TAB),
            Map.entry("kuayue:m1", CARRIAGE_TAB),
            Map.entry("kuayue:df11", LOCO_TAB),
            Map.entry("kuayue:df11g", LOCO_TAB),
            Map.entry("kuayue:df21", LOCO_TAB),
            Map.entry("kuayue:df5", LOCO_TAB),
            Map.entry("kuayue:hxd3d", LOCO_TAB),
            Map.entry("kuayue:jy290", LOCO_TAB),
            Map.entry("kuayue:ss3", LOCO_TAB),
            Map.entry("kuayue:ss8", LOCO_TAB)
    );
    private static final Set<String> KNOWN_UPSTREAM_MODEL_GAPS = Set.of(
            "block/df11g/door/frame_door_cabin_df11g_left",
            "block/df11g/door/frame_door_cabin_df11g_right"
    );
    private static final Set<String> PACK_IDS = Set.of(
            RUNNING_GEAR,
            "kuayue-preset-passenger-22",
            "kuayue-preset-passenger-25",
            "kuayue-preset-df",
            "kuayue-preset-ss",
            "kuayue-preset-hxd",
            "kuayue-preset-emu",
            "kuayue-preset-meter-gauge",
            "kuayue-preset-freight",
            "kuayue-preset-industrial"
    );

    @Test
    void everyPresetIsACompleteSelfContainedSourcePack() throws IOException {
        Set<String> directories = new LinkedHashSet<>();
        try (var paths = Files.list(PACKS)) {
            paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .forEach(directories::add);
        }
        assertEquals(PACK_IDS, directories);

        int parcelCount = 0;
        for (String packId : directories) {
            Path pack = PACKS.resolve(packId);
            assertTrue(Files.isRegularFile(pack.resolve("pack.json")), packId);
            assertTrue(Files.isRegularFile(pack.resolve("pack.mcmeta")), packId);
            assertTrue(Files.isRegularFile(pack.resolve("icon.png")), packId);
            assertTrue(Files.isRegularFile(
                    pack.resolve("assets/kuayue/lang/en_us.json")), packId);
            assertTrue(Files.isRegularFile(
                    pack.resolve("assets/kuayue/lang/zh_cn.json")), packId);

            JsonObject manifest = DataJson.GSON.fromJson(
                    Files.readString(pack.resolve("pack.json")),
                    JsonObject.class
            );
            assertEquals(packId, manifest.get("id").getAsString());

            Set<String> declared = new LinkedHashSet<>();
            for (JsonElement entry : manifest.getAsJsonArray("parcels")) {
                String parcelPath = entry.getAsString();
                declared.add(parcelPath);
                Path parcel = pack.resolve(parcelPath);
                assertTrue(Files.isRegularFile(parcel), parcelPath);
                JsonObject parcelManifest = DataJson.GSON.fromJson(
                        Files.readString(parcel),
                        JsonObject.class
                );
                assertTrue(parcelManifest.get("id").getAsString()
                        .startsWith("kuayue:"), parcelPath);
                parcelCount++;
            }

            Set<String> actual = new LinkedHashSet<>();
            try (var paths = Files.list(pack.resolve("parcels"))) {
                paths.filter(Files::isDirectory)
                        .map(path -> "parcels/" + path.getFileName() + "/parcel.json")
                        .sorted()
                        .forEach(actual::add);
            }
            assertEquals(actual, declared, packId);

            assertEquals(
                    RUNNING_GEAR.equals(packId),
                    manifest.get("required").getAsBoolean(),
                    packId
            );
            if (RUNNING_GEAR.equals(packId)) {
                assertEquals(Set.of(), manifest.getAsJsonObject("dependencies")
                        .getAsJsonObject("packs").keySet());
            } else {
                assertEquals(
                        "*",
                        manifest.getAsJsonObject("dependencies")
                                .getAsJsonObject("packs")
                                .get(RUNNING_GEAR)
                                .getAsString()
                );
            }
        }
        assertEquals(33, parcelCount);
        assertFalse(Files.exists(PACKS.getParent().resolve("tracks")));
        assertFalse(Files.exists(Path.of("src/main/resources/kuayue-packs")));
    }

    @Test
    void trainTranslationsExistInEveryRequiredLocale() throws IOException {
        for (String packId : PACK_IDS) {
            Path pack = PACKS.resolve(packId);
            Map<String, Set<String>> keysByLocale = new HashMap<>();
            for (String locale : Set.of("en_us", "zh_cn")) {
                Set<String> keys = new HashSet<>();
                try (var paths = Files.walk(pack)) {
                    for (Path language : paths
                            .filter(path -> path.getFileName().toString().equals(locale + ".json"))
                            .filter(path -> path.getParent() != null
                                    && path.getParent().getFileName().toString().equals("lang"))
                            .toList()) {
                        DataJson.GSON.fromJson(Files.readString(language), JsonObject.class)
                                .keySet()
                                .forEach(keys::add);
                    }
                }
                keysByLocale.put(locale, keys);
            }

            for (LocatedTrain train : readTrains(pack)) {
                for (String field : Set.of("name_key", "description_key")) {
                    String key = train.definition().get(field).getAsString();
                    for (var locale : keysByLocale.entrySet()) {
                        assertTrue(
                                locale.getValue().contains(key),
                                () -> train.source() + " / " + locale.getKey()
                                        + " is missing " + key
                        );
                    }
                }
            }
        }
    }

    @Test
    void trainDefinitionsAreIndexedAndExposeSplitCreativeFilters() throws IOException {
        Map<String, String> actual = new TreeMap<>();
        for (String packId : PACK_IDS) {
            Path pack = PACKS.resolve(packId);
            for (LocatedTrain located : readTrains(pack)) {
                JsonObject train = located.definition();
                String trainId = train.get("id").getAsString();
                JsonObject creative = train.getAsJsonObject("creative");
                actual.put(trainId, creative.get("tab").getAsString());

                Set<String> selectors = new LinkedHashSet<>();
                for (JsonElement selector
                        : train.getAsJsonObject("members").getAsJsonArray("item_tags")) {
                    selectors.add(selector.getAsString());
                }
                if ("kuayue:freight".equals(trainId)) {
                    assertEquals(
                            Set.of(
                                    "kuayue:system/parcel/"
                                            + packId + "/kuayue/c70",
                                    "kuayue:system/parcel/"
                                            + packId + "/kuayue/nx70"
                            ),
                            selectors
                    );
                } else {
                    assertTrue(
                            selectors.contains(
                                    "kuayue:system/parcel/"
                                            + packId + "/" + trainId.replace(':', '/')
                            ),
                            trainId
                    );
                }
            }

            JsonObject manifest = DataJson.GSON.fromJson(
                    Files.readString(pack.resolve("pack.json")),
                    JsonObject.class
            );
            for (JsonElement entry : manifest.getAsJsonArray("parcels")) {
                JsonObject parcel = DataJson.GSON.fromJson(
                        Files.readString(pack.resolve(entry.getAsString())),
                        JsonObject.class
                );
                assertFalse(parcel.has("creative_filter"), entry.getAsString());
            }
        }
        assertEquals(EXPECTED_TRAIN_TABS, actual);
    }

    @Test
    void modelTexturesResolveWithinDeclaredPresetClosure() throws IOException {
        for (String packId : PACK_IDS) {
            Path pack = PACKS.resolve(packId);
            Set<Path> resourceRoots = declaredResourceRoots(pack);
            Set<String> availableTextures = collectTexturePaths(resourceRoots);

            try (var paths = Files.walk(pack.resolve("parcels"))) {
                for (Path model : paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .filter(path -> path.toString().contains("/models/"))
                        .toList()) {
                    JsonObject json = DataJson.GSON.fromJson(
                            Files.readString(model),
                            JsonObject.class
                    );
                    if (!json.has("textures")) continue;
                    for (var texture : json.getAsJsonObject("textures").entrySet()) {
                        String location = texture.getValue().getAsString();
                        if (!location.startsWith("kuayue:")) continue;
                        String texturePath = location.substring("kuayue:".length()) + ".png";
                        assertTrue(
                                availableTextures.contains(texturePath),
                                () -> model + " references unavailable texture " + location
                        );
                    }
                }
            }
        }
    }

    @Test
    void modelReferencesResolveWithinDeclaredPresetClosure() throws IOException {
        for (String packId : PACK_IDS) {
            Path pack = PACKS.resolve(packId);
            Set<String> availableModels = collectModelPaths(declaredResourceRoots(pack));
            try (var paths = Files.walk(pack)) {
                for (Path jsonPath : paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .toList()) {
                    JsonObject json = DataJson.GSON.fromJson(
                            Files.readString(jsonPath),
                            JsonObject.class
                    );
                    assertModelReference(jsonPath, json.get("parent"), availableModels);
                    if (json.has("variants")) {
                        for (var variant : json.getAsJsonObject("variants").entrySet()) {
                            if (!variant.getValue().isJsonObject()) continue;
                            assertModelReference(
                                    jsonPath,
                                    variant.getValue().getAsJsonObject().get("model"),
                                    availableModels
                            );
                        }
                    }
                    if (json.has("multipart")) {
                        for (JsonElement part : json.getAsJsonArray("multipart")) {
                            if (!part.isJsonObject()
                                    || !part.getAsJsonObject().has("apply")) continue;
                            JsonObject apply = part.getAsJsonObject().getAsJsonObject("apply");
                            assertModelReference(jsonPath, apply.get("model"), availableModels);
                        }
                    }
                }
            }
        }
    }

    private static Set<Path> declaredResourceRoots(Path pack) throws IOException {
        Set<Path> roots = new HashSet<>();
        roots.add(pack);
        roots.add(CORE_RESOURCES);
        for (String category : Set.of("common", "seat", "toilet")) {
            roots.add(PACKS.getParent().resolve(category));
        }

        JsonObject manifest = DataJson.GSON.fromJson(
                Files.readString(pack.resolve("pack.json")),
                JsonObject.class
        );
        for (String dependency : manifest.getAsJsonObject("dependencies")
                .getAsJsonObject("packs")
                .keySet()) {
            roots.add(PACKS.resolve(dependency));
        }
        return roots;
    }

    private static List<LocatedTrain> readTrains(Path pack) throws IOException {
        List<LocatedTrain> trains = new ArrayList<>();
        JsonObject manifest = DataJson.GSON.fromJson(
                Files.readString(pack.resolve("pack.json")),
                JsonObject.class
        );
        for (JsonElement entry : manifest.getAsJsonArray("parcels")) {
            Path parcelManifestPath = pack.resolve(entry.getAsString());
            Path parcelRoot = parcelManifestPath.getParent();
            JsonObject parcel = DataJson.GSON.fromJson(
                    Files.readString(parcelManifestPath),
                    JsonObject.class
            );

            Set<String> declaredInIndexes = new HashSet<>();
            for (JsonElement indexEntry : parcel.getAsJsonArray("indexes")) {
                JsonObject index = DataJson.GSON.fromJson(
                        Files.readString(parcelRoot.resolve(indexEntry.getAsString())),
                        JsonObject.class
                );
                for (JsonElement registered : index.getAsJsonArray("on_register")) {
                    declaredInIndexes.add(registered.getAsString());
                }
            }

            Path trainDirectory = parcelRoot.resolve("data/kuayue/trains");
            Set<String> trainFiles = new HashSet<>();
            if (Files.isDirectory(trainDirectory)) {
                try (var paths = Files.list(trainDirectory)) {
                    for (Path file : paths
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".json"))
                            .toList()) {
                        trainFiles.add("trains/" + file.getFileName());
                        JsonObject root = DataJson.GSON.fromJson(
                                Files.readString(file),
                                JsonObject.class
                        );
                        assertEquals(1, root.getAsJsonArray("trains").size(), file.toString());
                        JsonObject train = root.getAsJsonArray("trains")
                                .get(0)
                                .getAsJsonObject();
                        trains.add(new LocatedTrain(pack.getFileName().toString(), file, train));
                    }
                }
            }
            assertEquals(trainFiles, new HashSet<>(declaredInIndexes.stream()
                    .filter(path -> path.startsWith("trains/"))
                    .toList()), parcelManifestPath.toString());
        }
        return List.copyOf(trains);
    }

    private static void assertModelReference(
            Path source,
            JsonElement reference,
            Set<String> availableModels
    ) {
        if (reference == null || !reference.isJsonPrimitive()) return;
        String location = reference.getAsString();
        if (!location.startsWith("kuayue:")) return;
        String modelPath = location.substring("kuayue:".length());
        if (KNOWN_UPSTREAM_MODEL_GAPS.contains(modelPath)) return;
        assertTrue(
                availableModels.contains(modelPath),
                () -> source + " references unavailable model " + location
        );
    }

    private static Set<String> collectTexturePaths(Set<Path> roots) throws IOException {
        Set<String> paths = new HashSet<>();
        for (Path root : roots) {
            try (var files = Files.walk(root)) {
                for (Path file : files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".png"))
                        .toList()) {
                    addTexturePath(paths, file);
                }
            }
        }
        return paths;
    }

    private static Set<String> collectModelPaths(Set<Path> roots) throws IOException {
        Set<String> paths = new HashSet<>();
        for (Path root : roots) {
            try (var files = Files.walk(root)) {
                for (Path file : files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .toList()) {
                    String normalized = file.toString().replace('\\', '/');
                    int marker = normalized.indexOf("/models/");
                    if (marker < 0) continue;
                    paths.add(normalized.substring(
                            marker + "/models/".length(),
                            normalized.length() - ".json".length()
                    ));
                }
            }
        }
        return paths;
    }

    private static void addTexturePath(Set<String> paths, Path file) {
        String normalized = file.toString().replace('\\', '/');
        int marker = normalized.indexOf("/textures/");
        if (marker < 0) return;
        paths.add(normalized.substring(marker + "/textures/".length()).toLowerCase(Locale.ROOT));
    }

    private record LocatedTrain(String packId, Path source, JsonObject definition) {
    }
}
