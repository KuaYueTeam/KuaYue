package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.door.TrainDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionDoorTestRig.ANCHOR;
import static org.junit.jupiter.api.Assertions.*;

class JYContraptionDoorInteractionTest {
    @Test
    void assembledSideDoorTogglesBothWaysForEveryFacingAndHingeThroughCreateRouting() throws Exception {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                var rig = new JYContraptionDoorTestRig(false);
                var state = JYContraptionDoorFixtures.sideDoor.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(BlockStateProperties.DOOR_HINGE, hinge);
                rig.addDoor(state);
                var nbt = rig.contraption.getBlocks().get(ANCHOR).nbt();
                rig.level.worldBlocks.put(ANCHOR, Blocks.STONE.defaultBlockState());

                assertTrue(rig.click(ANCHOR));
                assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
                assertTrue(rig.click(ANCHOR));
                assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
                assertEquals(facing, rig.doorState().getValue(BlockStateProperties.HORIZONTAL_FACING));
                assertEquals(hinge, rig.doorState().getValue(BlockStateProperties.DOOR_HINGE));
                assertSame(nbt, rig.contraption.getBlocks().get(ANCHOR).nbt());
                assertEquals(2, rig.entity.blockChanges);
                assertEquals(2, rig.contraption.colliderInvalidations);
                assertEquals(0, rig.level.worldWrites);
                assertTrue(rig.level.worldBlocks.get(ANCHOR).is(Blocks.STONE));
                assertEquals(1005, rig.level.sounds.getFirst().id());
                assertEquals(1011, rig.level.sounds.getLast().id());
                assertNull(rig.level.sounds.getFirst().excludedPlayer(), "The clicking player also receives server sound");
                assertEquals(BlockPos.containing(rig.world(Vec3.atCenterOf(ANCHOR))),
                        rig.level.sounds.getFirst().worldPos());
            }
        }
    }

    @Test
    void assembledEndfaceUsesTheSameAuthoritativeOperation() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.endface.defaultBlockState());
        assertTrue(rig.click(ANCHOR));
        assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
        assertTrue(rig.click(ANCHOR));
        assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
        assertEquals(2, rig.entity.blockChanges);
        assertEquals(0, rig.level.worldWrites);
    }

    @Test
    void companionsForwardOneClickForBothHingesAndAllFacings() throws Exception {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                var rig = new JYContraptionDoorTestRig(false);
                var state = JYContraptionDoorFixtures.sideDoor.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(BlockStateProperties.DOOR_HINGE, hinge);
                rig.addDoor(state);
                BlockPos child = rig.addCompanion(ANCHOR,
                        JYContraptionDoorFixtures.sideDoor.multiCellFootprint().relativeCells(state).getLast());
                assertTrue(rig.click(child));
                assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
                assertEquals(1, rig.entity.blockChanges);
                assertEquals(1, rig.contraption.colliderInvalidations);
                assertEquals(0, rig.level.worldWrites);
                assertEquals(1, rig.level.sounds.size());
            }
        }
    }

    @Test
    void clientAcknowledgesPrimaryAndCompanionClicksWithoutPredictingStateOrSound() throws Exception {
        var rig = new JYContraptionDoorTestRig(true);
        rig.addDoor(JYContraptionDoorFixtures.sideDoor.defaultBlockState());
        BlockPos child = rig.addCompanion(ANCHOR, new BlockPos(0, 1, 0));
        assertTrue(rig.click(ANCHOR));
        assertTrue(rig.click(child));
        assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
        assertEquals(0, rig.entity.blockChanges);
        assertEquals(0, rig.contraption.colliderInvalidations);
        assertEquals(0, rig.level.worldWrites);
        assertTrue(rig.level.sounds.isEmpty());
        assertTrue(rig.level.events.isEmpty());
    }

    @Test
    void staticWorldDoorInteractionStillTogglesTheWorldState() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        var state = JYContraptionDoorFixtures.sideDoor.defaultBlockState();
        rig.level.allowWorldWrites = true;
        rig.level.worldBlocks.put(ANCHOR, state);
        var hit = new BlockHitResult(Vec3.atCenterOf(ANCHOR), Direction.NORTH, ANCHOR, false);
        assertTrue(TrainDoorBlock.doorUse(state, rig.level, ANCHOR, null,
                net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction());
        assertTrue(rig.level.getBlockState(ANCHOR).getValue(BlockStateProperties.OPEN));
        assertEquals(1, rig.level.worldWrites);
        assertEquals(0, rig.entity.blockChanges);
    }

    @Test
    void invalidAndStaleCompanionOwnershipIsRejectedWithoutWritingAnything() throws Exception {
        for (int invalidOffset : new int[]{0, 17, Integer.MIN_VALUE}) {
            var rig = new JYContraptionDoorTestRig(false);
            rig.addDoor(JYContraptionDoorFixtures.sideDoor.defaultBlockState());
            var child = rig.addCompanion(ANCHOR, new BlockPos(0, 1, 0));
            var tag = rig.contraption.getBlocks().get(child).nbt();
            tag.putInt("parent_y", invalidOffset);
            assertFalse(rig.click(child));
            assertEquals(0, rig.entity.blockChanges);
        }
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.sideDoor.defaultBlockState());
        var child = rig.addCompanion(ANCHOR, new BlockPos(0, 1, 0));
        var tag = rig.contraption.getBlocks().get(child).nbt();
        tag.remove("parent_x");
        assertFalse(rig.click(child));
        tag.putInt("parent_x", 0);
        rig.contraption.getInteractors().remove(ANCHOR);
        assertFalse(rig.click(child));
        rig.contraption.getBlocks().remove(ANCHOR);
        assertFalse(rig.click(child));
        assertEquals(0, rig.entity.blockChanges);
        assertEquals(0, rig.level.worldWrites);
    }

    @Test
    void companionsCannotForwardToUndeclaredCellsOrAnotherCompanion() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.sideDoor.defaultBlockState());
        var undeclared = rig.addCompanion(ANCHOR, new BlockPos(-1, 1, 0));
        assertFalse(rig.click(undeclared), "A left-hinged parent does not own the right-hinged footprint");
        var nested = rig.addCompanion(undeclared, new BlockPos(0, 1, 0));
        assertFalse(rig.click(nested), "Companion links must not recurse");
        assertEquals(0, rig.entity.blockChanges);
        assertEquals(0, rig.level.worldWrites);
    }
}
