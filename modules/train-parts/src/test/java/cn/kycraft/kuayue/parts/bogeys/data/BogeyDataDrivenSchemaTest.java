package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BogeyDataDrivenSchemaTest {
    private static final Path DATA = Path.of(
            "resources/bogeys/data-driven/data/kuayue/kasuga_lib/data_driven/bogeys.json"
    );
    private static final Path CONTENT = Path.of(
            "resources/bogeys/data-driven/data/kuayue/bogeys/bogeys.json"
    );
    private static final Path GROUPS = Path.of(
            "resources/bogeys/data-driven/data/kuayue/bogeys/groups.json"
    );

    @Test
    void documentPreservesEveryExistingSizeBlockAndStyleVariant() throws IOException {
        JsonObject index = readJson(DATA);
        assertEquals(
                List.of(
                        "bogeys/groups.json",
                        "bogeys/bogeys.json",
                        "bogeys/motions.json",
                        "bogeys/models.json"
                ),
                index.getAsJsonArray("on_register").asList().stream()
                        .map(JsonElement::getAsString)
                        .toList()
        );

        BogeyDefinitionFile file = new BogeyDefinitionParser().parseFile(readJson(CONTENT));
        assertEquals(53, file.sizes().size());
        assertEquals(53, file.blocks().size());
        assertEquals(7, file.styles().size());
        assertEquals(53, file.styles().stream().mapToInt(style -> style.variants().size()).sum());

        Map<String, Long> variantCounts = file.styles().stream().collect(Collectors.toMap(
                BogeyStyleDefinition::id,
                style -> (long) style.variants().size(),
                (left, right) -> left,
                LinkedHashMap::new
        ));
        assertEquals(Map.of(
                "kuayue:carriage", 8L,
                "kuayue:meter_carriage", 5L,
                "kuayue:andesite_carriage", 8L,
                "kuayue:loco", 15L,
                "kuayue:asymmetry_loco", 1L,
                "kuayue:meter_loco", 4L,
                "kuayue:andesite_loco", 12L
        ), variantCounts);

        JsonObject groups = readJson(GROUPS);
        assertEquals(
                Set.of("kuayue:bogey_data", "kuayue:carriage_bogeys", "kuayue:loco_bogeys"),
                groups.getAsJsonArray("registry_groups").asList().stream()
                        .map(JsonElement::getAsJsonObject)
                        .map(group -> group.get("id").getAsString())
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void styleVariantsAreOneToOneAndMatchBlockDefaults() throws IOException {
        BogeyDefinitionFile file = new BogeyDefinitionParser().parseFile(readJson(CONTENT));
        Map<String, BogeyBlockDefinition> blocks = file.blocks().stream()
                .collect(Collectors.toMap(BogeyBlockDefinition::id, Function.identity()));
        Map<String, String> blockStyles = new LinkedHashMap<>();

        for (BogeyStyleDefinition style : file.styles()) {
            for (BogeyStyleDefinition.Variant variant : style.variants()) {
                assertFalse(blockStyles.containsKey(variant.block()));
                blockStyles.put(variant.block(), style.id());
                BogeyBlockDefinition block = blocks.get(variant.block());
                assertEquals(style.id(), block.defaultStyle());
                assertEquals(block.size(), variant.size());
                assertTrue(variant.model().startsWith("kuayue:bogey/"));
            }
        }

        assertEquals(blocks.keySet(), blockStyles.keySet());
        assertTrue(blocks.values().stream()
                .allMatch(block -> blockStyles.get(block.id()).equals(block.defaultStyle())));
    }

    @Test
    void physicalParametersMatchTheLegacyRegistrationTables() throws IOException {
        BogeyDefinitionFile file = new BogeyDefinitionParser().parseFile(readJson(CONTENT));
        Map<String, BogeySizeDefinition> sizes = file.sizes().stream()
                .collect(Collectors.toMap(BogeySizeDefinition::id, Function.identity()));

        assertEquals(21, file.blocks().stream()
                .filter(block -> block.registryGroup().equals("kuayue:carriage_bogeys"))
                .count());
        assertEquals(32, file.blocks().stream()
                .filter(block -> block.registryGroup().equals("kuayue:loco_bogeys"))
                .count());

        for (BogeyBlockDefinition block : file.blocks()) {
            BogeyBlockDefinition.Physics physics = block.physics();
            assertEquals(0.4575f, sizes.get(block.size()).wheelRadius());
            assertEquals("standard", physics.primaryTrackType());
            assertEquals(List.of("standard"), physics.validTrackTypes());
            assertEquals(List.of(0.0, 0.21875, 1.0), physics.connectorAnchorOffset());

            boolean carriage = block.registryGroup().equals("kuayue:carriage_bogeys");
            assertEquals(carriage, physics.allowSingleBogeyCarriage(), block.id());
            double expectedSpacing = carriage
                    && (block.id().equals("kuayue:cw2_bogey")
                    || block.id().equals("kuayue:cw2_bogey_a"))
                    ? 2.0
                    : carriage ? 2.5 : 2.0;
            assertEquals(expectedSpacing, physics.wheelPointSpacing(), block.id());
        }
    }

    @Test
    void everyBlockHasItsExistingBlockstateResource() throws IOException {
        BogeyDefinitionFile file = new BogeyDefinitionParser().parseFile(readJson(CONTENT));
        Set<String> blockstateNames = new HashSet<>();
        try (var paths = Files.walk(Path.of("resources"))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getParent() != null)
                    .filter(path -> path.getParent().getFileName().toString().equals("blockstates"))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5))
                    .forEach(blockstateNames::add);
        }

        Set<String> missing = file.blocks().stream()
                .map(block -> block.id().substring(block.id().indexOf(':') + 1))
                .filter(id -> !blockstateNames.contains(id))
                .collect(Collectors.toSet());
        assertTrue(missing.isEmpty(), () -> "Missing blockstates: " + missing);
    }

    @Test
    void parserRejectsUnknownFieldsAndBrokenCrossReferences() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        JsonObject document = readJson(CONTENT);
        JsonObject unknownTopLevel = document.deepCopy();
        unknownTopLevel.addProperty("bogey_motions", "not-a-list");
        assertThrows(IllegalArgumentException.class, () -> parser.parseFile(unknownTopLevel));

        JsonObject unknownBlockField = document.getAsJsonArray("bogey_blocks")
                .get(0)
                .getAsJsonObject()
                .deepCopy();
        unknownBlockField.addProperty("default_styles", "kuayue:carriage");
        assertThrows(IllegalArgumentException.class, () -> parser.parseBlock(unknownBlockField));

        JsonObject wrongDefaultStyle = document.deepCopy();
        wrongDefaultStyle.getAsJsonArray("bogey_blocks")
                .get(0)
                .getAsJsonObject()
                .addProperty("default_style", "kuayue:loco");
        assertThrows(IllegalArgumentException.class, () -> parser.parseFile(wrongDefaultStyle));

        JsonObject bareId = document.deepCopy();
        bareId.getAsJsonArray("bogey_blocks")
                .get(0)
                .getAsJsonObject()
                .addProperty("id", "pk209p_bogey");
        assertThrows(RuntimeException.class, () -> parser.parseFile(bareId));
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), () -> "Missing file " + path);
        return DataJson.GSON.fromJson(Files.readString(path), JsonObject.class);
    }
}
