package cn.kycraft.kuayue.parts.core.factory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lib.kasuga.geometry.Box;
import lib.kasuga.geometry.Shape;
import lib.kasuga.registration.data_driven.shape.ShapeCatalog;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import lib.kasuga.registration.minecraft.block.shape.BlockShapePlan;
import lib.kasuga.registration.minecraft.block.shape.ShapeSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KuaYueShapeBindingRuntimeTest {
    private static final Shape CLOSED = Shape.of(Box.fromPixels(0, 0, 0, 16, 16, 16));
    private static final Shape OPEN = Shape.of(Box.fromPixels(16, 0, 0, 32, 16, 16));
    private static final ShapeCatalog CATALOG = new ShapeCatalog(Map.of(
            "example:closed", new ShapeCatalog.Entry("example:closed", "box", CLOSED),
            "example:open", new ShapeCatalog.Entry("example:open", "box", OPEN)
    ));
    private final List<Block> installed = new ArrayList<>();

    @AfterEach
    void uninstall() {
        installed.forEach(BlockShapeBindings::uninstall);
    }

    @Test
    void partialStateRulesExpandAcrossFactoryOwnedProperties() throws Exception {
        Block block = ShapeBindingTestFixtures.bindingBlock;
        BlockShapePlan plan = plan("""
                {
                  "shape": {"ref": "example:closed"},
                  "collision": {"from": "shape"},
                  "states": [
                    {
                      "when": {"facing": "north", "open": "true"},
                      "shape": {"ref": "example:open"}
                    }
                  ]
                }
                """, block);
        install(block, plan);

        BlockState openActive = block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.OPEN, true)
                .setValue(ShapeBindingTestFixtures.ACTIVE, true);
        BlockState openInactive = openActive.setValue(ShapeBindingTestFixtures.ACTIVE, false);
        VoxelShape openShape = BlockShapeBindings.resolve(
                openActive, ShapeSlot.SHAPE, Shapes::empty);
        VoxelShape inactiveShape = BlockShapeBindings.resolve(
                openInactive, ShapeSlot.SHAPE, Shapes::empty);

        assertEquals(openShape, inactiveShape);
        assertEquals(OPEN.boxes(), openShape.toAabbs().stream()
                .map(box -> Box.fromPixels(
                        box.minX * 16, box.minY * 16, box.minZ * 16,
                        box.maxX * 16, box.maxY * 16, box.maxZ * 16
                ))
                .toList());
        assertEquals(
                openShape,
                BlockShapeBindings.resolve(openActive, ShapeSlot.COLLISION, Shapes::empty)
        );
    }

    @Test
    void coverageClipsOnlyTheAnchorCollisionAndLeavesTheFullSelectionShape() throws Exception {
        Block block = ShapeBindingTestFixtures.bindingBlock;
        BlockShapePlan plan = plan("""
                {
                  "shape": {"ref": "example:open"},
                  "collision": {"from": "shape"}
                }
                """, block);
        install(block, plan, state -> true);
        BlockState state = block.defaultBlockState();

        VoxelShape selection = state.getShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
        VoxelShape collision = state.getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
        VoxelShape fullCollision = BlockShapeBindings.resolveFull(
                state, ShapeSlot.COLLISION, Shapes::empty);
        assertTrue(selection.max(Direction.Axis.X) > 1.0);
        assertTrue(collision.isEmpty());
        assertTrue(fullCollision.max(Direction.Axis.X) > 1.0);
    }

    @Test
    void ambiguousStateRulesAreRejectedBeforeInstallation() throws Exception {
        Block block = ShapeBindingTestFixtures.bindingBlock;
        assertThrows(IllegalArgumentException.class, () -> plan("""
                {
                  "states": [
                    {"when": {"open": "true"}, "shape": {"ref": "example:closed"}},
                    {"when": {"facing": "north"}, "shape": {"ref": "example:open"}}
                  ]
                }
                """, block));
        assertFalse(BlockShapeBindings.isInstalled(block));
    }

    @Test
    void parserDiagnosticsUseShapeBindingsPath() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> plan("""
                        {"states": {}}
                        """, ShapeBindingTestFixtures.bindingBlock)
        );
        assertTrue(
                failure.getMessage().startsWith("params.shape_bindings.states:"),
                failure.getMessage()
        );
    }

    private static BlockShapePlan plan(
            String json,
            Block block
    ) throws Exception {
        Class<?> type = Class.forName(
                "cn.kycraft.kuayue.packs.shapes.KuaYueShapeBindingSpec");
        Method parse = type.getMethod("parse", JsonElement.class, ShapeCatalog.class);
        Object spec = invoke(parse, null, JsonParser.parseString(json), CATALOG);
        Method build = type.getMethod("buildPlan", Block.class);
        return (BlockShapePlan) invoke(build, spec, block);
    }

    private static Object invoke(Method method, Object receiver, Object... arguments)
            throws Exception {
        try {
            return method.invoke(receiver, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw failure;
        }
    }

    private void install(Block block, BlockShapePlan plan) {
        BlockShapeBindings.install(block, plan);
        installed.add(block);
    }

    private void install(
            Block block,
            BlockShapePlan plan,
            java.util.function.Predicate<BlockState> clipCollision
    ) {
        BlockShapeBindings.install(block, plan, clipCollision);
        installed.add(block);
    }
}
