package cn.kycraft.kuayue.parts.core.panel;

import cn.kycraft.kuayue.parts.core.panel.company.CompanyRegistry;
import cn.kycraft.kuayue.parts.core.panel.company.CompanyTrainPanel;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EndFaceBlock extends TrainPanelBlock {
    public final TrainPanelProperties.DoorType DOOR_TYPE;

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public EndFaceBlock(Properties pProperties, TrainPanelProperties.DoorType doorType) {
        super(pProperties, new Vec2(0, 0), new Vec2(1, 1));
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.EAST).setValue(OPEN, false));
        this.DOOR_TYPE = doorType;
    }

    public EndFaceBlock(Properties properties, Vec2 beginPos, Vec2 endPos, TrainPanelProperties.DoorType doorType) {
        super(properties, beginPos, endPos);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.EAST).setValue(OPEN, false));
        this.DOOR_TYPE = doorType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(OPEN));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        VoxelShape shape = EndFaceShapes.getEndFaceShape(pState.getValue(FACING).getOpposite(), DOOR_TYPE, pState.getValue(OPEN)).move(0, 1, 0);
        /*
        switch (pState.getValue(FACING)) {
            case EAST -> shape = shape.move(1, 0, 0);
            case WEST -> shape = shape.move(-1, 0, 0);
            case SOUTH -> shape = shape.move(0, 0, 1);
            case NORTH -> shape = shape.move(0, 0, -1);
        }
         */
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext).setValue(OPEN, false);
    }

    public void generateCompanyBlock(Level level, BlockState state, BlockPos pos, boolean isMoving) {
        Direction direction = state.getValue(FACING);
        boolean open = state.hasProperty(BlockStateProperties.OPEN) ?
                state.getValue(BlockStateProperties.OPEN) : false;
        boolean leftHinge = !state.hasProperty(BlockStateProperties.DOOR_HINGE) || state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT;
        BlockUseFunction function = (l, p, parentState, myPos, myState, player, hand, hit) -> {
            if (myPos.equals(p) || myPos.equals(p.above())) return InteractionResult.SUCCESS;
            BlockState state1 = generateCompanyState(direction, leftHinge ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT, open);
            level.setBlock(myPos, state1, 10);
            CompanyTrainPanel.setParentBlock(myPos, level, state1, pos);
            return InteractionResult.SUCCESS;
        };
        walkAllValidPos(level, pos, state, null, null, null, function);
    }

    public BlockState generateCompanyState(Direction direction, DoorHingeSide hingeSide, boolean open) {
        return CompanyRegistry.COMPANY_TRAIN_PANEL.getEntry().defaultBlockState()
                .setValue(CompanyTrainPanel.FACING, direction.getOpposite())
                .setValue(BlockStateProperties.DOOR_HINGE, hingeSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof PanelBlockItem) {
            return InteractionResult.PASS;
        }
        return TrainSmallWindowBlock.windowUse(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof PanelBlockItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        TrainSmallWindowBlock.windowUse(state, level, pos, player, hand, hitResult);
        return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }
}
