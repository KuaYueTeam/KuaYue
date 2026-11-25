package cn.kycraft.kuayue.parts.core.multiblock;

import cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionDoorFixtures;
import cn.kycraft.kuayue.parts.locomotives.jy.JYContraptionOutriggerFixtures;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultiCellContraptionModuleTest {
    @Test
    void companionProviderIsIndependentOfDoorAndOutriggerRegistrations() {
        SimpleRegistry<Block, MovingInteractionBehaviour> registry = SimpleRegistry.create();
        var companion = MultiCellRegistry.MULTI_CELL_COMPANION.getEntry();
        assertNull(registry.get(companion));
        MultiCellContraptionModule.register(registry);
        assertTrue(registry.get(companion) instanceof MultiCellMovingInteraction);
        assertNull(registry.get(JYContraptionDoorFixtures.sideDoor));
        assertNull(registry.get(JYContraptionOutriggerFixtures.outrigger));
    }
}
