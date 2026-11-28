package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.EndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.DoubleRotateDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.MeterCarriageEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.door.TrainDoorBlock;
import lib.kasuga.geometry.Box;
import lib.kasuga.geometry.Shape;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import lib.kasuga.registration.minecraft.block.shape.BlockShapePlan;
import lib.kasuga.registration.minecraft.block.shape.BlockStateKey;
import lib.kasuga.registration.minecraft.block.shape.ShapeSelection;
import lib.kasuga.registration.minecraft.block.shape.ShapeSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec2;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Registers real blocks during FML loading rather than constructing blocks after registry freeze. */
@EventBusSubscriber(modid = "kuayue", bus = EventBusSubscriber.Bus.MOD)
public final class JYContraptionDoorFixtures {
    public static JYMaintenanceDoorBlock sideDoor;
    public static JYMaintenanceEndFaceBlock endface;
    public static final Map<String, Block> allDoors = new LinkedHashMap<>();
    public static EndFaceBlock noDoor;

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
            installDoorShapePlan(sideDoor);
            installEndFaceShapePlan(endface);
            helper.register(ResourceLocation.parse("kuayue:contraption_test_side_door"), sideDoor);
            helper.register(ResourceLocation.parse("kuayue:contraption_test_endface"), endface);
            allDoors.put("maintenance_side", sideDoor);
            allDoors.put("maintenance_endface", endface);
            allDoors.put("ordinary", new TrainDoorBlock(doorProperties()));
            allDoors.put("sliding", new TrainDoorBlock.Sliding(doorProperties()));
            allDoors.put("jy290", new JY290DoorBlock(doorProperties(), Vec2.ZERO, new Vec2(1, 2)));
            var models = Couple.create(ResourceLocation.parse("kuayue:block/test_lower"),
                    ResourceLocation.parse("kuayue:block/test_upper"));
            allDoors.put("rendered_rotate", new CustomRenderedDoorBlock(doorProperties(), models,
                    models, RenderShape.ENTITYBLOCK_ANIMATED, false));
            allDoors.put("rendered_slide", new CustomRenderedDoorBlock(doorProperties(), models,
                    models, RenderShape.ENTITYBLOCK_ANIMATED, true));
            allDoors.put("plain_endface", new EndFaceBlock(doorProperties(), TrainPanelProperties.DoorType.SLIDE));
            allDoors.put("rendered_endface", new CustomRenderedEndFaceBlock(doorProperties(),
                    TrainPanelProperties.DoorType.ROTATE, (String) null, null, null));
            allDoors.put("double_rotate", new DoubleRotateDoorBlock(doorProperties(),
                    TrainPanelProperties.DoorType.ROTATE, null, null, null));
            allDoors.put("meter_endface", new MeterCarriageEndFaceBlock(doorProperties(),
                    TrainPanelProperties.DoorType.SLIDE, true));
            allDoors.put("wide", new TrainDoorBlock(doorProperties(), Vec2.ZERO, new Vec2(2, 2)));
            allDoors.put("wide_endface", new EndFaceBlock(doorProperties(), Vec2.ZERO,
                    new Vec2(2, 2), TrainPanelProperties.DoorType.SLIDE));
            for (var entry : allDoors.entrySet()) {
                if (entry.getValue() != sideDoor && entry.getValue() != endface) {
                    helper.register(ResourceLocation.parse("kuayue:contraption_test_" + entry.getKey()), entry.getValue());
                }
            }
            noDoor = new EndFaceBlock(doorProperties(), TrainPanelProperties.DoorType.NO_DOOR);
            helper.register(ResourceLocation.parse("kuayue:contraption_test_no_door"), noDoor);
        });
    }

    private static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.of().sound(SoundType.METAL);
    }

    private static void installDoorShapePlan(JYMaintenanceDoorBlock block) {
        Shape left = Shape.of(
                Box.fromPixels(0.0, 0.0, 15.9, 32.0, 29.0, 17.0)
        );
        Shape right = Shape.of(
                Box.fromPixels(-16.0, 0.0, 15.9, 16.0, 29.0, 17.0)
        );
        BlockShapePlan.Builder plan = BlockShapePlan.builder();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            Shape shape = state.getValue(BlockStateProperties.DOOR_HINGE)
                    == net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT
                    ? right : left;
            plan.state(
                    BlockStateKey.of(state),
                    java.util.Map.of(ShapeSlot.SHAPE, ShapeSelection.geometry(shape))
            );
        }
        BlockShapeBindings.install(block, plan.build());
    }

    private static void installEndFaceShapePlan(JYMaintenanceEndFaceBlock block) {
        Shape shape = Shape.of(
                Box.fromPixels(11.4, 0.0, 14.9, 25.9, 29.0, 30.0)
        );
        BlockShapePlan.Builder plan = BlockShapePlan.builder();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            plan.state(
                    BlockStateKey.of(state),
                    java.util.Map.of(ShapeSlot.SHAPE, ShapeSelection.geometry(shape))
            );
        }
        BlockShapeBindings.install(block, plan.build());
    }
}
