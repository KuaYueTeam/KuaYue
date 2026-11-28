package cn.kycraft.kuayue.parts.locomotives.jy;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import cn.kycraft.kuayue.parts.core.panel.company.CompanyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import cn.kycraft.kuayue.parts.core.panel.door.OpenableTrainDoor;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

import static cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionDoorTestRig.ANCHOR;
import static org.junit.jupiter.api.Assertions.*;

class AllTrainDoorContraptionTest {
    @Test
    void allDoorFamiliesHaveInteractionButOnlyPassengerBoardingDoorsHaveMovement() {
        assertEquals(13, JYContraptionDoorFixtures.allDoors.size());
        for (var entry : JYContraptionDoorFixtures.allDoors.entrySet()) {
            assertFalse(entry.getValue().defaultBlockState().getValue(BlockStateProperties.OPEN), entry.getKey());
            assertNotNull(MovingInteractionBehaviour.REGISTRY.get(entry.getValue()), entry.getKey());
            assertNull(MovementBehaviour.REGISTRY.get(entry.getValue()), "Generic door fixtures are not passenger boarding doors: " + entry.getKey());
        }
        for (var block : PassengerDoorTestData.doors().values()) {
            assertNotNull(MovingInteractionBehaviour.REGISTRY.get(block));
            assertNotNull(MovementBehaviour.REGISTRY.get(block));
        }
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("kuayue") || !OpenableTrainDoor.isDoor(block)) {
                continue;
            }
            assertNotNull(MovingInteractionBehaviour.REGISTRY.get(block), id.toString());
            assertEquals(PassengerDoorTestData.IDS.contains(id.getPath()), MovementBehaviour.REGISTRY.get(block) != null, id.toString());
        }
        for (var block : new net.minecraft.world.level.block.Block[]{
                JYContraptionDoorFixtures.noDoor, Blocks.STONE}) {
            assertNull(MovingInteractionBehaviour.REGISTRY.get(block));
            assertNull(MovementBehaviour.REGISTRY.get(block));
        }
    }

    @Test
    void everyDoorFamilyTogglesThroughCreateWithoutWritingTheStaticWorld() throws Exception {
        for (var entry : JYContraptionDoorFixtures.allDoors.entrySet()) {
            var rig = new JYContraptionDoorTestRig(false);
            rig.addDoor(entry.getValue().defaultBlockState());
            assertTrue(rig.click(ANCHOR), entry.getKey());
            assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN), entry.getKey());
            assertTrue(rig.click(ANCHOR), entry.getKey());
            assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN), entry.getKey());
            assertEquals(2, rig.entity.blockChanges);
            assertEquals(2, rig.contraption.colliderInvalidations);
            assertEquals(2, rig.level.sounds.size());
            assertEquals(0, rig.level.worldWrites);
        }
    }

    @Test
    void legacyUpperDoorsForwardToTheirParentAndSynchronizeTheirOwnState() throws Exception {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                var rig = new JYContraptionDoorTestRig(false);
                rig.addDoor(JYContraptionDoorFixtures.allDoors.get("ordinary").defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(BlockStateProperties.DOOR_HINGE, hinge));
                var child = rig.addCompany(new BlockPos(0, 1, 0),
                        CompanyRegistry.COMPANY_TRAIN_DOOR.getEntry().defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                                .setValue(BlockStateProperties.DOOR_HINGE, hinge));
                var nbt = rig.contraption.getBlocks().get(child).nbt();
                assertTrue(rig.click(child));
                assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
                assertTrue(rig.contraption.getBlocks().get(child).state().getValue(BlockStateProperties.OPEN));
                assertSame(nbt, rig.contraption.getBlocks().get(child).nbt());
                assertEquals(2, rig.entity.blockChanges);
                assertEquals(1, rig.contraption.colliderInvalidations);
                assertEquals(1, rig.level.sounds.size());
                assertTrue(rig.click(ANCHOR));
                assertFalse(rig.contraption.getBlocks().get(child).state().getValue(BlockStateProperties.OPEN));
                assertEquals(0, rig.level.worldWrites);
                assertEquals(0, rig.level.worldEntityLookups);
            }
        }
    }

    @Test
    void malformedAndUnownedLegacyCompanionsCannotToggleDoors() throws Exception {
        for (int offset : new int[]{0, 17, Integer.MIN_VALUE}) {
            var rig = new JYContraptionDoorTestRig(false);
            rig.addDoor(JYContraptionDoorFixtures.allDoors.get("ordinary").defaultBlockState());
            var child = rig.addCompany(new BlockPos(0, 1, 0),
                    CompanyRegistry.COMPANY_TRAIN_DOOR.getEntry().defaultBlockState());
            rig.contraption.getBlocks().get(child).nbt().putInt("py", offset);
            assertFalse(rig.click(child));
            assertEquals(0, rig.entity.blockChanges);
        }
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.allDoors.get("ordinary").defaultBlockState());
        var child = rig.addCompany(new BlockPos(1, 1, 0),
                CompanyRegistry.COMPANY_TRAIN_DOOR.getEntry().defaultBlockState());
        assertFalse(rig.click(child));
        rig.contraption.getBlocks().get(child).nbt().remove("px");
        assertFalse(rig.click(child));
        assertEquals(0, rig.entity.blockChanges);
    }

    @Test
    void capturedLegacyOffsetsRotateWithWideDoorsAndPreserveHingeOwnership() throws Exception {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                var rig = new JYContraptionDoorTestRig(false);
                rig.addDoor(JYContraptionDoorFixtures.allDoors.get("wide").defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(BlockStateProperties.DOOR_HINGE, hinge));
                BlockPos original = new BlockPos(hinge == DoorHingeSide.LEFT ? -1 : 1, 1, 0);
                Rotation rotation = switch (facing) {
                    case EAST -> Rotation.CLOCKWISE_90;
                    case SOUTH -> Rotation.CLOCKWISE_180;
                    case WEST -> Rotation.COUNTERCLOCKWISE_90;
                    default -> Rotation.NONE;
                };
                var child = rig.addCompany(original.rotate(rotation),
                        CompanyRegistry.COMPANY_SLIDING_DOOR.getEntry().defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
                var tag = rig.contraption.getBlocks().get(child).nbt();
                // StructureTransform rotates block positions/states, while the saved BE offset retains its old frame.
                tag.putInt("px", -original.getX());
                tag.putInt("py", -original.getY());
                tag.putInt("pz", -original.getZ());
                tag.putString("direction", Direction.NORTH.name());
                assertTrue(rig.click(child));
                assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
                assertTrue(rig.contraption.getBlocks().get(child).state().getValue(BlockStateProperties.OPEN));
                assertEquals(2, rig.entity.blockChanges);
                assertEquals(1, rig.contraption.colliderInvalidations);
            }
        }
    }

    @Test
    void endfaceCompanyPanelsForwardAndCellsBelongingToOtherParentsStayUnchanged() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.allDoors.get("wide_endface").defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var child = rig.addCompany(new BlockPos(-1, 1, 0),
                CompanyRegistry.COMPANY_TRAIN_PANEL.getEntry().defaultBlockState());
        var foreign = rig.addCompany(new BlockPos(-1, 0, 0),
                CompanyRegistry.COMPANY_TRAIN_DOOR.getEntry().defaultBlockState());
        rig.contraption.getBlocks().get(foreign).nbt().putInt("px", 0);
        rig.contraption.getBlocks().get(foreign).nbt().putInt("py", -1);
        assertTrue(rig.click(child));
        assertTrue(rig.contraption.getBlocks().get(child).state().getValue(BlockStateProperties.OPEN));
        assertFalse(rig.contraption.getBlocks().get(foreign).state().getValue(BlockStateProperties.OPEN));
        assertEquals(2, rig.entity.blockChanges);
        assertEquals(0, rig.level.worldEntityLookups);
    }

    @Test
    void clientsAcknowledgeAllFamiliesAndLegacyChildrenWithoutPrediction() throws Exception {
        for (var block : JYContraptionDoorFixtures.allDoors.values()) {
            var rig = new JYContraptionDoorTestRig(true);
            rig.addDoor(block.defaultBlockState());
            assertTrue(rig.click(ANCHOR));
            assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
            assertEquals(0, rig.entity.blockChanges);
            assertTrue(rig.level.sounds.isEmpty());
        }
        var rig = new JYContraptionDoorTestRig(true);
        rig.addDoor(JYContraptionDoorFixtures.allDoors.get("ordinary").defaultBlockState());
        var child = rig.addCompany(new BlockPos(0, 1, 0),
                CompanyRegistry.COMPANY_SLIDING_DOOR.getEntry().defaultBlockState());
        assertTrue(rig.click(child));
        assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
        assertEquals(0, rig.entity.blockChanges);
        assertTrue(rig.level.sounds.isEmpty());
    }
}
