package cn.kycraft.kuayue.train;

import com.google.gson.JsonParser;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.source.LogicalPath;
import lib.kasuga.registration.data_driven.source.SourceDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainOverlayMergerTest {
    private final TrainPatchParser parser = new TrainPatchParser();
    private final TrainOverlayMerger merger = new TrainOverlayMerger();

    @Test
    void recursivelyMergesMultipleLayersAndKeepsProvenance() {
        LocatedDefinition<TrainPatch> base = located(
                "base_owner",
                10,
                1,
                """
                        {
                          "id":"kuayue:ss3",
                          "category":"locomotive",
                          "name_key":"train.kuayue.ss3.name",
                          "description_key":"train.kuayue.ss3.description",
                          "creative":{
                            "tab":"kuayue:kuayue_loco",
                            "icon_item":"kuayue:ss3_head"
                          },
                          "members":{
                            "blocks":["kuayue:ss3_body"],
                            "items":["kuayue:ss3_item"],
                            "item_tags":["kuayue:ss3_parts"],
                            "bogey_sizes":["kuayue:ss3_size"],
                            "bogey_motions":["kuayue:ss3_motion"],
                            "bogey_models":["kuayue:ss3_model"],
                            "bogey_blocks":["kuayue:ss3_bogey"],
                            "bogey_styles":["kuayue:ss3_style"],
                            "bogey_style_variants":["kuayue:ss3_variant"]
                          }
                        }
                        """
        );
        LocatedDefinition<TrainPatch> extension = located(
                "extension_owner",
                20,
                2,
                """
                        {
                          "id":"kuayue:ss3",
                          "creative":{"icon_item":"kuayue:ss3_new_head"}
                        }
                        """
        );
        LocatedDefinition<TrainPatch> override = located(
                "override_owner",
                30,
                3,
                """
                        {
                          "id":"kuayue:ss3",
                          "creative":{"tab":"external:transport"}
                        }
                        """
        );

        KuaYueTrainDefinition merged = merger.merge(List.of(override, base, extension));

        var creative = merged.fields().getAsJsonObject("creative");
        assertEquals("external:transport", creative.get("tab").getAsString());
        assertEquals("kuayue:ss3_new_head", creative.get("icon_item").getAsString());
        assertEquals(
                List.of("kuayue:ss3_body"),
                merged.blocks().stream().map(Object::toString).toList()
        );
        assertEquals(
                "base_owner",
                merged.fieldProvenance().get("/members/blocks").ownerId()
        );
        assertEquals(
                "extension_owner",
                merged.fieldProvenance().get("/creative/icon_item").ownerId()
        );
        assertEquals(
                "override_owner",
                merged.fieldProvenance().get("/creative/tab").ownerId()
        );
        assertEquals(
                List.of("base_owner", "extension_owner", "override_owner"),
                merged.contributors().stream()
                        .map(KuaYueTrainDefinition.Contributor::ownerId)
                        .toList()
        );
    }

    @Test
    void arrayReplacementAndEmptyArrayClear() {
        LocatedDefinition<TrainPatch> base = located(
                "base_owner",
                10,
                1,
                completeJson("""
                        "items":["kuayue:first","kuayue:second"]
                        """)
        );
        LocatedDefinition<TrainPatch> replacement = located(
                "replacement_owner",
                20,
                2,
                """
                        {
                          "id":"kuayue:ss3",
                          "members":{"items":["kuayue:replacement"]}
                        }
                        """
        );
        LocatedDefinition<TrainPatch> clear = located(
                "clear_owner",
                30,
                3,
                """
                        {
                          "id":"kuayue:ss3",
                          "members":{"items":[]}
                        }
                        """
        );

        KuaYueTrainDefinition merged = merger.merge(List.of(base, replacement, clear));

        assertTrue(merged.items().isEmpty());
        assertEquals(
                "clear_owner",
                merged.fieldProvenance().get("/members/items").ownerId()
        );
    }

    @Test
    void rejectsIncompleteMergedDefinition() {
        LocatedDefinition<TrainPatch> incomplete = located(
                "base_owner",
                10,
                1,
                """
                        {
                          "id":"kuayue:ss3",
                          "members":{
                            "blocks":[],
                            "items":[],
                            "item_tags":[],
                            "bogey_sizes":[],
                            "bogey_motions":[],
                            "bogey_models":[],
                            "bogey_blocks":[],
                            "bogey_style_variants":[]
                          }
                        }
                        """
        );

        TrainOverlayMerger.MergeException exception = assertThrows(
                TrainOverlayMerger.MergeException.class,
                () -> merger.merge(List.of(incomplete))
        );

        assertTrue(exception.violations().stream()
                .anyMatch(value -> value.contains("/members/bogey_styles")));
    }

    @Test
    void rejectsSameOwnerContributingTwice() {
        LocatedDefinition<TrainPatch> first = located(
                "same_owner",
                10,
                1,
                completeJson("")
        );
        LocatedDefinition<TrainPatch> second = located(
                "same_owner",
                20,
                2,
                """
                        {"id":"kuayue:ss3","members":{"items":[]}}
                        """
        );

        assertThrows(
                TrainOverlayMerger.MergeException.class,
                () -> merger.merge(List.of(first, second))
        );
    }

    private LocatedDefinition<TrainPatch> located(
            String owner,
            int sourceOrder,
            int declarationOrder,
            String json
    ) {
        TrainPatch patch = parser.parse(JsonParser.parseString(json).getAsJsonObject());
        SourceDescriptor source = SourceDescriptor.of(
                owner + "_source",
                owner,
                Set.of("kuayue")
        );
        return new LocatedDefinition<TrainPatch>(
                new DefinitionKey(TrainDataDrivenTypes.TRAINS, patch.id()),
                new TrainTypeHandler(),
                patch,
                source,
                sourceOrder,
                LogicalPath.of("trains/ss3.json"),
                "/trains/" + declarationOrder,
                declarationOrder
        );
    }

    private static String completeJson(String itemsMember) {
        String items = itemsMember.isBlank()
                ? "\"items\":[\"kuayue:ss3_item\"]"
                : itemsMember;
        return """
                {
                  "id":"kuayue:ss3",
                  "category":"locomotive",
                  "name_key":"train.kuayue.ss3.name",
                  "description_key":"train.kuayue.ss3.description",
                  "members":{
                    "blocks":["kuayue:ss3_body"],
                    %s,
                    "item_tags":["kuayue:ss3_parts"],
                    "bogey_sizes":["kuayue:ss3_size"],
                    "bogey_motions":["kuayue:ss3_motion"],
                    "bogey_models":["kuayue:ss3_model"],
                    "bogey_blocks":["kuayue:ss3_bogey"],
                    "bogey_styles":["kuayue:ss3_style"],
                    "bogey_style_variants":["kuayue:ss3_variant"]
                  }
                }
                """.formatted(items);
    }
}
