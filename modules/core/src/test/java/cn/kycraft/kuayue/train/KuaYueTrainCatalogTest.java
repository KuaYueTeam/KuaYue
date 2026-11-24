package cn.kycraft.kuayue.train;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KuaYueTrainCatalogTest {
    @AfterEach
    void resetCatalog() {
        KuaYueTrainCatalog.resetForTests();
    }

    @Test
    void exposesImmutableIdAndReverseIndexes() {
        KuaYueTrainDefinition definition = definition("kuayue:ss3");
        KuaYueTrainCatalog catalog = KuaYueTrainCatalog.of(List.of(definition));

        assertSame(definition, catalog.find("kuayue:ss3").orElseThrow());
        assertEquals(List.of(definition), catalog.all());
        assertEquals(List.of(definition), catalog.byBlock("kuayue:ss3_body"));
        assertEquals(List.of(definition), catalog.byItem("kuayue:ss3_item"));
        assertEquals(List.of(definition), catalog.byBogeyBlock("kuayue:ss3_bogey"));
        assertEquals(List.of(definition), catalog.byBogeyStyle("kuayue:ss3_style"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
    }

    @Test
    void reverseIndexesReturnEveryTrainInStableIdOrder() {
        KuaYueTrainDefinition ss3 = definition("kuayue:ss3");
        KuaYueTrainDefinition hxd3d = definition("kuayue:hxd3d");
        KuaYueTrainCatalog catalog = KuaYueTrainCatalog.of(List.of(ss3, hxd3d));

        assertEquals(
                List.of("kuayue:hxd3d", "kuayue:ss3"),
                catalog.byBlock("kuayue:ss3_body").stream()
                        .map(definition -> definition.id().toString())
                        .toList()
        );
    }

    @Test
    void currentCatalogCanBeAtomicallyReplacedAndReset() {
        KuaYueTrainCatalog first = KuaYueTrainCatalog.of(
                List.of(definition("kuayue:ss3"))
        );
        KuaYueTrainCatalog second = KuaYueTrainCatalog.of(
                List.of(definition("kuayue:hxd3d"))
        );

        KuaYueTrainCatalog previous = KuaYueTrainCatalog.install(first);
        assertTrue(previous.all().isEmpty());
        assertSame(first, KuaYueTrainCatalog.current());

        assertSame(first, KuaYueTrainCatalog.install(second));
        assertSame(second, KuaYueTrainCatalog.current());

        KuaYueTrainCatalog.resetForTests();
        assertTrue(KuaYueTrainCatalog.current().all().isEmpty());
    }

    private static KuaYueTrainDefinition definition(String id) {
        var fields = JsonParser.parseString("""
                {
                  "category":"locomotive",
                  "name_key":"train.kuayue.ss3.name",
                  "description_key":"train.kuayue.ss3.description",
                  "members":{
                  "blocks":["kuayue:ss3_body"],
                  "items":["kuayue:ss3_item"],
                  "item_tags":[],
                  "bogey_sizes":["kuayue:ss3_size"],
                  "bogey_motions":[],
                  "bogey_models":[],
                  "bogey_blocks":["kuayue:ss3_bogey"],
                  "bogey_styles":["kuayue:ss3_style"],
                  "bogey_style_variants":[]
                  }
                }
                """).getAsJsonObject();
        return new KuaYueTrainDefinition(
                ResourceLocation.parse(id),
                "locomotive",
                "train.kuayue.ss3.name",
                "train.kuayue.ss3.description",
                List.of(ResourceLocation.parse("kuayue:ss3_body")),
                List.of(ResourceLocation.parse("kuayue:ss3_item")),
                List.of(),
                List.of(ResourceLocation.parse("kuayue:ss3_size")),
                List.of(),
                List.of(),
                List.of(ResourceLocation.parse("kuayue:ss3_bogey")),
                List.of(ResourceLocation.parse("kuayue:ss3_style")),
                List.of(),
                fields,
                List.of(),
                Map.of()
        );
    }
}
