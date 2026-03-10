package cn.kycraft.kuayue.parts.core.panel.door;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelShapes;
import cn.kycraft.kuayue.parts.core.panel.company.CompanyTrainDoor;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TrainDoorBlock extends TrainPanelBlock {

    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public TrainDoorBlock(Properties pProperties, Vec2 beginPos, Vec2 endPos) {
        super(pProperties, beginPos, endPos);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(HINGE, DoorHingeSide.LEFT)
                        .setValue(OPEN, false)
        );
    }

    public TrainDoorBlock(Properties properties) {
        super(properties, new Vec2(0, 0), new Vec2(1, 2));
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(HINGE, DoorHingeSide.LEFT)
                        .setValue(OPEN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(HINGE, OPEN));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext)
                .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
                .setValue(HINGE, TrainOpenableWindowBlock.getHinge(pContext))
                .setValue(OPEN, false);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return TrainPanelShapes.getDoorShape(pState.getValue(FACING).getOpposite(), pState.getValue(HINGE), pState.getValue(OPEN));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return doorUse(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    public static InteractionResult doorUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        state = state.cycle(OPEN);
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.getBlock() instanceof CompanyTrainDoor) {
            aboveState = aboveState.cycle(OPEN);
            level.setBlock(abovePos, aboveState, 10);
            CompanyTrainDoor.setParentBlock(abovePos, level, aboveState, pos);
            level.levelEvent(player, aboveState.getValue(OPEN) ? getOpenSound(aboveState) : getCloseSound(aboveState), abovePos, 0);
            level.gameEvent(player, isOpen(aboveState) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, abovePos);
        }
        level.setBlock(pos, state, 10);
        level.levelEvent(player, state.getValue(OPEN) ? getOpenSound(state) : getCloseSound(state), pos, 0);
        level.gameEvent(player, isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }


    public static int getCloseSound(BlockState state) {
        SoundType soundType = state.getSoundType();
        return soundType == SoundType.METAL ? 1011 : 1012;
    }

    public static int getOpenSound(BlockState state) {
        SoundType soundType = state.getSoundType();
        return soundType == SoundType.METAL ? 1005 : 1006;
    }

    public static boolean isOpen(BlockState state) {
        return state.getValue(OPEN);
    }
}
