package cn.kycraft.kuayue.train;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainPatchParserTest {
    private final TrainPatchParser parser = new TrainPatchParser();

    @Test
    void preservesDeclaredFields() {
        TrainPatch patch = parser.parse(JsonParser.parseString("""
                {
                  "id": "kuayue:ss3",
                  "members": {
                    "items": ["kuayue:part"],
                    "item_tags": []
                  }
                }
                """).getAsJsonObject());

        assertEquals("kuayue:ss3", patch.id());
        assertTrue(patch.declares("/members/items"));
        assertTrue(patch.declares("members.item_tags"));
        assertFalse(patch.declares("members.blocks"));
    }

    @Test
    void rejectsIdsWithoutExplicitNamespaces() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse(JsonParser.parseString("""
                        {"id":"ss3"}
                        """).getAsJsonObject()));

        assertThrows(IllegalArgumentException.class, () ->
                parser.parse(JsonParser.parseString("""
                        {
                          "id":"kuayue:ss3",
                          "members":{"blocks":["part"]}
                        }
                        """).getAsJsonObject()));
    }

    @Test
    void rejectsUnknownFieldsAndDuplicateEntries() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse(JsonParser.parseString("""
                        {"id":"kuayue:ss3","unknown":true}
                        """).getAsJsonObject()));

        assertThrows(IllegalArgumentException.class, () ->
                parser.parse(JsonParser.parseString("""
                        {
                          "id":"kuayue:ss3",
                          "members":{"items":["kuayue:part","kuayue:part"]}
                        }
                        """).getAsJsonObject()));
    }
}
