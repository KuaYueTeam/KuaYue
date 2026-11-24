package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

/** Real outrigger fixture registered before FML freezes the block registry. */
@EventBusSubscriber(modid = "kuayue", bus = EventBusSubscriber.Bus.MOD)
public final class JYContraptionOutriggerFixtures {
    public static JYMaintenanceOutriggerBlock outrigger;

    private JYContraptionOutriggerFixtures() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            outrigger = new JYMaintenanceOutriggerBlock(BlockBehaviour.Properties.of(),
                    new MultiCellFootprint(List.of(new BlockPos(0, 1, 0)), List.of()));
            helper.register(ResourceLocation.parse("kuayue:contraption_test_outrigger"), outrigger);
        });
    }
}
