package cn.kycraft.kuayue.parts.locomotives;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticLocomotiveDataDrivenDefinitionTest {
    private static final Map<String, Integer> EXPECTED_COUNTS = Map.of(
            "df5", 31,
            "df11", 23,
            "df11g", 36,
            "df21", 14,
            "ss3", 17,
            "ss8", 19,
            "jy290", 12
    );

    @Test
    void eachMigratedLocomotiveIndexesOnlyItsOwnDataDrivenDefinitions() throws IOException {
        for (Map.Entry<String, Integer> entry : EXPECTED_COUNTS.entrySet()) {
            String model = entry.getKey();
            Path root = modelRoot(model);
            JsonObject index = readJson(root.resolve(
                    "data/kuayue/kasuga_lib/data_driven/" + model + ".json"
            ));
            List<String> expectedIndexes = new ArrayList<>(List.of(
                    "locomotives/" + model + "/groups.json",
                    "locomotives/" + model + "/blocks.json"
            ));
            Path bogeyData = root.resolve("data/kuayue/bogeys/" + model + ".json");
            if (Files.isRegularFile(bogeyData)) {
                expectedIndexes.add("bogeys/" + model + ".json");
            }
            Path trainData = root.resolve("data/kuayue/trains/" + model + ".json");
            if (Files.isRegularFile(trainData)) {
                expectedIndexes.add("trains/" + model + ".json");
            }
            assertEquals(
                    expectedIndexes,
                    strings(index.getAsJsonArray("on_register")),
                    model
            );

            JsonObject groups = readJson(root.resolve(
                    "data/kuayue/locomotives/" + model + "/groups.json"
            ));
            assertEquals(1, groups.getAsJsonArray("registry_groups").size(), model);
            JsonObject group = groups.getAsJsonArray("registry_groups")
                    .get(0)
                    .getAsJsonObject();
            assertEquals("kuayue:" + model + "_panels", group.get("id").getAsString());
            assertEquals(
                    "kuayue:kuayue_loco",
                    group.getAsJsonObject("item_properties").get("tab").getAsString()
            );
            assertEquals(
                    8,
                    group.getAsJsonObject("item_properties").get("stacks_to").getAsInt()
            );

            JsonObject blocks = readJson(root.resolve(
                    "data/kuayue/locomotives/" + model + "/blocks.json"
            ));
            List<JsonObject> definitions = blocks.getAsJsonArray("blocks").asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .toList();
            assertEquals(entry.getValue(), definitions.size(), model);

            Set<String> blockIds = definitions.stream()
                    .map(block -> block.get("id").getAsString())
                    .collect(Collectors.toSet());
            Set<String> blockstateIds = blockstateIds(root);
            assertEquals(blockstateIds, blockIds, model);
            assertTrue(
                    definitions.stream().allMatch(block ->
                            block.get("registry_group").getAsString().equals("kuayue:" + model + "_panels")),
                    model
            );
        }
    }

    @Test
    void eachTakenOverParcelPointsAtItsDataDrivenIndex() throws IOException {
        for (String model : EXPECTED_COUNTS.keySet()) {
            JsonObject parcel = readJson(modelRoot(model).resolve("parcel.json"));
            assertEquals(
                    List.of("data/kuayue/kasuga_lib/data_driven/" + model + ".json"),
                    strings(parcel.getAsJsonArray("indexes")),
                    model
            );
        }
    }

    private static Path modelRoot(String model) {
        String preset = switch (model) {
            case "df5", "df11", "df11g", "df21" -> "kuayue-preset-df";
            case "ss3", "ss8" -> "kuayue-preset-ss";
            case "jy290" -> "kuayue-preset-industrial";
            default -> throw new IllegalArgumentException("Unknown locomotive: " + model);
        };
        return Path.of("resources/packs", preset, "parcels", model);
    }

    private static Set<String> blockstateIds(Path root) throws IOException {
        Path blockstates = root.resolve("blockstates");
        try (var paths = Files.list(blockstates)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().contains("bogey"))
                    .map(path -> path.getFileName().toString())
                    .map(name -> "kuayue:" + name.substring(0, name.length() - ".json".length()))
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), () -> "Missing file " + path);
        return DataJson.GSON.fromJson(Files.readString(path), JsonObject.class);
    }

    private static List<String> strings(com.google.gson.JsonArray array) {
        return array.asList().stream()
                .map(JsonElement::getAsString)
                .toList();
    }
}
