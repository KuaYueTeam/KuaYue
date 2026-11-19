package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.source.LogicalPath;
import lib.kasuga.registration.data_driven.source.SourceDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BogeyStyleVariantsTest {
    private static final String STYLE = "kuayue:loco";
    private static final String SIZE = "kuayue:ss3";
    private static final String BLOCK = "kuayue:ss3_bogey";
    private static final String MODEL = "kuayue:bogey/ss3";

    @Test
    void partialDocumentKeepsContributionOwnedByVehiclePack() {
        JsonObject root = new JsonObject();
        root.add("bogey_sizes", array(sizeJson()));
        root.add("bogey_blocks", array(blockJson()));
        root.add("bogey_style_variants", array(contributionJson()));

        BogeyDefinitionFile file = new BogeyDefinitionParser().parseFile(root);

        assertEquals(1, file.sizes().size());
        assertEquals(1, file.blocks().size());
        assertTrue(file.styles().isEmpty());
        assertEquals(STYLE, file.styleVariants().getFirst().style());
    }

    @Test
    void contributionParentAlwaysPointsToExistingStyleDomain() {
        BogeyStyleVariantsDefinition contribution = contribution(
                "kuayue:ss3_loco",
                variant()
        );

        DefinitionKey parent = new BogeyStyleVariantsTypeHandler()
                .parentDefinition(contribution);

        assertEquals(new DefinitionKey(BogeyDataDrivenTypes.STYLES, STYLE), parent);
    }

    @Test
    void duplicatesAcrossSourceOwnersUseStableSourceOrder() {
        BogeyStyleVariantsDefinition first = contribution(
                "kuayue:first_loco",
                variant()
        );
        BogeyStyleVariantsDefinition second = contribution(
                "kuayue:second_loco",
                variant()
        );
        LocatedDefinition<BogeyStyleVariantsDefinition> firstDefinition =
                definition(first, "vehicle-a", 1, 0);
        LocatedDefinition<BogeyStyleVariantsDefinition> secondDefinition =
                definition(second, "vehicle-b", 2, 0);

        List<BogeyStyleVariantsTypeHandler.Conflict> conflicts =
                BogeyStyleVariantsTypeHandler.duplicateConflicts(
                        "kuayue",
                        second,
                        null,
                        secondDefinition,
                        List.of(firstDefinition, secondDefinition)
                );

        assertTrue(conflicts.stream()
                .anyMatch(conflict -> conflict.message().contains(
                        "size kuayue:ss3 is already supplied"
                )));
        assertTrue(conflicts.stream()
                .anyMatch(conflict -> conflict.message().contains(
                        "block kuayue:ss3_bogey is already supplied"
                )));
    }

    @Test
    void overriddenContributionDoesNotParticipateInDuplicateChecks() {
        BogeyStyleVariantsDefinition contribution = contribution(
                "kuayue:ss3_loco",
                variant()
        );
        LocatedDefinition<BogeyStyleVariantsDefinition> lowerPriority =
                definition(contribution, "vehicle", 1, 0);
        LocatedDefinition<BogeyStyleVariantsDefinition> winner =
                definition(contribution, "vehicle", 2, 0);

        List<BogeyStyleVariantsTypeHandler.Conflict> conflicts =
                BogeyStyleVariantsTypeHandler.duplicateConflicts(
                        "kuayue",
                        contribution,
                        null,
                        winner,
                        List.of(lowerPriority, winner)
                );

        assertTrue(conflicts.isEmpty(), conflicts.toString());
    }

    @Test
    void blockDeclarationIgnoresOverriddenContribution() {
        BogeyStyleDefinition.Variant lowerVariant =
                new BogeyStyleDefinition.Variant(SIZE, BLOCK, MODEL);
        BogeyStyleDefinition.Variant winningVariant =
                new BogeyStyleDefinition.Variant(SIZE, "kuayue:winning_bogey", MODEL);
        LocatedDefinition<BogeyStyleVariantsDefinition> lowerPriority = definition(
                contribution("kuayue:ss3_loco", lowerVariant),
                "vehicle",
                1,
                0
        );
        LocatedDefinition<BogeyStyleVariantsDefinition> winner = definition(
                contribution("kuayue:ss3_loco", winningVariant),
                "vehicle",
                2,
                0
        );

        assertFalse(BogeyStyleVariantsTypeHandler.declaresBlock(
                "kuayue",
                STYLE,
                BLOCK,
                List.of(lowerPriority, winner),
                key -> winner
        ));
        assertTrue(BogeyStyleVariantsTypeHandler.declaresBlock(
                "kuayue",
                STYLE,
                "kuayue:winning_bogey",
                List.of(lowerPriority, winner),
                key -> winner
        ));
    }

    @Test
    void applyAggregationAppendsVehicleVariantsAfterBaseVariants() {
        BogeyStyleVariantAggregation aggregation =
                new BogeyStyleVariantAggregation(STYLE);
        BogeyStyleDefinition.Variant base = new BogeyStyleDefinition.Variant(
                "kuayue:base",
                "kuayue:base_bogey",
                "kuayue:bogey/base"
        );
        BogeyStyleDefinition.Variant vehicle = variant();

        aggregation.add(base, "base style " + STYLE);
        aggregation.add(vehicle, "contribution kuayue:ss3_loco");

        assertEquals(List.of(base, vehicle), aggregation.variants());
        assertEquals(2, aggregation.size());
    }

    @Test
    void duplicateAgainstBaseStyleIsReported() {
        BogeyStyleDefinition style = new BogeyStyleDefinition(
                STYLE,
                "kuayue:cycle",
                null,
                "kuayue.bogey.style.loco",
                List.of(variant())
        );
        BogeyStyleVariantsDefinition contribution = contribution(
                "kuayue:ss3_loco",
                variant()
        );
        LocatedDefinition<BogeyStyleVariantsDefinition> current =
                definition(contribution, "vehicle", 2, 0);

        List<BogeyStyleVariantsTypeHandler.Conflict> conflicts =
                BogeyStyleVariantsTypeHandler.duplicateConflicts(
                        "kuayue",
                        contribution,
                        style,
                        current,
                        List.of(current)
                );

        assertEquals(2, conflicts.size());
        assertTrue(conflicts.stream()
                .allMatch(conflict -> conflict.message().contains("base style")));
    }

    @Test
    void wrongBlockDefaultStyleIsReported() {
        BogeyBlockDefinition block = block("kuayue:carriage");

        List<BogeyStyleVariantsTypeHandler.Conflict> conflicts =
                BogeyStyleVariantsTypeHandler.blockConflicts(
                        "kuayue",
                        STYLE,
                        0,
                        variant(),
                        block
                );

        assertTrue(conflicts.stream()
                .anyMatch(conflict -> conflict.message().contains(
                        "belongs to another default style"
                )));
    }

    @Test
    void emptyContributionIsRejectedBeforeGsonConstruction() {
        JsonObject contribution = contributionJson();
        contribution.add("variants", new JsonArray());

        assertThrows(
                IllegalArgumentException.class,
                () -> new BogeyDefinitionParser().parseStyleVariants(contribution)
        );
    }

    private static LocatedDefinition<BogeyStyleVariantsDefinition> definition(
            BogeyStyleVariantsDefinition contribution,
            String owner,
            int sourceOrder,
            int declarationOrder
    ) {
        SourceDescriptor source = SourceDescriptor.of(
                owner + "-source",
                owner,
                Set.of("kuayue")
        );
        return new LocatedDefinition<>(
                new DefinitionKey(
                        BogeyDataDrivenTypes.STYLE_VARIANTS,
                        contribution.id()
                ),
                new FixedTypeHandler<>(BogeyDataDrivenTypes.STYLE_VARIANTS),
                contribution,
                source,
                sourceOrder,
                LogicalPath.of(
                        "data/kuayue/bogeys/" + owner + "_" + sourceOrder + ".json"
                ),
                "/bogey_style_variants/0",
                declarationOrder
        );
    }

    private static JsonObject sizeJson() {
        JsonObject size = new JsonObject();
        size.addProperty("id", SIZE);
        size.addProperty("wheel_radius", 0.4575);
        return size;
    }

    private static JsonObject blockJson() {
        JsonObject block = new JsonObject();
        block.addProperty("id", BLOCK);
        block.addProperty("size", SIZE);
        block.addProperty("default_style", STYLE);
        JsonObject physics = new JsonObject();
        physics.addProperty("wheel_point_spacing", 2.0);
        physics.addProperty("allow_single_bogey_carriage", false);
        block.add("physics", physics);
        return block;
    }

    private static JsonObject contributionJson() {
        JsonObject contribution = new JsonObject();
        contribution.addProperty("id", "kuayue:ss3_loco");
        contribution.addProperty("style", STYLE);
        JsonObject variant = new JsonObject();
        variant.addProperty("size", SIZE);
        variant.addProperty("block", BLOCK);
        variant.addProperty("model", MODEL);
        contribution.add("variants", array(variant));
        return contribution;
    }

    private static JsonArray array(JsonObject value) {
        JsonArray values = new JsonArray();
        values.add(value);
        return values;
    }

    private static BogeyStyleVariantsDefinition contribution(
            String id,
            BogeyStyleDefinition.Variant variant
    ) {
        return new BogeyStyleVariantsDefinition(id, STYLE, List.of(variant));
    }

    private static BogeyStyleDefinition.Variant variant() {
        return new BogeyStyleDefinition.Variant(SIZE, BLOCK, MODEL);
    }

    private static BogeyBlockDefinition block(String defaultStyle) {
        return new BogeyBlockDefinition(
                BLOCK,
                SIZE,
                null,
                defaultStyle,
                new JsonObject(),
                new BogeyBlockDefinition.Physics(
                        "standard",
                        List.of("create:standard"),
                        2.0,
                        List.of(0.0, 0.21875, 1.0),
                        false
                )
        );
    }

    private record FixedTypeHandler<T>(String type) implements TypeHandler<T> {
        @Override
        public String getTypeName() {
            return type;
        }

        @Override
        public int getPhase() {
            return 0;
        }

        @Override
        public T parse(JsonObject json) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void apply(
                T definition,
                lib.kasuga.registration.data_driven.context.BuildContext context
        ) {
            throw new UnsupportedOperationException();
        }
    }
}
