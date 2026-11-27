package cn.kycraft.kuayue.parts.core.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import lib.kasuga.registration.minecraft.block.shape.ShapeSlot;
import lib.kasuga.registration.data_driven.json.DataJson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterGaugeShapeMigrationTest {
    private static final Path PACK = Path.of(
            "resources/packs/kuayue-preset-meter-gauge"
    );
    private static final String LEGACY_AABB_RESOURCE =
            "/kuayue/shapes/meter-gauge-legacy-aabb.json";
    private static final List<Parcel> PARCELS = List.of(
            new Parcel(
                    "m1",
                    "data/kuayue/carriages/m1/blocks.json",
                    "data/kuayue/carriages/m1/shapes.json",
                    "carriages/m1/shapes.json"
            ),
            new Parcel(
                    "dfh21",
                    "data/kuayue/locomotives/dfh21/blocks.json",
                    "data/kuayue/locomotives/dfh21/shapes.json",
                    "locomotives/dfh21/shapes.json"
            ),
            new Parcel(
                    "gc220ii",
                    "data/kuayue/locomotives/gc220ii/blocks.json",
                    "data/kuayue/locomotives/gc220ii/shapes.json",
                    "locomotives/gc220ii/shapes.json"
            ),
            new Parcel(
                    "meter_freight",
                    "data/kuayue/carriages/meter_freight/blocks.json",
                    "data/kuayue/carriages/meter_freight/shapes.json",
                    "carriages/meter_freight/shapes.json"
            )
    );
    private static final List<String> KEY_ORDER =
            List.of("facing", "hinge", "open", "lit");

    @Test
    void everyLegacyAabbStateMapsToTheSameCompiledBoxAndSourceId() throws IOException {
        JsonObject legacyBlocks = legacyAabbs();
        int blockCount = 0;
        int stateCount = 0;
        for (Parcel parcel : PARCELS) {
            Path parcelRoot = PACK.resolve("parcels").resolve(parcel.id());
            JsonObject blocks = read(parcelRoot.resolve(parcel.blocksPath()));
            JsonObject shapeFile = read(parcelRoot.resolve(parcel.shapesPath()));
            Map<String, JsonArray> definitions = definitions(shapeFile.getAsJsonArray("shapes"));
            for (JsonElement element : blocks.getAsJsonArray("blocks")) {
                JsonObject block = element.getAsJsonObject();
                JsonObject params = block.getAsJsonObject("params");
                String blockId = block.get("id").getAsString();
                assertTrue(
                        params == null || !params.has("aabb"),
                        () -> "Migrated block still declares params.aabb: " + blockId
                );
                assertTrue(
                        params == null || !params.has("shapes"),
                        () -> "Migrated block still declares legacy params.shapes: " + blockId
                );
                JsonObject legacy = legacyBlocks.getAsJsonObject(blockId);
                if (legacy == null) continue;
                blockCount++;
                assertEquals(parcel.id(), legacy.get("parcel").getAsString());
                JsonObject aabb = legacy.getAsJsonObject("aabb");
                stateCount += aabb.size();
                JsonObject bindings = params.getAsJsonObject("shape_bindings");
                assertTrue(
                        bindings != null,
                        () -> "Missing params.shape_bindings for " + block.get("id")
                );
                assertEquals(
                        "shape",
                        bindings.getAsJsonObject("collision").get("from").getAsString()
                );

                Map<String, JsonObject> rules = new HashMap<>();
                for (JsonElement ruleValue : bindings.getAsJsonArray("states")) {
                    JsonObject rule = ruleValue.getAsJsonObject();
                    String key = serialise(rule.getAsJsonObject("when"));
                    assertTrue(rules.put(key, rule) == null, () -> "Duplicate rule " + key);
                }
                for (Map.Entry<String, JsonElement> entry : aabb.entrySet()) {
                    String key = normalise(entry.getKey());
                    JsonObject rule = rules.get(key);
                    assertTrue(key.isEmpty() || rule != null, () -> "Missing binding " + key);
                    JsonObject shapeBinding = key.isEmpty()
                            ? bindings.getAsJsonObject("shape")
                            : rule.getAsJsonObject("shape");
                    assertTrue(shapeBinding != null, () -> "Missing shape selection " + key);
                    String shapeId = shapeBinding.get("ref").getAsString();
                    assertTrue(
                            shapeId.startsWith("kuayue:" + parcel.id() + "/shapes/"),
                            () -> "Shape source escaped parcel: " + shapeId
                    );
                    JsonArray expected = entry.getValue().getAsJsonArray();
                    JsonArray actual = definitions.get(shapeId);
                    assertTrue(actual != null, () -> "Missing shape definition " + shapeId);
                    if (expected.isEmpty()) {
                        assertTrue(actual.isEmpty(), shapeId);
                    } else {
                        assertEquals(expected, actual, shapeId);
                    }
                }
                assertEquals(
                        aabb.size(),
                        bindings.has("shape") ? 1 : rules.size(),
                        () -> "Binding coverage mismatch for " + block.get("id")
                );
            }
        }
        assertEquals(89, blockCount);
        assertEquals(629, stateCount);
        assertEquals(legacyBlocks.size(), blockCount);
    }

    @Test
    void blockstateVariantsAndCrossCellRulesUseOnlyTheirExactState() throws IOException {
        JsonObject legacyBlocks = legacyAabbs();
        for (Parcel parcel : PARCELS) {
            Path parcelRoot = PACK.resolve("parcels").resolve(parcel.id());
            JsonObject blocks = read(parcelRoot.resolve(parcel.blocksPath()));
            for (JsonElement element : blocks.getAsJsonArray("blocks")) {
                JsonObject block = element.getAsJsonObject();
                JsonObject params = block.getAsJsonObject("params");
                String blockId = block.get("id").getAsString();
                JsonObject legacy = legacyBlocks.getAsJsonObject(blockId);
                if (legacy == null) continue;
                JsonObject aabb = legacy.getAsJsonObject("aabb");
                String path = blockId.split(":", 2)[1];
                JsonObject blockstate = read(parcelRoot.resolve("blockstates").resolve(path + ".json"));
                Set<String> variants = new HashSet<>();
                for (String key : blockstate.getAsJsonObject("variants").keySet()) {
                    variants.add(normalise(key));
                }
                Set<String> legacyStates = new HashSet<>();
                aabb.keySet().forEach(key -> legacyStates.add(normalise(key)));
                assertEquals(variants, legacyStates, () -> "State source mismatch for " + blockId);

                JsonObject multiCell = params.getAsJsonObject("multi_cell");
                Map<String, JsonArray> coverage = new HashMap<>();
                if (multiCell != null) {
                    for (JsonElement rule : multiCell.getAsJsonArray("state_cells")) {
                        JsonObject object = rule.getAsJsonObject();
                        coverage.put(
                                serialise(object.getAsJsonObject("when")),
                                object.getAsJsonArray("cells")
                        );
                    }
                }
                for (Map.Entry<String, JsonElement> entry
                        : aabb.entrySet()) {
                    String key = normalise(entry.getKey());
                    Set<BlockPos> expected = occupiedCells(entry.getValue().getAsJsonArray(), key);
                    JsonArray actual = coverage.get(key);
                    if (expected.isEmpty()) {
                        assertTrue(actual == null, () -> "Unexpected coverage for " + blockId);
                    } else {
                        assertTrue(actual != null, () -> "Missing coverage for " + blockId);
                        Set<BlockPos> actualCells = offsets(actual);
                        assertEquals(expected, actualCells, () -> "Coverage mismatch for " + blockId);
                    }
                }
            }
        }
    }

    @Test
    void parcelIndexesRegisterShapeDefinitionsBeforeBlocks() throws IOException {
        for (Parcel parcel : PARCELS) {
            Path parcelRoot = PACK.resolve("parcels").resolve(parcel.id());
            Path index = Files.walk(parcelRoot.resolve("data/kuayue/kasuga_lib/data_driven"))
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .orElseThrow();
            JsonArray entries = read(index).getAsJsonArray("on_register");
            List<String> values = entries.asList().stream().map(JsonElement::getAsString).toList();
            assertEquals(parcel.indexEntry(), values.getFirst());
            String blockEntry = parcel.blocksPath().substring("data/kuayue/".length());
            assertTrue(values.indexOf(parcel.indexEntry()) < values.indexOf(blockEntry));
        }
    }

    @Test
    void productionFactoriesInstallExactSelectionAndCellClippedCollisionPlans() {
        BlockState floor = state("kuayue:31_freight_floor", Direction.EAST);
        AABB expectedFloor = new AABB(0.0, 3.0 / 16.0, 0.0, 1.0, 8.0 / 16.0, 1.0);
        assertEquals(
                List.of(expectedFloor),
                BlockShapeBindings.resolve(floor, ShapeSlot.SHAPE, Shapes::empty).toAabbs()
        );
        assertEquals(
                List.of(expectedFloor),
                BlockShapeBindings.resolve(floor, ShapeSlot.COLLISION, Shapes::empty).toAabbs()
        );

        BlockState fan = state("kuayue:m1_ceiling_fan", Direction.NORTH);
        VoxelShape full = BlockShapeBindings.resolve(fan, ShapeSlot.SHAPE, Shapes::empty);
        VoxelShape anchorCollision =
                BlockShapeBindings.resolve(fan, ShapeSlot.COLLISION, Shapes::empty);
        assertTrue(full.max(Direction.Axis.Y) > 1.0);
        assertEquals(1.0, anchorCollision.max(Direction.Axis.Y), 1.0e-7);
    }

    private static Map<String, JsonArray> definitions(JsonArray definitions) {
        Map<String, JsonArray> result = new LinkedHashMap<>();
        for (JsonElement value : definitions) {
            JsonObject definition = value.getAsJsonObject();
            String id = definition.get("id").getAsString();
            JsonArray box = definition.getAsJsonObject("nodes")
                    .get(definition.get("output").getAsString())
                    .getAsJsonArray();
            assertTrue(result.put(id, box) == null, () -> "Duplicate shape id " + id);
        }
        return result;
    }

    private static Set<BlockPos> occupiedCells(JsonArray box, String key) {
        if (box.isEmpty()) return Set.of();
        BigDecimal[] values = new BigDecimal[6];
        for (int index = 0; index < values.length; index++) {
            values[index] = box.get(index).getAsBigDecimal()
                    .divide(BigDecimal.valueOf(16), 20, RoundingMode.HALF_UP);
        }
        int minX = values[0].setScale(0, RoundingMode.FLOOR).intValueExact();
        int minY = values[1].setScale(0, RoundingMode.FLOOR).intValueExact();
        int minZ = values[2].setScale(0, RoundingMode.FLOOR).intValueExact();
        int maxX = values[3].setScale(0, RoundingMode.CEILING).intValueExact();
        int maxY = values[4].setScale(0, RoundingMode.CEILING).intValueExact();
        int maxZ = values[5].setScale(0, RoundingMode.CEILING).intValueExact();
        String facing = properties(key).getOrDefault("facing", "north");
        Set<BlockPos> result = new HashSet<>();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos canonical = inverseRotate(new BlockPos(x, y, z), facing);
                    if (!BlockPos.ZERO.equals(canonical)) result.add(canonical);
                }
            }
        }
        return result;
    }

    private static BlockPos inverseRotate(BlockPos pos, String facing) {
        return switch (facing) {
            case "east" -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case "south" -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case "west" -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            default -> pos;
        };
    }

    private static Set<BlockPos> offsets(JsonArray cells) {
        Set<BlockPos> result = new HashSet<>();
        for (JsonElement value : cells) {
            JsonArray tuple = value.getAsJsonArray();
            BlockPos offset = new BlockPos(
                    tuple.get(0).getAsInt(),
                    tuple.get(1).getAsInt(),
                    tuple.get(2).getAsInt()
            );
            assertFalse(BlockPos.ZERO.equals(offset), "state_cells must exclude the anchor");
            assertTrue(result.add(offset), () -> "Duplicate cell " + offset);
        }
        return result;
    }

    private static String normalise(String key) {
        if (key.isBlank()) return "";
        Map<String, String> values = properties(key);
        return serialise(values);
    }

    private static Map<String, String> properties(String key) {
        Map<String, String> values = new HashMap<>();
        if (!key.isBlank()) {
            for (String part : key.split(",")) {
                String[] pair = part.trim().split("=", 2);
                values.put(pair[0], pair[1]);
            }
        }
        return values;
    }

    private static String serialise(JsonObject object) {
        Map<String, String> values = new HashMap<>();
        object.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsString()));
        return serialise(values);
    }

    private static String serialise(Map<String, String> values) {
        List<String> parts = new ArrayList<>();
        for (String property : KEY_ORDER) {
            if (values.containsKey(property)) {
                parts.add(property + "=" + values.get(property));
            }
        }
        return String.join(",", parts);
    }

    private static JsonObject read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), () -> "Missing " + path);
        return DataJson.GSON.fromJson(Files.readString(path), JsonObject.class);
    }

    private static JsonObject legacyAabbs() throws IOException {
        try (InputStream stream = MeterGaugeShapeMigrationTest.class
                .getResourceAsStream(LEGACY_AABB_RESOURCE)) {
            assertTrue(stream != null, () -> "Missing test resource " + LEGACY_AABB_RESOURCE);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return DataJson.GSON.fromJson(json, JsonObject.class).getAsJsonObject("blocks");
        }
    }

    private static BlockState state(String id, Direction facing) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        assertTrue(BlockShapeBindings.isInstalled(block), () -> "No shape plan for " + id);
        BlockState state = block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
        return state;
    }

    private record Parcel(
            String id,
            String blocksPath,
            String shapesPath,
            String indexEntry
    ) {
    }
}
