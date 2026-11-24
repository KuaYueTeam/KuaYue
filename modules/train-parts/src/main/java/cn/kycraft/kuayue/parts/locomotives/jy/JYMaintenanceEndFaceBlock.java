package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.door.TrainDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.door.OpenableTrainDoor;
import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** JY maintenance end face with a proxy-rendered, code-defined swinging door. */
public final class JYMaintenanceEndFaceBlock extends TrainPanelProxyBlock
        implements OpenableTrainDoor, ContraptionShapeProvider {
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public JYMaintenanceEndFaceBlock(Properties properties) {
        super(properties, MultiCellFootprint.EMPTY);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
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
        super.createBlockStateDefinition(builder.add(OPEN));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(OPEN, false);
    }

    @Override
    public ProxyComposition<TrainPanelProxyBlockEntity, ProxyModelInput, Void, Void, Void>
    createProxyComposition(TrainPanelProxyBlockEntity element) {
        return JYMaintenanceEndFaceProxy.create(element);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return collisionShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return collisionShape(state);
    }

    private static VoxelShape collisionShape(BlockState state) {
        return JYMaintenanceEndFaceShapes.get(
                state.getValue(FACING),
                state.getValue(OPEN)
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
