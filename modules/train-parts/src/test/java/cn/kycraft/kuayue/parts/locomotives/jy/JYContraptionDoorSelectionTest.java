package cn.kycraft.kuayue.parts.locomotives.jy;

import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
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

class JYContraptionDoorSelectionTest {
    @Test
    void upperCompanionCellsAreHitUsingCapturedOwnershipForBothHinges() throws Exception {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            var rig = new JYContraptionDoorTestRig(false);
            rig.addDoor(JYContraptionDoorFixtures.sideDoor.defaultBlockState()
                    .setValue(BlockStateProperties.DOOR_HINGE, hinge));
            BlockPos child = rig.addCompanion(ANCHOR, new BlockPos(0, 1, 0));
            double x = hinge == DoorHingeSide.LEFT ? 0.8 : 0.1;
            BlockHitResult hit = trace(rig, x, 1.4, -1, 3);
            assertNotNull(hit, "A horizontal eye-height ray must hit the visible upper door");
            assertTrue(hit.getBlockPos().equals(child) || hit.getBlockPos().equals(ANCHOR));
            assertEquals(0, rig.level.worldEntityLookups, "Selection must not query unrelated world BEs");
            assertTrue(rig.click(hit.getBlockPos()));
            assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
            assertEquals(1, rig.entity.blockChanges);
        }
    }

    @Test
    void anEndfaceWithoutCompanionsCanBeClickedAboveItsAnchorCell() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.endface.defaultBlockState());
        assertTrue(JYContraptionDoorFixtures.endface.multiCellFootprint().isEmpty());
        BlockHitResult hit = trace(rig, 1.2, 1.4, -1, 3);
        assertNotNull(hit, "The actual door leaf lies above and beside the anchor cell");
        assertEquals(ANCHOR, hit.getBlockPos());
        assertTrue(rig.click(hit.getBlockPos()));
        assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
        assertEquals(0, rig.level.worldWrites);
    }

    @Test
    void aCloserOrdinaryBlockKeepsItsNativeHitAndOccludesTheDoor() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.endface.defaultBlockState());
        BlockPos wall = ANCHOR.offset(1, 1, 0);
        rig.add(wall, Blocks.STONE.defaultBlockState(), null);
        BlockHitResult hit = trace(rig, 1.2, 1.4, -1, 3);
        assertNotNull(hit);
        assertEquals(wall, hit.getBlockPos());
        assertEquals(Direction.NORTH, hit.getDirection());
    }

    @Test
    void anOrdinaryBlockBehindTheDoorDoesNotStealItsInteraction() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.endface.defaultBlockState());
        rig.add(ANCHOR.offset(1, 1, 2), Blocks.STONE.defaultBlockState(), null);
        BlockHitResult hit = trace(rig, 1.2, 1.4, -1, 4);
        assertNotNull(hit);
        assertEquals(ANCHOR, hit.getBlockPos());
    }

    @Test
    void portalHiddenModelsAreNotOfferedAsExtendedInteractionHits() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(JYContraptionDoorFixtures.endface.defaultBlockState());
        rig.contraption.hidden.add(ANCHOR);
        assertNull(trace(rig, 1.2, 1.4, -1, 3));
    }

    private static BlockHitResult trace(
            JYContraptionDoorTestRig rig, double x, double y, double startZ, double endZ
    ) {
        Vec3 anchor = Vec3.atLowerCornerOf(ANCHOR);
        return ContraptionHandlerClient.rayTraceContraption(
                rig.world(anchor.add(x, y, startZ)),
                rig.world(anchor.add(x, y, endZ)), rig.entity);
    }
}
