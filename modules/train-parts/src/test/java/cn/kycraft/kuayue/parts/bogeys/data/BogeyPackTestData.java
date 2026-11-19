package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BogeyPackTestData {
    private static final Path PACKS = Path.of("resources/packs");
    private static final Set<String> BOGEY_FIELDS = Set.of(
            "bogey_sizes",
            "bogey_motions",
            "bogey_models",
            "bogey_blocks",
            "bogey_styles",
            "bogey_style_variants"
    );

    final List<BogeySizeDefinition> sizes;
    final List<BogeyMotionDefinition> motions;
    final List<BogeyModelDefinition> models;
    final List<BogeyBlockDefinition> blocks;
    final List<BogeyStyleDefinition> styles;
    final List<BogeyStyleVariantsDefinition> styleVariants;

    private BogeyPackTestData(
            List<BogeySizeDefinition> sizes,
            List<BogeyMotionDefinition> motions,
            List<BogeyModelDefinition> models,
            List<BogeyBlockDefinition> blocks,
            List<BogeyStyleDefinition> styles,
            List<BogeyStyleVariantsDefinition> styleVariants
    ) {
        this.sizes = List.copyOf(sizes);
        this.motions = List.copyOf(motions);
        this.models = List.copyOf(models);
        this.blocks = List.copyOf(blocks);
        this.styles = List.copyOf(styles);
        this.styleVariants = List.copyOf(styleVariants);
    }

    static BogeyPackTestData load() throws IOException {
        BogeyDefinitionParser parser = new BogeyDefinitionParser();
        List<BogeySizeDefinition> sizes = new ArrayList<>();
        List<BogeyMotionDefinition> motions = new ArrayList<>();
        List<BogeyModelDefinition> models = new ArrayList<>();
        List<BogeyBlockDefinition> blocks = new ArrayList<>();
        List<BogeyStyleDefinition> styles = new ArrayList<>();
        List<BogeyStyleVariantsDefinition> styleVariants = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(PACKS)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()) {
                JsonObject root = DataJson.GSON.fromJson(
                        Files.readString(path),
                        JsonObject.class
                );
                if (root == null || root.keySet().stream().noneMatch(BOGEY_FIELDS::contains)) {
                    continue;
                }

                BogeyDefinitionFile file = parser.parseFile(root);
                sizes.addAll(file.sizes());
                blocks.addAll(file.blocks());
                styles.addAll(file.styles());
                styleVariants.addAll(file.styleVariants());
                if (root.has("bogey_motions")) {
                    for (JsonElement motion : root.getAsJsonArray("bogey_motions")) {
                        motions.add(parser.parseMotion(motion.getAsJsonObject()));
                    }
                }
                if (root.has("bogey_models")) {
                    for (JsonElement model : root.getAsJsonArray("bogey_models")) {
                        models.add(parser.parseModel(model.getAsJsonObject()));
                    }
                }
            }
        }

        requireUnique(sizes, BogeySizeDefinition::id, "bogey size");
        requireUnique(motions, BogeyMotionDefinition::id, "bogey motion");
        requireUnique(models, BogeyModelDefinition::id, "bogey model");
        requireUnique(blocks, BogeyBlockDefinition::id, "bogey block");
        requireUnique(styles, BogeyStyleDefinition::id, "bogey style");
        requireUnique(
                styleVariants,
                BogeyStyleVariantsDefinition::id,
                "bogey style contribution"
        );

        Set<String> motionIds = motions.stream()
                .map(BogeyMotionDefinition::id)
                .collect(java.util.stream.Collectors.toSet());
        for (BogeyModelDefinition model : models) {
            for (BogeyModelDefinition.Part part : model.parts()) {
                if (part.motion() != null) {
                    assertTrue(
                            motionIds.contains(part.motion()),
                            () -> "Missing motion " + part.motion() + " for " + model.id()
                    );
                }
            }
        }

        return new BogeyPackTestData(
                sizes,
                motions,
                models,
                blocks,
                styles,
                styleVariants
        );
    }

    Map<String, List<BogeyStyleDefinition.Variant>> variantsByStyle() {
        Map<String, List<BogeyStyleDefinition.Variant>> variants =
                new LinkedHashMap<>();
        for (BogeyStyleDefinition style : styles) {
            variants.put(style.id(), new ArrayList<>(style.variants()));
        }
        for (BogeyStyleVariantsDefinition contribution : styleVariants) {
            variants.computeIfAbsent(contribution.style(), ignored -> new ArrayList<>())
                    .addAll(contribution.variants());
        }
        return variants;
    }

    List<BogeyStyleDefinition.Variant> allVariants() {
        return variantsByStyle().values().stream()
                .flatMap(List::stream)
                .toList();
    }

    private static <T> void requireUnique(
            List<T> values,
            Function<T, String> id,
            String description
    ) {
        Set<String> ids = new LinkedHashSet<>();
        for (T value : values) {
            assertTrue(
                    ids.add(id.apply(value)),
                    () -> "Duplicate " + description + " id " + id.apply(value)
            );
        }
    }
}
