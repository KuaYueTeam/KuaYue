package cn.kycraft.kuayue.parts.core.factory;

import cn.kycraft.kuayue.KuaYue;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Opt-in headless acceptance for the migrated meter-gauge preset shapes.
 *
 * <p>This runs after the real client has loaded KuaYue and the Kasuga Create adapter, then uses the
 * Minecraft block-state caches exactly as gameplay and rendering do.
 */
@EventBusSubscriber(modid = KuaYue.MODID, value = Dist.CLIENT)
public final class KuaYueTrainPackShapeHeadlessAcceptance {
    private static final boolean ENABLED =
            Boolean.getBoolean("kuayue.testTrainPackShapes");
    private static final String PACK_ID = "kuayue-preset-meter-gauge";
    private static final Path REPORT = Path.of(
            "debug",
            "kuayue-train-pack-shapes-headless.json"
    );
    private static boolean finished;

    private KuaYueTrainPackShapeHeadlessAcceptance() {
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        if (!ENABLED || finished) {
            return;
        }
        finished = true;

        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> coverage = new ArrayList<>();
        Exception failure = null;
        Minecraft minecraft = Minecraft.getInstance();
        Path reportPath = minecraft.gameDirectory.toPath().resolve(REPORT);
        try {
            verifyPresetLoaded(minecraft.gameDirectory.toPath());
            verifyFreightFloor(coverage);
            verifyCrossCellFan(coverage);
            verifyDoorStateSelection(coverage);
            verifyCrossCellDoorCollision(coverage);
        } catch (Exception exception) {
            failure = exception;
        }

        report.put("status", failure == null ? "PASS" : "FAIL");
        report.put("pack", PACK_ID);
        report.put("headlessBackend", System.getProperty("kasuga.headless"));
        report.put("gameDirectory", minecraft.gameDirectory.getAbsolutePath());
        report.put("coveredBlockStateCases", coverage);
        report.put("failure", failure == null ? "" : failure.toString());
        try {
            Files.createDirectories(reportPath.getParent());
            Files.writeString(
                    reportPath,
                    new GsonBuilder().setPrettyPrinting().create().toJson(report)
            );
            if (failure == null) {
                LogUtils.getLogger().info(
                        "KUA_YUE_TRAIN_PACK_SHAPES_HEADLESS_PASS {}",
                        reportPath
                );
            } else {
                LogUtils.getLogger().error(
                        "KUA_YUE_TRAIN_PACK_SHAPES_HEADLESS_FAIL {}",
                        reportPath,
                        failure
                );
            }
        } catch (Exception reportingFailure) {
            LogUtils.getLogger().error(
                    "KuaYue train-pack shape headless reporting failed",
                    reportingFailure
            );
        } finally {
            if (failure == null) {
                minecraft.stop();
            } else {
                System.exit(1);
            }
        }
    }

    private static void verifyPresetLoaded(Path gameDirectory) throws Exception {
        Path startupReport = gameDirectory.resolve("kuayue/reports/packs/startup.json");
        require(Files.isRegularFile(startupReport), "Missing KuaYue startup report: " + startupReport);
        JsonObject report = JsonParser.parseString(Files.readString(startupReport))
                .getAsJsonObject();
        boolean effective = false;
        for (JsonElement element : report.getAsJsonArray("effective_packs")) {
            JsonObject pack = element.getAsJsonObject();
            if (PACK_ID.equals(pack.get("id").getAsString())) {
                effective = true;
                break;
            }
        }
        require(effective, "Preset was not effective in the headless client: " + PACK_ID);
    }

    private static void verifyFreightFloor(List<Map<String, Object>> coverage) {
        Block block = block("kuayue:31_freight_floor");
        BlockState state = facing(block, Direction.NORTH);
        requireInstalled(block);
        VoxelShape selection = state.getShape(
                EmptyBlockGetter.INSTANCE,
                BlockPos.ZERO,
                CollisionContext.empty()
        );
        VoxelShape collision = state.getCollisionShape(
                EmptyBlockGetter.INSTANCE,
                BlockPos.ZERO,
                CollisionContext.empty()
        );
        AABB expected = new AABB(
                0.0,
                3.0 / 16.0,
                0.0,
                1.0,
                8.0 / 16.0,
                1.0
        );
        requireShape(selection, expected, "31_freight_floor selection");
        requireShape(collision, expected, "31_freight_floor collision");
        coverage.add(caseReport(
                block,
                List.of("facing=north"),
                "exact slab-top selection and collision",
                selection,
                collision
        ));
    }

    private static void verifyCrossCellFan(List<Map<String, Object>> coverage) {
        Block block = block("kuayue:m1_ceiling_fan");
        requireInstalled(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState state = facing(block, facing);
            VoxelShape selection = state.getShape(
                    EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO,
                    CollisionContext.empty()
            );
            VoxelShape collision = state.getCollisionShape(
                    EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO,
                    CollisionContext.empty()
            );
            require(!selection.isEmpty(), "Ceiling fan selection is empty for " + facing);
            require(
                    selection.bounds().maxY > 1.0,
                    "Ceiling fan selection did not expose its neighboring cell for " + facing
            );
            requireAnchorClipped(collision, "Ceiling fan collision for " + facing);
            coverage.add(caseReport(
                    block,
                    List.of("facing=" + facing.getSerializedName()),
                    "cross-cell selection with anchor-clipped collision",
                    selection,
                    collision
            ));
        }
    }

    private static void verifyDoorStateSelection(List<Map<String, Object>> coverage) {
        Block block = block("kuayue:dfh21_door_1");
        requireInstalled(block);
        BlockState closed = door(block, Direction.NORTH, DoorHingeSide.LEFT, false);
        BlockState open = closed.setValue(BlockStateProperties.OPEN, true);
        VoxelShape closedSelection = shape(closed);
        VoxelShape openSelection = shape(open);
        require(
                closedSelection.max(Direction.Axis.Z)
                        < openSelection.max(Direction.Axis.Z),
                "DFH21 door open state did not select the authored wider shape"
        );
        requireShape(
                closedSelection,
                new AABB(
                        0.0,
                        0.0,
                        0.0,
                        16.0 / 16.0,
                        32.0 / 16.0,
                        1.0 / 16.0
                ),
                "DFH21 door closed selection"
        );
        requireShape(
                openSelection,
                new AABB(
                        15.0 / 16.0,
                        0.0,
                        0.0,
                        16.0 / 16.0,
                        32.0 / 16.0,
                        16.0 / 16.0
                ),
                "DFH21 door open selection"
        );
        coverage.add(caseReport(
                block,
                List.of(
                        "facing=north,hinge=left,open=false",
                        "facing=north,hinge=left,open=true"
                ),
                "open-state shape selection",
                openSelection,
                collision(open)
        ));
    }

    private static void verifyCrossCellDoorCollision(List<Map<String, Object>> coverage) {
        Block block = block("kuayue:c31_door");
        requireInstalled(block);
        BlockState closed = facing(block, Direction.NORTH);
        BlockState open = closed.setValue(BlockStateProperties.OPEN, true);
        VoxelShape closedSelection = shape(closed);
        VoxelShape openSelection = shape(open);
        VoxelShape closedCollision = collision(closed);
        VoxelShape openCollision = collision(open);
        require(
                closedSelection.bounds().maxX > 1.0,
                "C31 closed door selection did not expose a cross-cell region"
        );
        require(
                openSelection.bounds().minZ < 0.0,
                "C31 open door selection did not expose the negative-Z cell"
        );
        requireAnchorClipped(closedCollision, "C31 closed door collision");
        requireAnchorClipped(openCollision, "C31 open door collision");
        coverage.add(caseReport(
                block,
                List.of(
                        "facing=north,open=false",
                        "facing=north,open=true"
                ),
                "state-specific cross-cell selection with anchor-clipped collision",
                openSelection,
                openCollision
        ));
    }

    private static Block block(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        require(block != Blocks.AIR, "Migrated preset block was not registered: " + id);
        return block;
    }

    private static void requireInstalled(Block block) {
        require(
                BlockShapeBindings.isInstalled(block),
                "Migrated preset block has no shape binding: "
                        + BuiltInRegistries.BLOCK.getKey(block)
        );
    }

    private static BlockState facing(Block block, Direction facing) {
        return block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }

    private static BlockState door(
            Block block,
            Direction facing,
            DoorHingeSide hinge,
            boolean open
    ) {
        @SuppressWarnings("unchecked")
        EnumProperty<DoorHingeSide> hingeProperty =
                (EnumProperty<DoorHingeSide>) block.getStateDefinition().getProperty("hinge");
        BooleanProperty openProperty =
                (BooleanProperty) block.getStateDefinition().getProperty("open");
        require(hingeProperty != null, "Door block has no hinge property");
        require(openProperty != null, "Door block has no open property");
        return facing(block, facing)
                .setValue(hingeProperty, hinge)
                .setValue(openProperty, open);
    }

    private static VoxelShape shape(BlockState state) {
        return state.getShape(
                EmptyBlockGetter.INSTANCE,
                BlockPos.ZERO,
                CollisionContext.empty()
        );
    }

    private static VoxelShape collision(BlockState state) {
        return state.getCollisionShape(
                EmptyBlockGetter.INSTANCE,
                BlockPos.ZERO,
                CollisionContext.empty()
        );
    }

    private static void requireShape(
            VoxelShape actual,
            AABB expected,
            String label
    ) {
        require(actual.toAabbs().size() == 1, label + " should contain exactly one box");
        AABB box = actual.toAabbs().getFirst();
        require(close(box.minX, expected.minX), label + " minX mismatch: " + box);
        require(close(box.minY, expected.minY), label + " minY mismatch: " + box);
        require(close(box.minZ, expected.minZ), label + " minZ mismatch: " + box);
        require(close(box.maxX, expected.maxX), label + " maxX mismatch: " + box);
        require(close(box.maxY, expected.maxY), label + " maxY mismatch: " + box);
        require(close(box.maxZ, expected.maxZ), label + " maxZ mismatch: " + box);
    }

    private static void requireAnchorClipped(VoxelShape collision, String label) {
        AABB bounds = collision.bounds();
        require(!collision.isEmpty(), label + " is empty");
        require(bounds.minX >= -1.0e-7, label + " minX crosses the anchor");
        require(bounds.minY >= -1.0e-7, label + " minY crosses the anchor");
        require(bounds.minZ >= -1.0e-7, label + " minZ crosses the anchor");
        require(bounds.maxX <= 1.0 + 1.0e-7, label + " maxX crosses the anchor");
        require(bounds.maxY <= 1.0 + 1.0e-7, label + " maxY crosses the anchor");
        require(bounds.maxZ <= 1.0 + 1.0e-7, label + " maxZ crosses the anchor");
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 1.0e-7;
    }

    private static Map<String, Object> caseReport(
            Block block,
            List<String> states,
            String assertion,
            VoxelShape selection,
            VoxelShape collision
    ) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("block", BuiltInRegistries.BLOCK.getKey(block).toString());
        report.put("states", states);
        report.put("assertion", assertion);
        report.put("selection", selection.toString());
        report.put("selectionBounds", selection.bounds().toString());
        report.put("collision", collision.toString());
        report.put("collisionBounds", collision.bounds().toString());
        return report;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
