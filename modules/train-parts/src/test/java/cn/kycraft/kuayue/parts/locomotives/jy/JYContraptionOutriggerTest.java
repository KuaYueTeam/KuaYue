package cn.kycraft.kuayue.parts.locomotives.jy;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionDoorTestRig.ANCHOR;
import static org.junit.jupiter.api.Assertions.*;

class JYContraptionOutriggerTest {
    @Test
    void outriggerHasItsOwnAssembledInteraction() {
        var behaviour = MovingInteractionBehaviour.REGISTRY.get(JYContraptionOutriggerFixtures.outrigger);
        assertTrue(behaviour instanceof JYMaintenanceOutriggerMovingInteraction);
        assertFalse(JYContraptionOutriggerFixtures.outrigger.defaultBlockState().hasProperty(BlockStateProperties.OPEN));
    }

    @Test
    void ownModuleRegistersRuntimeOutriggersWithoutInstallingDoorBehaviour() {
        SimpleRegistry<Block, MovingInteractionBehaviour> registry = SimpleRegistry.create();
        assertNull(registry.get(JYContraptionOutriggerFixtures.outrigger));
        JYMaintenanceOutriggerContraptionModule.register(registry);
        assertTrue(registry.get(JYContraptionOutriggerFixtures.outrigger)
                instanceof JYMaintenanceOutriggerMovingInteraction);
        assertNull(registry.get(JYContraptionDoorFixtures.sideDoor));
        var direct = new MovingInteractionBehaviour() {};
        SimpleRegistry<Block, MovingInteractionBehaviour> directRegistry = SimpleRegistry.create();
        directRegistry.register(JYContraptionOutriggerFixtures.outrigger, direct);
        JYMaintenanceOutriggerContraptionModule.register(directRegistry);
        assertSame(direct, directRegistry.get(JYContraptionOutriggerFixtures.outrigger));
    }

    @Test
    void serverTogglesBothWaysForAllFacingsAndHingesWithoutWorldWritesOrDoorEffects() throws Exception {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                var rig = rig(false, state(false).setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(BlockStateProperties.DOOR_HINGE, hinge));
                var nbt = rig.contraption.getBlocks().get(ANCHOR).nbt();
                rig.level.worldBlocks.put(ANCHOR, Blocks.STONE.defaultBlockState());
                assertTrue(rig.click(ANCHOR));
                assertTrue(partState(rig).getValue(JYMaintenanceOutriggerBlock.EXTENDED));
                assertTrue(rig.click(ANCHOR));
                assertFalse(partState(rig).getValue(JYMaintenanceOutriggerBlock.EXTENDED));
                assertEquals(facing, partState(rig).getValue(BlockStateProperties.HORIZONTAL_FACING));
                assertEquals(hinge, partState(rig).getValue(BlockStateProperties.DOOR_HINGE));
                assertSame(nbt, rig.contraption.getBlocks().get(ANCHOR).nbt());
                assertEquals(2, rig.entity.blockChanges);
                assertEquals(0, rig.level.worldWrites);
                assertTrue(rig.level.worldBlocks.get(ANCHOR).is(Blocks.STONE));
                assertEquals(0, rig.contraption.colliderInvalidations, "Collision is the same cube in either state");
                assertTrue(rig.level.sounds.isEmpty());
                assertTrue(rig.level.events.isEmpty());
            }
        }
    }

    @Test
    void clientAcknowledgesPrimaryAndCompanionWithoutPredictingExtended() throws Exception {
        for (boolean extended : new boolean[]{false, true}) {
            var rig = rig(true, state(extended));
            var child = rig.addCompanion(ANCHOR, new BlockPos(0, 1, 0));
            assertTrue(rig.click(ANCHOR));
            assertTrue(rig.click(child));
            assertEquals(extended, partState(rig).getValue(JYMaintenanceOutriggerBlock.EXTENDED));
            assertEquals(0, rig.entity.blockChanges);
            assertEquals(0, rig.level.worldWrites);
        }
    }

    @Test
    void sharedCompanionRoutingForwardsOnlyOneOperationAndRejectsInvalidOwnership() throws Exception {
        var rig = rig(false, state(false));
        var child = rig.addCompanion(ANCHOR, new BlockPos(0, 1, 0));
        assertTrue(rig.click(child));
        assertTrue(partState(rig).getValue(JYMaintenanceOutriggerBlock.EXTENDED));
        assertEquals(1, rig.entity.blockChanges);
        rig.contraption.getBlocks().get(child).nbt().putInt("parent_y", Integer.MIN_VALUE);
        assertFalse(rig.click(child));
        assertEquals(1, rig.entity.blockChanges);
        assertEquals(0, rig.level.worldWrites);
    }

    @Test
    void actualCreateRayHitsThePrimaryOuTriggerInEitherState() throws Exception {
        for (boolean extended : new boolean[]{false, true}) {
            var rig = rig(false, state(extended));
            Vec3 anchor = Vec3.atLowerCornerOf(ANCHOR);
            var hit = ContraptionHandlerClient.rayTraceContraption(
                    rig.world(anchor.add(0.5, 0.5, -1)), rig.world(anchor.add(0.5, 0.5, 2)), rig.entity);
            assertNotNull(hit);
            assertEquals(ANCHOR, hit.getBlockPos());
            assertTrue(rig.click(hit.getBlockPos()));
            assertEquals(!extended, partState(rig).getValue(JYMaintenanceOutriggerBlock.EXTENDED));
        }
    }

    @Test
    void independentBehaviourRejectsMissingBlocksAndDoorStates() throws Exception {
        var rig = rig(false, JYContraptionDoorFixtures.sideDoor.defaultBlockState());
        var behaviour = new JYMaintenanceOutriggerMovingInteraction();
        assertFalse(behaviour.handlePlayerInteraction(null, InteractionHand.MAIN_HAND, ANCHOR, rig.entity));
        assertFalse(behaviour.handlePlayerInteraction(null, InteractionHand.MAIN_HAND, ANCHOR.above(), rig.entity));
        assertFalse(partState(rig).getValue(BlockStateProperties.OPEN));
        assertEquals(0, rig.entity.blockChanges);
        assertEquals(0, rig.level.worldWrites);
    }

    @Test
    void staticWorldInteractionStillUsesExtendedAndServerAuthority() throws Exception {
        for (boolean client : new boolean[]{false, true}) {
            var rig = new JYContraptionDoorTestRig(client);
            var state = state(false);
            rig.level.allowWorldWrites = true;
            rig.level.worldBlocks.put(ANCHOR, state);
            var hit = new BlockHitResult(Vec3.atCenterOf(ANCHOR), Direction.NORTH, ANCHOR, false);
            assertTrue(state.useWithoutItem(rig.level, null, hit).consumesAction());
            assertEquals(!client, rig.level.getBlockState(ANCHOR).getValue(JYMaintenanceOutriggerBlock.EXTENDED));
            assertEquals(client ? 0 : 1, rig.level.worldWrites);
            assertEquals(0, rig.entity.blockChanges);
        }
    }

    private static BlockState state(boolean extended) {
        return JYContraptionOutriggerFixtures.outrigger.defaultBlockState()
                .setValue(JYMaintenanceOutriggerBlock.EXTENDED, extended);
    }

    private static JYContraptionDoorTestRig rig(boolean client, BlockState state) throws Exception {
        var rig = new JYContraptionDoorTestRig(client);
        CompoundTag nbt = new CompoundTag();
        nbt.putString("test_payload", "preserve_outrigger");
        rig.add(ANCHOR, state, nbt);
        return rig;
    }

    private static BlockState partState(JYContraptionDoorTestRig rig) {
        return rig.contraption.getBlocks().get(ANCHOR).state();
    }
}
