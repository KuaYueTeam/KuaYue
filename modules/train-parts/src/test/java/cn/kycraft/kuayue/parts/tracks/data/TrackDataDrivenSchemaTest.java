package cn.kycraft.kuayue.parts.tracks.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackDataDrivenSchemaTest {

    private static final Path DATA = Path.of(
            "resources/tracks/data/kuayue/kasuga_lib/data_driven/tracks.json"
    );
    private static final Path CONTENT = Path.of(
            "resources/tracks/data/kuayue/tracks/tracks.json"
    );

    @Test
    void indexAndContentDescribeTheFiveCurrentTracks() throws IOException {
        assertTrue(Files.isRegularFile(DATA));
        assertTrue(Files.isRegularFile(CONTENT));

        JsonObject index = DataJson.GSON.fromJson(Files.readString(DATA), JsonObject.class);
        assertEquals(
                List.of("tracks/tracks.json"),
                index.getAsJsonArray("on_register").asList().stream()
                        .map(JsonElement::getAsString)
                        .toList()
        );

        String content = Files.readString(CONTENT);
        JsonObject document = DataJson.GSON.fromJson(content, JsonObject.class);
        TrackDefinitionFile file = DataJson.GSON.fromJson(
                content,
                TrackDefinitionFile.class
        );
        assertEquals(1, file.registryGroups().size());
        assertEquals(
                "kuayue:kuayue_temp_tab",
                file.registryGroups().getFirst()
                        .getAsJsonObject("item_properties")
                        .get("tab")
                        .getAsString()
        );
        assertEquals(5, file.tracks().size());
        assertEquals(Set.of(
                "kuayue:standard_track",
                "kuayue:ballastless_track",
                "kuayue:tieless_track",
                "kuayue:meter_track",
                "kuayue:guard_track"
        ), file.tracks().stream().map(TrackDefinition::id).collect(java.util.stream.Collectors.toSet()));

        for (TrackDefinition track : file.tracks()) {
            assertEquals("simple_track", track.type());
            assertEquals("kuayue:track_blocks", track.registryGroup());
            assertFalse(track.defaultBogey().isBlank());
            assertTrue(track.material().particle().startsWith("kuayue:"));
            assertTrue(track.material().halfDistance() > 0);
            assertFalse(track.material().sleepers().isEmpty());
            assertEquals(1, track.material().sleepers().size());
        }

        Map<String, String> expectedBogeys = Map.of(
                "kuayue:standard_track", "kuayue:pk209p_bogey",
                "kuayue:ballastless_track", "kuayue:pk209p_bogey",
                "kuayue:tieless_track", "kuayue:pk209p_bogey",
                "kuayue:meter_track", "kuayue:mkz_bogey",
                "kuayue:guard_track", "kuayue:pk209p_bogey"
        );
        Map<String, String> expectedMaterials = Map.of(
                "kuayue:standard_track", "kuayue:standard",
                "kuayue:ballastless_track", "kuayue:ballastless",
                "kuayue:tieless_track", "kuayue:tieless",
                "kuayue:meter_track", "kuayue:meter",
                "kuayue:guard_track", "kuayue:guard"
        );
        for (TrackDefinition track : file.tracks()) {
            assertEquals(expectedBogeys.get(track.id()), track.defaultBogey());
            assertEquals(expectedMaterials.get(track.id()), track.material().id());
        }
        assertEquals(
                0.525f,
                file.tracks().stream()
                        .filter(track -> track.id().equals("kuayue:meter_track"))
                        .findFirst()
                        .orElseThrow()
                        .material()
                        .halfDistance()
        );

        TrackDefinitionParser parser = new TrackDefinitionParser();
        document.getAsJsonArray("tracks").forEach(element ->
                assertEquals(
                        element.getAsJsonObject().get("id").getAsString(),
                        parser.parse(element.getAsJsonObject()).id()
                )
        );

        JsonObject unknownField = document.getAsJsonArray("tracks")
                .get(0)
                .getAsJsonObject()
                .deepCopy();
        unknownField.addProperty("default_bogeys", "kuayue:pk209p_bogey");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(unknownField));

        JsonObject bareId = document.getAsJsonArray("tracks")
                .get(0)
                .getAsJsonObject()
                .deepCopy();
        bareId.addProperty("id", "standard_track");
        assertThrows(RuntimeException.class, () -> parser.parse(bareId));
    }

    @Test
    void everyDeclaredTrackModelExistsInTheResourceTree() throws IOException {
        TrackDefinitionFile file = DataJson.GSON.fromJson(
                Files.readString(CONTENT),
                TrackDefinitionFile.class
        );

        Set<String> modelPaths = new HashSet<>();
        try (var paths = Files.walk(Path.of("resources"))) {
            paths.filter(Files::isRegularFile)
                    .map(path -> path.toString().replace('\\', '/'))
                    .filter(path -> path.contains("/models/") && path.endsWith(".json"))
                    .forEach(path -> modelPaths.add(path.substring(path.indexOf("/models/") + 8, path.length() - 5)));
        }

        for (TrackDefinition track : file.tracks()) {
            TrackDefinition.Models models = track.material().models();
            for (String model : List.of(models.tie(), models.left(), models.right())) {
                String path = model.substring(model.indexOf(':') + 1);
                assertTrue(modelPaths.contains(path), () -> "Missing model " + model + " for " + track.id());
            }
        }
    }
}
