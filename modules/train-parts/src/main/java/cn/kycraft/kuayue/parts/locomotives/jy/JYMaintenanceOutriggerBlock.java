package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlock;
import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlockEntity;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelInput;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** JY maintenance vehicle-specific proxy model host and outrigger interaction. */
public final class JYMaintenanceOutriggerBlock extends TrainPanelProxyBlock {
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoorHingeSide> HINGE =
            BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty EXTENDED =
            BooleanProperty.create("extended");

    public JYMaintenanceOutriggerBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(EXTENDED, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder.add(HINGE, EXTENDED));
    }

    @Override
    public ProxyComposition<TrainPanelProxyBlockEntity, ProxyModelInput, Void, Void, Void>
    createProxyComposition(TrainPanelProxyBlockEntity element) {
        return JYMaintenanceOutriggerProxy.create(element);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Override
    public boolean isLadder(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            LivingEntity entity
    ) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(EXTENDED), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
