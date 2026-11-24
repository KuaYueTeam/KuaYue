package cn.kycraft.kuayue.parts.core.panel.door;

import cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionDoorFixtures;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
        assertNull(registry.get(Blocks.OAK_DOOR), "Vanilla and other train families keep their own registrations");
    }
}
