package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;

/** Registers real blocks during FML loading rather than constructing blocks after registry freeze. */
@EventBusSubscriber(modid = "kuayue", bus = EventBusSubscriber.Bus.MOD)
public final class JYContraptionDoorFixtures {
    public static JYMaintenanceDoorBlock sideDoor;
    public static JYMaintenanceEndFaceBlock endface;

    private JYContraptionDoorFixtures() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            MultiCellFootprint footprint = new MultiCellFootprint(List.of(), List.of(
                    new MultiCellFootprint.StateRule("hinge", Map.of(
                            "left", List.of(new BlockPos(0, 1, 0), new BlockPos(1, 1, 0),
                                    new BlockPos(0, 1, 1), new BlockPos(1, 1, 1)),
                            "right", List.of(new BlockPos(0, 1, 0), new BlockPos(-1, 1, 0),
                                    new BlockPos(0, 1, 1), new BlockPos(-1, 1, 1))
                    ))));
            sideDoor = new JYMaintenanceDoorBlock(
                    BlockBehaviour.Properties.of().sound(SoundType.METAL), footprint);
            endface = new JYMaintenanceEndFaceBlock(
                    BlockBehaviour.Properties.of().sound(SoundType.METAL));
            helper.register(ResourceLocation.parse("kuayue:contraption_test_side_door"), sideDoor);
            helper.register(ResourceLocation.parse("kuayue:contraption_test_endface"), endface);
        });
    }
}
