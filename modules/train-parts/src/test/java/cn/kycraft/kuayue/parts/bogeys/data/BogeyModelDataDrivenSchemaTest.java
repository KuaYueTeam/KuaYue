package cn.kycraft.kuayue.parts.bogeys.data;

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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BogeyModelDataDrivenSchemaTest {
    private static final Path CONTENT = Path.of(
            "resources/bogeys/data-driven/data/kuayue/bogeys/bogeys.json"
    );
    private static final Path MODELS = Path.of(
            "resources/bogeys/data-driven/data/kuayue/bogeys/models.json"
    );
    private static final Path MOTIONS = Path.of(
            "resources/bogeys/data-driven/data/kuayue/bogeys/motions.json"
    );
    private static final Path QJ_CLIP = Path.of(
            "resources/bogeys/data-driven/data/kuayue/animation_clips/qj_mechanism.json"
    );

    @Test
    void allStyleVariantsReferenceCompiledModelsAndMotionProfiles() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        List<BogeyMotionDefinition> motions = parser.parseMotionFile(readJson(MOTIONS));
        Set<String> motionIds = motions.stream()
                .map(BogeyMotionDefinition::id)
                .collect(Collectors.toSet());
        List<BogeyModelDefinition> models = parser.parseModelFile(
                readJson(MODELS),
                motionIds
        );
        BogeyDefinitionFile content = parser.parseFile(readJson(CONTENT));

        assertEquals(9, motions.size());
        assertEquals(53, models.size());
        assertEquals(241, models.stream().mapToInt(model -> model.parts().size()).sum());
        assertEquals(
                Map.of("wheel_spin", 7L, "rod_rotation", 1L, "formula", 1L),
                motions.stream().collect(Collectors.groupingBy(
                        BogeyMotionDefinition::type,
                        Collectors.counting()
                ))
        );

        Set<String> modelIds = models.stream()
                .map(BogeyModelDefinition::id)
                .collect(Collectors.toSet());
        Set<String> referencedModels = content.styles().stream()
                .flatMap(style -> style.variants().stream())
                .map(BogeyStyleDefinition.Variant::model)
                .collect(Collectors.toSet());
        assertEquals(modelIds, referencedModels);
        assertEquals(53, referencedModels.size());
        assertTrue(models.stream().allMatch(model -> model.yOffset() == -1.5078125f));
    }

    @Test
    void motionGoldenValuesPreserveWheelRodAndQjSemantics() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        Map<String, BogeyMotionDefinition> motions = parser.parseMotionFile(
                readJson(MOTIONS)
        ).stream().collect(Collectors.toMap(BogeyMotionDefinition::id, Function.identity()));

        assertEquals(3.256f, motions.get(
                "kuayue:motion/wheel_spin/scale_3.256_sign_-1"
        ).scale());
        assertEquals(-1f, motions.get(
                "kuayue:motion/wheel_spin/scale_3.256_sign_-1"
        ).sign());
        assertEquals(1.22f, motions.get(
                "kuayue:motion/wheel_spin/scale_1.22_sign_1"
        ).scale());
        assertEquals(1.09f, motions.get(
                "kuayue:motion/wheel_spin/scale_1.09_sign_1"
        ).scale());
        assertEquals(3.256f, motions.get(
                "kuayue:motion/rod_rotation/scale_3.256_sign_1"
        ).scale());

        assertEquals(
                "kuayue:qj_mechanism",
                motions.get("kuayue:motion/qj/formula").clip()
        );
        assertTrue(Files.isRegularFile(QJ_CLIP));
    }

    @Test
    void everyDeclaredModelFileExistsAndQjModelKeepsAllParts() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        Set<String> motionIds = parser.parseMotionFile(readJson(MOTIONS)).stream()
                .map(BogeyMotionDefinition::id)
                .collect(Collectors.toSet());
        List<BogeyModelDefinition> models = parser.parseModelFile(
                readJson(MODELS),
                motionIds
        );

        Set<String> resources = new HashSet<>();
        try (var paths = Files.walk(Path.of("resources"))) {
            paths.filter(Files::isRegularFile)
                    .map(path -> path.toString().replace('\\', '/'))
                    .filter(path -> path.contains("/models/") && path.endsWith(".obj"))
                    .forEach(path -> resources.add(
                            path.substring(path.indexOf("/models/") + 1)
                    ));
        }

        Set<String> missing = models.stream()
                .flatMap(model -> model.parts().stream())
                .map(BogeyModelDefinition.Part::model)
                .map(id -> id.substring(id.indexOf(':') + 1))
                .filter(id -> !resources.contains(id))
                .collect(Collectors.toSet());
        assertTrue(missing.isEmpty(), () -> "Missing model resources: " + missing);

        BogeyModelDefinition qjMain = models.stream()
                .filter(model -> model.id().equals("kuayue:bogey/qj_main"))
                .findFirst()
                .orElseThrow();
        assertEquals(31, qjMain.parts().size());
        assertEquals(12, qjMain.parts().stream()
                .filter(part -> part.motion() != null
                        && part.motion().startsWith("kuayue:motion/qj/"))
                .count());
    }

    @Test
    void parserRejectsUnknownFieldsAndMissingCrossReferences() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        JsonObject motionsDocument = readJson(MOTIONS);
        JsonObject unknownMotion = motionsDocument.getAsJsonArray("bogey_motions")
                .get(0)
                .getAsJsonObject()
                .deepCopy();
        unknownMotion.addProperty("speed", 2.0);
        assertThrows(IllegalArgumentException.class, () -> parser.parseMotion(unknownMotion));

        JsonObject missingClip = new JsonObject();
        missingClip.addProperty("id", "kuayue:motion/qj/missing_clip");
        missingClip.addProperty("type", "formula");
        assertThrows(IllegalArgumentException.class, () -> parser.parseMotion(missingClip));

        JsonObject modelsDocument = readJson(MODELS);
        JsonObject unknownPart = modelsDocument.getAsJsonArray("bogey_models")
                .get(0)
                .getAsJsonObject()
                .getAsJsonArray("parts")
                .get(0)
                .getAsJsonObject()
                .deepCopy();
        unknownPart.addProperty("material", "kuayue:missing");
        assertThrows(IllegalArgumentException.class, () -> parser.parseModel(unknownPart));

        Set<String> knownMotions = parser.parseMotionFile(motionsDocument).stream()
                .map(BogeyMotionDefinition::id)
                .collect(Collectors.toSet());
        knownMotions.remove("kuayue:motion/wheel_spin/scale_1.0_sign_1");
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseModelFile(modelsDocument, knownMotions)
        );
    }

    @Test
    void localOperationsPreserveTheLegacyOrder() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        Set<String> motionIds = parser.parseMotionFile(readJson(MOTIONS)).stream()
                .map(BogeyMotionDefinition::id)
                .collect(Collectors.toSet());
        BogeyModelDefinition backwardAndesite = parser.parseModelFile(
                readJson(MODELS),
                motionIds
        ).stream()
                .filter(model -> model.id().equals(
                        "kuayue:bogey/pk209p_backward_a"
                ))
                .findFirst()
                .orElseThrow();

        List<BogeyModelDefinition.LocalOperation> frame =
                backwardAndesite.parts().getFirst().local();
        assertEquals(List.of(0f, 180f, 0f), frame.get(0).rotate());
        assertEquals(List.of(1.2f, 1f, 1f), frame.get(1).scale());
        assertEquals(List.of(0f, 0.91f, 0f), frame.get(2).translate());
        assertEquals(
                List.of(0.998046875f, 0.998046875f, 0.998046875f),
                frame.get(3).scale()
        );
        assertFalse(frame.stream().anyMatch(operation -> operation.rotate() != null
                && operation.rotate().get(1) == 180f
                && operation != frame.get(0)));
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), () -> "Missing file " + path);
        return DataJson.GSON.fromJson(Files.readString(path), JsonObject.class);
    }
}
