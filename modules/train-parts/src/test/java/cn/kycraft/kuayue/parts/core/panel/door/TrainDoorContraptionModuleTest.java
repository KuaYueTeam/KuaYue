package cn.kycraft.kuayue.parts.core.panel.door;

import cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionDoorFixtures;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrainDoorContraptionModuleTest {
    @Test
    void addingTheProviderClearsNegativeCachesWithoutReplacingDirectRegistrations() {
        SimpleRegistry<Block, MovingInteractionBehaviour> registry = SimpleRegistry.create();
        var door = JYContraptionDoorFixtures.sideDoor;
        var endface = JYContraptionDoorFixtures.endface;
        assertNull(registry.get(door));
        var otherModBehaviour = new MovingInteractionBehaviour() {};
        registry.register(endface, otherModBehaviour);

        TrainDoorContraptionModule.register(registry);

        assertTrue(registry.get(door) instanceof MovingTrainDoorInteraction);
        assertSame(registry.get(door), registry.get(door));
        assertSame(otherModBehaviour, registry.get(endface));
        assertNull(registry.get(Blocks.OAK_DOOR), "Other mods and vanilla retain their registrations");
    }

    @Test
    void movementProviderClearsNegativeCachesWithoutReplacingDirectRegistrations() {
        SimpleRegistry<Block, MovementBehaviour> registry = SimpleRegistry.create();
        var id = ResourceLocation.parse("kuayue:door_25g");
        assertTrue(BuiltInRegistries.BLOCK.containsKey(id));
        var door = BuiltInRegistries.BLOCK.get(id);
        var endface = JYContraptionDoorFixtures.endface;
        assertNull(registry.get(door));
        var otherModBehaviour = new MovementBehaviour() {};
        registry.register(endface, otherModBehaviour);

        TrainDoorContraptionModule.registerMovement(registry);

        assertTrue(registry.get(door) instanceof TrainDoorMovementBehaviour);
        assertSame(registry.get(door), registry.get(door));
        assertSame(otherModBehaviour, registry.get(endface));
        assertNull(registry.get(JYContraptionDoorFixtures.noDoor));
        assertNull(registry.get(JYContraptionDoorFixtures.sideDoor));
        assertNull(registry.get(Blocks.OAK_DOOR));
    }
}
