package cn.kycraft.kuayue.parts.locomotives.hxd3d;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Hxd3dDataDrivenDefinitionTest {
    private static final Path ROOT = Path.of(
            "resources/packs/kuayue-preset-hxd/parcels/hxd3d"
    );
    private static final Path INDEX = ROOT.resolve(
            "data/kuayue/kasuga_lib/data_driven/hxd3d.json"
    );
    private static final Path GROUPS = ROOT.resolve(
            "data/kuayue/locomotives/hxd3d/groups.json"
    );
    private static final Path BLOCKS = ROOT.resolve(
            "data/kuayue/locomotives/hxd3d/blocks.json"
    );
    private static final Map<String, String> EXPECTED_TYPES = Map.ofEntries(
            Map.entry("door_cabin_hxd3d", "custom_rendered_door"),
            Map.entry("floor_hxd3d", "height_slab"),
            Map.entry("head_hxd3d", "full_shape_directional"),
            Map.entry("hxd3d_cowcatcher", "hxd3d_cowcatcher"),
            Map.entry("hxd3d_pantograph", "single_arm_pantograph"),
            Map.entry("hxd3d_carport", "slab_top"),
            Map.entry("hxd3d_carport_center", "slab_top"),
            Map.entry("hxd3d_underbody_equip", "slab"),
            Map.entry("panel_hexie_hxd3d", "train_panel"),
            Map.entry("hxd3d_panel_bottom", "train_panel"),
            Map.entry("panel_logo_num_hxd3d", "train_panel"),
            Map.entry("hxd3d_panel_middle", "train_panel"),
            Map.entry("hxd3d_panel_transit", "train_hinge_panel"),
            Map.entry("hxd3d_panel_transit_2", "train_hinge_panel")
    );

    @Test
    void hxd3dIndexRegistersItsGroupAndEveryBodyBlock() throws IOException {
        JsonObject index = readJson(INDEX);
        assertEquals(
                List.of(
                        "locomotives/hxd3d/groups.json",
                        "locomotives/hxd3d/blocks.json",
                        "bogeys/hxd3d.json"
                ),
                strings(index.getAsJsonArray("on_register"))
        );

        JsonObject groups = readJson(GROUPS);
        JsonObject group = groups.getAsJsonArray("registry_groups")
                .get(0)
                .getAsJsonObject();
        assertEquals("kuayue:hxd3d_panels", group.get("id").getAsString());
        assertEquals(
                "kuayue:train_panel_tab",
                group.getAsJsonObject("item_properties").get("tab").getAsString()
        );
        assertEquals(
                8,
                group.getAsJsonObject("item_properties").get("stacks_to").getAsInt()
        );
        assertEquals(
                "red",
                group.getAsJsonObject("properties").get("map_color").getAsString()
        );

        Map<String, String> actualTypes = readJson(BLOCKS)
                .getAsJsonArray("blocks")
                .asList()
                .stream()
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toMap(
                        block -> block.get("id").getAsString().substring("kuayue:".length()),
                        block -> block.get("type").getAsString(),
                        (left, right) -> {
                            throw new AssertionError("Duplicate HXD3D block");
                        }
                ));
        assertEquals(EXPECTED_TYPES, actualTypes);
    }

    @Test
    void everyHxd3dBodyBlockKeepsItsExistingClientResources() {
        Set<String> missing = EXPECTED_TYPES.keySet().stream()
                .flatMap(id -> List.of(
                        ROOT.resolve("blockstates").resolve(id + ".json"),
                        ROOT.resolve("models/item").resolve(id + ".json")
                ).stream())
                .filter(path -> !Files.isRegularFile(path))
                .map(Path::toString)
                .collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), () -> "Missing HXD3D resources: " + missing);
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), () -> "Missing file " + path);
        return DataJson.GSON.fromJson(Files.readString(path), JsonObject.class);
    }

    private static List<String> strings(com.google.gson.JsonArray array) {
        return array.asList().stream()
                .map(JsonElement::getAsString)
                .toList();
    }
}
