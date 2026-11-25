package cn.kycraft.kuayue.train;

import cn.kycraft.kuayue.packs.PackLimits;
import cn.kycraft.kuayue.packs.origin.PackOriginIndex;
import com.google.gson.JsonObject;
import lib.kasuga.core.resource.source.ResourceSnapshot;
import lib.kasuga.core.resource.source.ResourceSource;
import lib.kasuga.registration.data_driven.json.DataJson;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemTrainTagsTest {
    @BeforeAll
    static void setPackVersionSource() {
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
    }

    @Test
    void compilesBlocksItemsAndNestedTagsIntoOneTrainTag() throws Exception {
        ResourceLocation stone = ResourceLocation.parse("minecraft:stone");
        PackOriginIndex origin = new PackOriginIndex(
                Map.of(stone, new PackOriginIndex.Product(
                        stone,
                        "kuayue-preset-test",
                        ResourceLocation.parse("kuayue:ss3")
                )),
                Map.of(stone, new PackOriginIndex.Product(
                        stone,
                        "kuayue-preset-test",
                        ResourceLocation.parse("kuayue:ss3")
                ))
        );
        KuaYueTrainDefinition train = new KuaYueTrainDefinition(
                ResourceLocation.parse("kuayue:ss3"),
                "locomotive",
                "train.kuayue.ss3.name",
                "train.kuayue.ss3.description",
                List.of(stone),
                List.of(ResourceLocation.parse("minecraft:stick")),
                List.of(ResourceLocation.parse("kuayue:ss3_parts")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new JsonObject(),
                List.of(),
                Map.of()
        );

        try (ResourceSource source = SystemTrainTags.create(
                KuaYueTrainCatalog.of(List.of(train)),
                origin,
                5,
                PackLimits.defaults(),
                id -> true,
                id -> true
        );
             ResourceSnapshot snapshot = source.capture()) {
            JsonObject items = readJson(
                    snapshot,
                    "data/kuayue/tags/item/system/train/kuayue/ss3.json"
            );
            JsonObject blocks = readJson(
                    snapshot,
                    "data/kuayue/tags/block/system/train/kuayue/ss3.json"
            );

            assertEquals(
                    Set.of(
                            "minecraft:stone",
                            "minecraft:stick",
                            "#kuayue:ss3_parts"
                    ),
                    values(items)
            );
            assertEquals(Set.of("minecraft:stone"), values(blocks));
        }
    }

    private static JsonObject readJson(ResourceSnapshot snapshot, String path)
            throws Exception {
        byte[] bytes = snapshot.files().stream()
                .filter(file -> file.descriptor().logicalPath().equals(path))
                .findFirst()
                .orElseThrow()
                .readBytes();
        return DataJson.GSON.fromJson(
                new String(bytes, StandardCharsets.UTF_8),
                JsonObject.class
        );
    }

    private static Set<String> values(JsonObject tag) {
        return tag.getAsJsonArray("values").asList().stream()
                .map(element -> element.getAsString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
