package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonArray;
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
    private static final Path INDEX = Path.of(
            "resources/packs/kuayue-preset-running-gear/parcels/shared/"
                    + "data/kuayue/kasuga_lib/data_driven/bogeys.json"
    );
    private static final Path CONTENT = Path.of(
            "resources/packs/kuayue-preset-running-gear/parcels/shared/"
                    + "data/kuayue/bogeys/bogeys.json"
    );
    private static final Path GROUPS = Path.of(
            "resources/packs/kuayue-preset-running-gear/parcels/shared/"
                    + "data/kuayue/bogeys/groups.json"
    );

    @Test
    void documentAggregatesEveryShiftedBogeyDefinition() throws IOException {
        JsonObject index = readJson(INDEX);
        List<String> registered = index.getAsJsonArray("on_register").asList().stream()
                .map(JsonElement::getAsString)
                .toList();
        assertTrue(registered.containsAll(List.of(
                "bogeys/groups.json",
                "bogeys/bogeys.json",
                "bogeys/motions.json",
                "bogeys/models.json"
        )));

        BogeyPackTestData data = BogeyPackTestData.load();
        assertEquals(55, data.sizes.size());
        assertEquals(55, data.blocks.size());
        assertEquals(55, data.models.size());
        assertEquals(7, data.styles.size());
        assertFalse(data.styleVariants.isEmpty());

        List<BogeyStyleDefinition.Variant> variants = data.allVariants();
        assertEquals(55, variants.size());
        assertEquals(55, variants.stream()
                .map(BogeyStyleDefinition.Variant::size)
                .collect(Collectors.toSet())
                .size());
        assertEquals(55, variants.stream()
                .map(BogeyStyleDefinition.Variant::block)
                .collect(Collectors.toSet())
                .size());
        assertEquals(55, variants.stream()
                .map(BogeyStyleDefinition.Variant::model)
                .collect(Collectors.toSet())
                .size());

        Map<String, Long> variantCounts = data.variantsByStyle().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (long) entry.getValue().size(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        assertEquals(Map.of(
                "kuayue:carriage", 8L,
                "kuayue:meter_carriage", 5L,
                "kuayue:andesite_carriage", 8L,
                "kuayue:loco", 15L,
                "kuayue:asymmetry_loco", 1L,
                "kuayue:meter_loco", 6L,
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
        BogeyPackTestData data = BogeyPackTestData.load();
        Map<String, BogeyBlockDefinition> blocks = data.blocks.stream()
                .collect(Collectors.toMap(BogeyBlockDefinition::id, Function.identity()));
        Map<String, String> blockStyles = new LinkedHashMap<>();

        for (BogeyStyleDefinition style : data.styles) {
            for (BogeyStyleDefinition.Variant variant : style.variants()) {
                assertVariant(blocks, blockStyles, style.id(), variant);
            }
        }
        for (BogeyStyleVariantsDefinition contribution : data.styleVariants) {
            for (BogeyStyleDefinition.Variant variant : contribution.variants()) {
                assertVariant(blocks, blockStyles, contribution.style(), variant);
            }
        }

        assertEquals(blocks.keySet(), blockStyles.keySet());
        assertTrue(blocks.values().stream()
                .allMatch(block -> blockStyles.get(block.id()).equals(block.defaultStyle())));
    }

    @Test
    void physicalParametersMatchTheLegacyRegistrationTables() throws IOException {
        BogeyPackTestData data = BogeyPackTestData.load();
        Map<String, BogeySizeDefinition> sizes = data.sizes.stream()
                .collect(Collectors.toMap(BogeySizeDefinition::id, Function.identity()));

        assertEquals(21, data.blocks.stream()
                .filter(block -> block.registryGroup().equals("kuayue:carriage_bogeys"))
                .count());
        assertEquals(34, data.blocks.stream()
                .filter(block -> block.registryGroup().equals("kuayue:loco_bogeys"))
                .count());

        for (BogeyBlockDefinition block : data.blocks) {
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
        BogeyPackTestData data = BogeyPackTestData.load();
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

        Set<String> missing = data.blocks.stream()
                .map(block -> block.id().substring(block.id().indexOf(':') + 1))
                .filter(id -> !blockstateNames.contains(id))
                .collect(Collectors.toSet());
        assertTrue(missing.isEmpty(), () -> "Missing blockstates: " + missing);
    }

    @Test
    void parserRejectsInvalidFieldsButAllowsPartialDocuments() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        JsonObject document = readJson(CONTENT);
        JsonObject unknownTopLevel = document.deepCopy();
        unknownTopLevel.addProperty("bogey_style_variants", "not-a-list");
        assertThrows(IllegalArgumentException.class, () -> parser.parseFile(unknownTopLevel));

        JsonObject unknownBlockField = document.getAsJsonArray("bogey_blocks")
                .get(0)
                .getAsJsonObject()
                .deepCopy();
        unknownBlockField.addProperty("default_styles", "kuayue:carriage");
        assertThrows(IllegalArgumentException.class, () -> parser.parseBlock(unknownBlockField));

        JsonObject externalStyle = document.deepCopy();
        externalStyle.getAsJsonArray("bogey_blocks")
                .get(0)
                .getAsJsonObject()
                .addProperty("default_style", "kuayue:loco");
        assertEquals(25, parser.parseFile(externalStyle).blocks().size());

        JsonObject bareId = document.deepCopy();
        bareId.getAsJsonArray("bogey_blocks")
                .get(0)
                .getAsJsonObject()
                .addProperty("id", "pk209p_bogey");
        assertThrows(RuntimeException.class, () -> parser.parseFile(bareId));

        JsonObject emptyContribution = new JsonObject();
        emptyContribution.addProperty("id", "kuayue:empty");
        emptyContribution.addProperty("style", "kuayue:loco");
        emptyContribution.add("variants", new JsonArray());
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseStyleVariants(emptyContribution)
        );
    }

    private static void assertVariant(
            Map<String, BogeyBlockDefinition> blocks,
            Map<String, String> blockStyles,
            String style,
            BogeyStyleDefinition.Variant variant
    ) {
        assertFalse(blockStyles.containsKey(variant.block()));
        blockStyles.put(variant.block(), style);
        BogeyBlockDefinition block = blocks.get(variant.block());
        assertEquals(style, block.defaultStyle());
        assertEquals(block.size(), variant.size());
        assertTrue(variant.model().startsWith("kuayue:bogey/"));
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), () -> "Missing file " + path);
        return DataJson.GSON.fromJson(Files.readString(path), JsonObject.class);
    }
}
