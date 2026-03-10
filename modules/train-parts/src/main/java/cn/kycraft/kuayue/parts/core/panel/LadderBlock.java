package cn.kycraft.kuayue.parts.core.panel;

import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LadderBlock extends SlabBlock{

    private boolean isMeter = false;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public LadderBlock(Properties pProperties) {
        super(pProperties, false);
        registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.EAST)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
        );
    }

    public LadderBlock(Properties properties, boolean isMeter) {
        super(properties, false);
        registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.EAST)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
        );
        this.isMeter = isMeter;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(HINGE, OPEN));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if(!isMeter)
            return TrainPanelShapes.getLadderShape(pState);
        return TrainPanelShapes.getMeterLadderShape(pState);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext)
                .setValue(HINGE, TrainOpenableWindowBlock.getHinge(pContext))
                .setValue(FACING, pContext.getHorizontalDirection())
                .setValue(OPEN, false);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return TrainSmallWindowBlock.windowUse(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(stack.getItem() instanceof SlabBlockItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = TrainSmallWindowBlock.windowUse(state, level, pos, player, hand, hitResult);
        return result.equals(InteractionResult.sidedSuccess(level.isClientSide)) ?
                ItemInteractionResult.sidedSuccess(level.isClientSide) :
                ItemInteractionResult.FAIL;
    }
}
