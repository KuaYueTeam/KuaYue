package cn.kycraft.kuayue.parts.core.factory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** FML fixture used by shape binding and multi-cell state tests. */
@EventBusSubscriber(modid = "kuayue", bus = EventBusSubscriber.Bus.MOD)
public final class ShapeBindingTestFixtures {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static Block bindingBlock;
    public static Block multiCellBlock;

    private ShapeBindingTestFixtures() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            bindingBlock = block();
            multiCellBlock = block();
            helper.register(
                    ResourceLocation.parse("kuayue:shape_binding_test"),
                    bindingBlock
            );
            helper.register(
                    ResourceLocation.parse("kuayue:multi_cell_state_test"),
                    multiCellBlock
            );
        });
    }

    private static Block block() {
        return new Block(BlockBehaviour.Properties.of()) {
            @Override
            protected void createBlockStateDefinition(
                    StateDefinition.Builder<Block, BlockState> builder
            ) {
                builder.add(
                        BlockStateProperties.HORIZONTAL_FACING,
                        BlockStateProperties.OPEN,
                        ACTIVE
                );
            }
        };
    }
}
