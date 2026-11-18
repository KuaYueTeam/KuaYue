package cn.kycraft.kuayue.parts.packs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetPackSourceLayoutTest {
    private static final Path PACKS = Path.of("resources/packs");
    private static final String RUNNING_GEAR = "kuayue-preset-running-gear";
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
        assertEquals(32, parcelCount);
        assertFalse(Files.exists(Path.of("src/main/resources/kuayue-packs")));
    }
}
