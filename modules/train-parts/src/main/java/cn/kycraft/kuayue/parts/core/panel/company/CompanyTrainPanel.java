package cn.kycraft.kuayue.parts.core.panel.company;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import cn.kycraft.kuayue.parts.core.panel.base.TrainPanelShapes;
import cn.kycraft.kuayue.utils.DirectionUtil;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CompanyTrainPanel extends BaseEntityBlock implements IWrenchable {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<TrainPanelProperties.ShapeType> SHAPE_TYPE =
            EnumProperty.create("shape_type", TrainPanelProperties.ShapeType.class);

    protected CompanyTrainPanel(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(HINGE, DoorHingeSide.LEFT)
                        .setValue(OPEN, false)
                        .setValue(SHAPE_TYPE, TrainPanelProperties.ShapeType.NORMAL)
        );
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        TrainPanelProperties.ShapeType shapeType = pState.getValue(SHAPE_TYPE);
        switch (shapeType) {
            case BIG_TRUSS:
                return TrainPanelShapes.getOverheadBigTrussShape(pState.getValue(FACING));
            case SMALL_TRUSS:
                return TrainPanelShapes.getOverheadSmallTrussShape(pState.getValue(FACING));
            case PILLAR_TRUSS:
                return TrainPanelShapes.getOverheadPillarTrussShape(pState.getValue(FACING));
            default:
                return TrainPanelShapes.getShape(pState.getValue(FACING).getOpposite());
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        BlockPos pos = getParentPos(pLevel, pPos);
        if (pos == null) {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            return;
        }
        BlockState state = pLevel.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof TrainPanelBlock trainPanelBlock && !pState.getBlock().equals(pNewState.getBlock()))
            trainPanelBlock.specialRemove(state, pLevel, pos, pNewState, pIsMoving);
        else
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, HINGE, OPEN, SHAPE_TYPE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext)
                .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
                .setValue(SHAPE_TYPE, TrainPanelProperties.ShapeType.NORMAL);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (pIsMoving) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof CompanyTrainBlockEntity entity) {
                entity.updateDirection(pState.getValue(FACING));
            }
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos parentPos = getParentPos(level, pos);
        if (parentPos == null) return InteractionResult.PASS;
        BlockState parentState = level.getBlockState(parentPos);
        if (parentState.getBlock() instanceof CompanyTrainPanel) return InteractionResult.PASS;
        return parentState.useWithoutItem(level, player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos parentPos = getParentPos(level, pos);
        if (parentPos == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        BlockState parentState = level.getBlockState(parentPos);
        if (parentState.getBlock() instanceof CompanyTrainPanel) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return parentState.useItemOn(stack, level, player, hand, hitResult);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    public static BlockPos getParentPos(BlockGetter level, BlockPos myPos) {
        BlockEntity entity = level.getBlockEntity(myPos);
        if (!(entity instanceof CompanyTrainBlockEntity companyTrainBlockEntity)) return null;
        return companyTrainBlockEntity.getParentPos().offset(myPos);
    }

    public static void setParentBlock(BlockPos myPos, BlockGetter level, BlockState state, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(myPos);
        if (!(entity instanceof CompanyTrainBlockEntity companyTrainBlockEntity)) return;
        companyTrainBlockEntity.setParentPos(pos.subtract(myPos), state.getValue(FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CompanyTrainBlockEntity(blockPos, blockState);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity entity = level.getBlockEntity(context.getClickedPos());
        if (!(entity instanceof CompanyTrainBlockEntity)) return InteractionResult.PASS;
        BlockPos parentPos = context.getClickedPos().offset(((CompanyTrainBlockEntity) entity).getParentPos());
        BlockState state1 = level.getBlockState(parentPos);
        if (!(state1.getBlock() instanceof TrainPanelBlock)) return InteractionResult.PASS;
        Vec3 hitLoc = context.getClickLocation().add(DirectionUtil.toVec3(((CompanyTrainBlockEntity) entity).getParentPos()));
        BlockHitResult result = new BlockHitResult(hitLoc, context.getClickedFace(), parentPos, context.isInside());
        UseOnContext context1 = new UseOnContext(context.getPlayer(), context.getHand(), result);
        return ((IWrenchable)state1.getBlock()).onWrenched(state1, context1);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity entity = level.getBlockEntity(context.getClickedPos());
        if (!(entity instanceof CompanyTrainBlockEntity)) return InteractionResult.PASS;
        BlockPos parentPos = context.getClickedPos().offset(((CompanyTrainBlockEntity) entity).getParentPos());
        BlockState state1 = level.getBlockState(parentPos);
        if (!(state1.getBlock() instanceof TrainPanelBlock)) return InteractionResult.PASS;
        Vec3 hitLoc = context.getClickLocation().add(DirectionUtil.toVec3(((CompanyTrainBlockEntity) entity).getParentPos()));
        BlockHitResult result = new BlockHitResult(hitLoc, context.getClickedFace(), parentPos, context.isInside());
        UseOnContext context1 = new UseOnContext(context.getPlayer(), context.getHand(), result);
        return ((IWrenchable)state1.getBlock()).onSneakWrenched(state1, context1);
    }
}
