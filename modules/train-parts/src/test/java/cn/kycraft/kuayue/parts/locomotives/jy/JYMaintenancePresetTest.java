package cn.kycraft.kuayue.parts.locomotives.jy;

import lib.kasuga.rendering.models.mc.proxies.ProxyModelBlockEntity;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import lib.kasuga.registration.minecraft.block.shape.ShapeSlot;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FML checks the actual embedded parcel, rather than registering another set of test blocks. */
class JYMaintenancePresetTest {
    private static final String PARCEL = "/kuayue-packs/kuayue-preset-industrial/parcels/jy_maintenance/";

    @Test
    void embeddedParcelRegistersKuaYueIdsAndProxyHosts() {
        for (String part : List.of("head", "carport", "console", "floor", "floor_ladder", "outrigger",
                "endface", "door", "panel", "tail", "handrail")) {
            ResourceLocation id = id(part);
            var block = BuiltInRegistries.BLOCK.get(id);
            assertNotSame(Blocks.AIR, block, id.toString());
            assertNotSame(Items.AIR, BuiltInRegistries.ITEM.get(id), id.toString());
            assertTrue(block.defaultBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING), part);
            if (block instanceof EntityBlock entityBlock) {
                var entity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                assertTrue(entity instanceof ProxyModelBlockEntity, part);
                assertEquals("kuayue", ((ProxyModelBlockEntity) entity).model().getNamespace(), part);
            }
        }
        assertTrue(BuiltInRegistries.BLOCK.get(id("door")) instanceof JYMaintenanceDoorBlock);
        assertTrue(BuiltInRegistries.BLOCK.get(id("head")) instanceof JYMaintenanceHeadBlock);
        assertTrue(BuiltInRegistries.BLOCK.get(id("outrigger")) instanceof JYMaintenanceOutriggerBlock);
    }

    @Test
    void tailOnlyUsesCompanionsForItsLeftAndRightWings() {
        var tailBlock = BuiltInRegistries.BLOCK.get(id("tail"));
        assertTrue(tailBlock instanceof JYMaintenanceTailBlock);
        var tail = (JYMaintenanceTailBlock) tailBlock;
        BlockState state = tail.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        assertEquals(
                List.of(new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0)),
                tail.multiCellFootprint().relativeCells(state)
        );
    }

    @Test
    void bundledCreateSchematicIsAvailableFromTheParcel() throws Exception {
        String path = PARCEL + "assets/kuayue/schematic/jy_maintenance.nbt";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            var root = NbtIo.readCompressed(
                    stream,
                    NbtAccounter.create(4L << 20)
            );
            assertFalse(root.getList("blocks", Tag.TAG_COMPOUND).isEmpty(), path);

            boolean containsTail = false;
            var palette = root.getList("palette", Tag.TAG_COMPOUND);
            for (int index = 0; index < palette.size(); index++) {
                String name = palette.getCompound(index).getString("Name");
                assertFalse(name.startsWith("jy_maintenance:"), path);
                containsTail |= "kuayue:jy_maintenance_tail".equals(name);
            }
            assertTrue(containsTail, path);
        }
    }

    @Test
    void customJYGeometryComesFromInstalledShapePlans() {
        for (String part : List.of("head", "carport", "console", "floor", "floor_ladder",
                "outrigger", "endface", "door", "panel", "tail", "handrail")) {
            var block = BuiltInRegistries.BLOCK.get(id(part));
            assertTrue(BlockShapeBindings.isInstalled(block), part);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                VoxelShape planned = BlockShapeBindings.resolve(
                        state,
                        ShapeSlot.SHAPE,
                        Shapes::empty
                );
                VoxelShape effective = state.getShape(
                        EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO,
                        CollisionContext.empty()
                );
                assertFalse(effective.isEmpty(), () -> part + " " + state);
                assertEquals(
                        planned.toAabbs(),
                        effective.toAabbs(),
                        () -> part + " " + state
                );
            }
        }

        var doorBlock = BuiltInRegistries.BLOCK.get(id("door"));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                for (boolean open : new boolean[]{false, true}) {
                    BlockState door = doorBlock.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                            .setValue(BlockStateProperties.DOOR_HINGE, hinge)
                            .setValue(BlockStateProperties.OPEN, open);
                    VoxelShape selection = BlockShapeBindings.resolve(
                            door, ShapeSlot.SHAPE, Shapes::empty);
                    VoxelShape fullCollision = BlockShapeBindings.resolveFull(
                            door, ShapeSlot.COLLISION, Shapes::empty);
                    VoxelShape clippedCollision = BlockShapeBindings.resolve(
                            door, ShapeSlot.COLLISION, Shapes::empty);

                    assertEquals(29.0 / 16.0,
                            selection.max(Direction.Axis.Y), 1.0e-7);
                    assertEquals(26.0 / 16.0,
                            fullCollision.max(Direction.Axis.Y), 1.0e-7);
                    assertEquals(1.0,
                            clippedCollision.max(Direction.Axis.Y), 1.0e-7);
                }
            }
        }
    }

    @Test
    void embeddedNativeRecipeDecodesWithTheCurrentCreateSerializer() throws Exception {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        // The accepted parcel's other recipes use the legacy blueprint system, which has not
        // migrated its serializer or crafting materials. This test covers the native Create recipe.
        String path = PARCEL + "data/kuayue/recipe/jy_maintenance_floor_ladder.json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertDoesNotThrow(() -> Recipe.CODEC.parse(
                    registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(), path);
        }
    }

    private static ResourceLocation id(String part) {
        return ResourceLocation.fromNamespaceAndPath("kuayue", "jy_maintenance_" + part);
    }
}
