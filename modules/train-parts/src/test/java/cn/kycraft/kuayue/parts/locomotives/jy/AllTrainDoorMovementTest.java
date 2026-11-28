package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import cn.kycraft.kuayue.parts.core.panel.company.CompanyRegistry;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorEntity;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import cn.kycraft.kuayue.parts.core.panel.door.TrainDoorMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;

import static cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionDoorTestRig.ANCHOR;
import static org.junit.jupiter.api.Assertions.*;

class AllTrainDoorMovementTest {
    @Test
    void passengerBoardingDoorsUseCreateDoorControlAndCloseWhenMovingOrDisabled() throws Exception {
        for (var entry : PassengerDoorTestData.doors().entrySet()) {
            var rig = new JYContraptionDoorTestRig(false);
            rig.addDoor(entry.getValue().defaultBlockState());
            MovementBehaviour behaviour = MovementBehaviour.REGISTRY.get(entry.getValue());
            assertNotNull(behaviour, entry.getKey());
            assertTrue(behaviour.mustTickWhileDisabled());
            assertFalse(behaviour.disableBlockEntityRendering());
            var control = controller(DoorControl.ALL);
            var context = context(rig, control);
            behaviour.tick(context);
            assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN), entry.getKey());
            behaviour.tick(context);
            assertEquals(1, rig.entity.blockChanges, "Unchanged station state must not replay a state packet");
            assertTrue(rig.click(ANCHOR));
            behaviour.tick(context);
            assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN), "Manual change persists until the control changes");
            control.mode = DoorControl.NONE;
            behaviour.tick(context);
            control.mode = DoorControl.ALL;
            behaviour.tick(context);
            assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
            context.motion = new Vec3(0, 0, .1);
            behaviour.tick(context);
            assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
            context.motion = Vec3.ZERO;
            context.temporaryData = new WeakReference<>(control);
            behaviour.tick(context);
            assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
            context.disabled = true;
            behaviour.tick(context);
            assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
            assertEquals(rig.entity.blockChanges, rig.contraption.colliderInvalidations);
            assertEquals(rig.entity.blockChanges, rig.level.sounds.size());
            assertEquals(0, rig.level.worldWrites);
        }
    }

    @Test
    void initialAssemblyDoesNotOpenUntilFirstMovementAndMissingStationCloses() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(PassengerDoorTestData.block("door_25g").defaultBlockState());
        var behaviour = MovementBehaviour.REGISTRY.get(rig.doorState().getBlock());
        assertNotNull(behaviour);
        var control = controller(DoorControl.ALL);
        var context = context(rig, control);
        context.firstMovement = true;
        behaviour.tick(context);
        assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
        context.firstMovement = false;
        behaviour.tick(context);
        assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
        control.blockEntity.setRemoved();
        behaviour.tick(context);
        assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
    }

    @Test
    void directionalControlUsesTheDoorSideAndContraptionRotation() throws Exception {
        for (Direction side : new Direction[]{Direction.EAST, Direction.WEST}) {
            var rig = new JYContraptionDoorTestRig(false);
            rig.addDoor(PassengerDoorTestData.block("door_25g").defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
            rig.contraption.bounds = new AABB(ANCHOR).move(side == Direction.EAST ? -2 : 2, 0, 0);
            var control = controller(DoorControl.EAST);
            var context = context(rig, control);
            var behaviour = MovementBehaviour.REGISTRY.get(rig.doorState().getBlock());
            assertNotNull(behaviour);
            behaviour.tick(context);
            assertEquals(side == Direction.EAST, rig.doorState().getValue(BlockStateProperties.OPEN));
            control.mode = DoorControl.WEST;
            behaviour.tick(context);
            assertEquals(side == Direction.WEST, rig.doorState().getValue(BlockStateProperties.OPEN));
            context.rotation = v -> new Vec3(-v.z, v.y, v.x);
            control.mode = DoorControl.SOUTH;
            behaviour.tick(context);
            assertEquals(side == Direction.EAST, rig.doorState().getValue(BlockStateProperties.OPEN));
        }
    }

    @Test
    void manualDoorRenderInputsFollowBlockStateWithoutMovementOrClockChanges() throws Exception {
        for (String name : new String[]{"rendered_rotate", "rendered_slide", "rendered_endface", "double_rotate", "meter_endface"}) {
            var rig = new JYContraptionDoorTestRig(true);
            var state = JYContraptionDoorFixtures.allDoors.get(name).defaultBlockState()
                    .setValue(BlockStateProperties.OPEN, true);
            rig.addDoor(state);
            var behaviour = MovementBehaviour.REGISTRY.get(state.getBlock());
            assertNull(behaviour, "Manual fixtures must render without an actor");
            if (name.startsWith("rendered_") && !name.equals("rendered_endface")) {
                var be = new CustomRenderedDoorEntity(ANCHOR, state);
                be.setOpen(false);
                be.animation_controller = .4f;
                rig.contraption.clientBlockEntities.put(ANCHOR, be);
                assertTrue(be.isOpen());
                assertEquals(.4f, be.animation_controller);
                be.setBlockState(state.setValue(BlockStateProperties.OPEN, false));
                assertFalse(be.isOpen());
            } else {
                var be = new CustomRenderedEndFaceEntity(ANCHOR, state);
                be.setOpen(false);
                be.type = name.equals("meter_endface") ? TrainPanelProperties.DoorType.ROTATE : TrainPanelProperties.DoorType.SLIDE;
                be.counter = .4f;
                rig.contraption.clientBlockEntities.put(ANCHOR, be);
                assertTrue(be.isOpen());
                assertEquals(!name.equals("meter_endface"), be.isRotateDoor(), "Door type comes from the block even if its saved cache is stale");
                assertEquals(.4f, be.counter);
                be.setBlockState(state.setValue(BlockStateProperties.OPEN, false));
                assertFalse(be.isOpen());
            }
            assertEquals(0, rig.entity.blockChanges);
            assertEquals(0, rig.level.worldWrites);
            assertTrue(rig.level.sounds.isEmpty());
        }
    }

    @Test
    void legacyDoorCompanionsFollowThePrimaryActorWithoutIndependentControl() throws Exception {
        var rig = new JYContraptionDoorTestRig(false);
        rig.addDoor(PassengerDoorTestData.block("door_25g").defaultBlockState());
        var child = rig.addCompany(new net.minecraft.core.BlockPos(0, 1, 0),
                CompanyRegistry.COMPANY_TRAIN_DOOR.getEntry().defaultBlockState());
        var control = controller(DoorControl.ALL);
        var primaryContext = context(rig, control);
        var primary = MovementBehaviour.REGISTRY.get(rig.doorState().getBlock());
        var follower = MovementBehaviour.REGISTRY.get(rig.contraption.getBlocks().get(child).state().getBlock());
        assertNotNull(primary);
        assertNull(follower, "Companions do not have their own movement behaviour");
        primary.tick(primaryContext);
        assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
        assertTrue(rig.contraption.getBlocks().get(child).state().getValue(BlockStateProperties.OPEN));
        assertEquals(2, rig.entity.blockChanges);
        assertEquals(1, rig.contraption.colliderInvalidations);
        assertEquals(1, rig.level.sounds.size());
    }

    @Test
    void excludedDoorsIgnoreStaleMovementCallbacksAndRemainManuallyOpenable() throws Exception {
        var staleBehaviour = new TrainDoorMovementBehaviour();
        for (var door : JYContraptionDoorFixtures.allDoors.values()) {
            var rig = new JYContraptionDoorTestRig(false);
            rig.addDoor(door.defaultBlockState());
            staleBehaviour.tick(context(rig, controller(DoorControl.ALL)));
            assertFalse(rig.doorState().getValue(BlockStateProperties.OPEN));
            assertEquals(0, rig.entity.blockChanges);
            assertTrue(rig.level.sounds.isEmpty());
            assertTrue(rig.click(ANCHOR));
            staleBehaviour.tick(context(rig, controller(DoorControl.NONE)));
            assertTrue(rig.doorState().getValue(BlockStateProperties.OPEN));
            assertEquals(1, rig.entity.blockChanges);
        }
    }

    private static DoorControlBehaviour controller(DoorControl mode) {
        var be = new CustomRenderedEndFaceEntity(ANCHOR,
                JYContraptionDoorFixtures.allDoors.get("rendered_endface").defaultBlockState());
        var control = new DoorControlBehaviour(be);
        control.mode = mode;
        return control;
    }

    private static MovementContext context(JYContraptionDoorTestRig rig, DoorControlBehaviour control) {
        var context = new MovementContext(rig.level, rig.contraption.getBlocks().get(ANCHOR), rig.contraption);
        context.firstMovement = false;
        context.position = rig.world(Vec3.atCenterOf(ANCHOR));
        context.temporaryData = new WeakReference<>(control);
        return context;
    }
}
