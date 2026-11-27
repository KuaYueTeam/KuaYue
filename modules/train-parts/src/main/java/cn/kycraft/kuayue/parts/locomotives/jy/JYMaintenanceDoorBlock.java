package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.door.TrainDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.door.OpenableTrainDoor;
import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlock;
import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlockEntity;
import lib.kasuga.create.interaction.ContraptionShapeProvider;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelInput;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** JY maintenance side door with code-defined state, collision and authored playback. */
public final class JYMaintenanceDoorBlock extends TrainPanelProxyBlock
        implements OpenableTrainDoor, ContraptionShapeProvider {
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoorHingeSide> HINGE =
            BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public JYMaintenanceDoorBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false));
    }

    @Override
    public BooleanProperty doorOpenProperty() {
        return OPEN;
    }

    @Override
    public VoxelShape getContraptionShape(BlockState state, BlockGetter localBlocks, BlockPos localPos) {
        return state.getShape(localBlocks, localPos);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder.add(HINGE, OPEN));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(OPEN, false);
    }

    @Override
    public ProxyComposition<TrainPanelProxyBlockEntity, ProxyModelInput, Void, Void, Void>
    createProxyComposition(TrainPanelProxyBlockEntity element) {
        return JYMaintenanceBooleanClipProxy.create(
                element,
                OPEN,
                "door_open",
                "door"
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return TrainDoorBlock.doorUse(
                state,
                level,
                pos,
                player,
                InteractionHand.MAIN_HAND,
                hitResult
        );
    }
}
