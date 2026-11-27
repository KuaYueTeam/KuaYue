package cn.kycraft.kuayue.parts.core.panel;

import cn.kycraft.kuayue.parts.core.panel.company.CompanyRegistry;
import cn.kycraft.kuayue.parts.core.panel.company.CompanyTrainPanel;
import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.multiblock.MultiCellStructureManager;
import cn.kycraft.kuayue.utils.DirectionUtil;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import lib.kasuga.content.block.UnModeledBlockProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class TrainPanelBlock extends Block implements IWrenchable /*, EntityBlock */ {
    public final Vec2 beginPos, endPos;
    private MultiCellFootprint multiCellFootprint = MultiCellFootprint.EMPTY;
    private Map<String, VoxelShape> modelAabbShapes = ModelAabbShapes.NONE;
    private boolean shapeCoverage;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final UnModeledBlockProperty<TrainPanelProperties.EditType, EnumProperty<TrainPanelProperties.EditType>> EDIT_TYPE =
            UnModeledBlockProperty.create(EnumProperty.create("edit_type", TrainPanelProperties.EditType.class));

    public TrainPanelBlock(Properties pProperties, Vec2 beginPos, Vec2 endPos) {
        super(pProperties);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(EDIT_TYPE, TrainPanelProperties.EditType.NONE)
        );
        this.beginPos = beginPos;
        this.endPos = endPos;
    }

    public TrainPanelBlock(Properties properties) {
        this(properties, Vec2.ZERO, Vec2.ONE);
    }

    /**
     * Enables KuaYue footprint companions for this panel without changing its model or rendering root.
     * Factories may call this once while constructing the registered block.
     */
    public final void configureMultiCell(MultiCellFootprint footprint) {
        if (footprint.isEmpty()) {
            return;
        }
        int width = (int) (endPos.x - beginPos.x);
        int height = (int) (endPos.y - beginPos.y);
        if (width > 1 || height > 1) {
            throw new IllegalArgumentException(
                    "multi_cell cannot be combined with a legacy company footprint wider or taller than one cell");
        }
        if (!multiCellFootprint.isEmpty() && !multiCellFootprint.equals(footprint)) {
            throw new IllegalStateException("multi-cell footprint was already configured");
        }
        footprint.validate(defaultBlockState());
        multiCellFootprint = footprint;
    }

    public final MultiCellFootprint multiCellFootprint() {
        return multiCellFootprint;
    }

    /** Enables per-cell collision coverage from the installed lib shape plan. */
    public final void configureShapeCoverage(boolean enabled) {
        shapeCoverage = enabled;
    }

    public final boolean usesShapeCoverage() {
        return shapeCoverage;
    }

    public final boolean canPlaceMultiCell(BlockGetter level, BlockPos pos, BlockState state) {
        return multiCellFootprint.isEmpty()
                || MultiCellStructureManager.canPlace(level, pos, state, multiCellFootprint);
    }

    protected final void ensureMultiCell(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel && !multiCellFootprint.isEmpty()) {
            MultiCellStructureManager.ensure(serverLevel, pos, state, multiCellFootprint);
        }
    }

    /**
     * Installs model-derived AABBs. Factories call this once while constructing the block,
     * using the same params object that carries the other factory arguments.
     */
    public final void configureModelAabb(Map<String, VoxelShape> shapes) {
        this.modelAabbShapes = shapes == null || shapes.isEmpty() ? ModelAabbShapes.NONE : shapes;
    }

    /** @return the model AABB for this state, or {@code null} when no model AABB was configured. */
    @Nullable
    protected final VoxelShape modelAabbShape(BlockState state) {
        return ModelAabbShapes.lookup(modelAabbShapes, state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canPlaceMultiCell(level, pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        VoxelShape modelShape = modelAabbShape(pState);
        if (modelShape != null) {
            return modelShape;
        }
        return TrainPanelShapes.getShape(pState.getValue(FACING).getOpposite());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, EDIT_TYPE);
    }

    // @TODO: 水牌迁移
//    @Nullable
//    @Override
//    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, BlockState pState) {
//        if (pState.getValue(EDIT_TYPE) != TrainPanelProperties.EditType.NONE) {
//            return new EditablePanelEntity(pPos, pState);
//        }
//        return null;
//    }

//    @Override
//    protected ItemInteractionResult useItemOn(ItemStack item, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
//
//        BlockState neoState = pState;
//        // 手持剪贴板
//        TrainPanelProperties.EditType orgType = pState.getValue(EDIT_TYPE);
//        boolean changed = false;
//        // 手持水牌
//        if (
//                (item.is(EditablePanelItem.LAQUERED_BOARD.getItem()) || (item.is(EditablePanelItem.COLORED_BRUSH.getItem()) && TrainPanelProperties.EditType.LAQUERED.equals(orgType)))
//                && pState.is(Objects.requireNonNull(AllTags.BOTTOM_PANEL.tag()))
//                && neoState.getBlock() instanceof TrainPanelBlock
//        ) {
//            neoState = pState.setValue(EDIT_TYPE, TrainPanelProperties.EditType.LAQUERED);
//            changed = orgType != TrainPanelProperties.EditType.LAQUERED;
//            return openScreen(pLevel, pPos, pPlayer, neoState, changed);
//        }
//        // 创建速度文字的唯一途径
//        // 手持带颜色刷子
//        if ((item.is(EditablePanelItem.COLORED_BRUSH.getItem())  || TrainPanelProperties.EditType.SPEED.equals(orgType))) {
//            // 带有下厢板标签
//            if (pState.is(Objects.requireNonNull(AllTags.BOTTOM_PANEL.tag()))) {
//                neoState = pState.setValue(EDIT_TYPE, TrainPanelProperties.EditType.TYPE);
//                changed = orgType != TrainPanelProperties.EditType.TYPE;
//            }
//            // 带有地板标签
//            if (pState.is(Objects.requireNonNull(AllTags.FLOOR.tag()))) {
//                neoState = pState.setValue(EDIT_TYPE, TrainPanelProperties.EditType.SPEED);
//                changed = orgType != TrainPanelProperties.EditType.SPEED;
//            }
//
//            return openScreen(pLevel, pPos, pPlayer, neoState, changed);
//        }
//        // 手持贴纸
//        if (item.is(EditablePanelItem.STICKER.getItem()) && pState.is(Objects.requireNonNull(AllTags.UPPER_PANEL.tag()))) {
//            neoState = pState.setValue(EDIT_TYPE, TrainPanelProperties.EditType.NUM);
//            changed = orgType != TrainPanelProperties.EditType.NUM;
//            return openScreen(pLevel, pPos, pPlayer, neoState, changed);
//        }
//
//        return ItemInteractionResult.PASS;
//    }
//
//    public ItemInteractionResult openScreen(Level pLevel, BlockPos pPos, Player pPlayer, BlockState neoState, boolean changed) {
//        BlockEntity oldBe = pLevel.getBlockEntity(pPos);
//        if (!(oldBe instanceof EditablePanelEntity) || changed) {
//            BlockEntity blockEntity = newBlockEntity(pPos, neoState);
//            if (blockEntity == null) return ItemInteractionResult.PASS;
//            pLevel.setBlockEntity(blockEntity);
//            pLevel.setBlock(pPos, neoState, 10);
//        }
//        if (!pLevel.isClientSide) {
//            NetworkHooks.openScreen(
//                    (ServerPlayer) pPlayer,
//                    (EditablePanelEntity) pLevel.getBlockEntity(pPos),
//                    pPos);
//        }
//        return ItemInteractionResult.SUCCESS;
//    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext)
                .setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pCurrentPos, pNeighborPos);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        generateCompanyBlock(pLevel, pState, pPos, pIsMoving);
        ensureMultiCell(pLevel, pPos, pState);
    }

    @Override
    public void onBlockStateChange(
            LevelReader level,
            BlockPos pos,
            BlockState oldState,
            BlockState newState
    ) {
        super.onBlockStateChange(level, pos, oldState, newState);
        if (level instanceof ServerLevel serverLevel) {
            ensureMultiCell(serverLevel, pos, newState);
        }
    }

    public boolean hasHinge() {
        if (!defaultBlockState().hasProperty(BlockStateProperties.DOOR_HINGE)) return false;
        int width = (int) endPos.x - (int) beginPos.x;
        if (width % 2 == 0) return true;
        return !(width % 2 == 1 && - beginPos.x == (width / 2));
    }

    public void generateCompanyBlock(Level level, BlockState state, BlockPos pos, boolean isMoving) {
        Direction direction = state.getValue(FACING);
        boolean open = state.hasProperty(BlockStateProperties.OPEN) ?
                state.getValue(BlockStateProperties.OPEN) : false;
        boolean leftHinge = !state.hasProperty(BlockStateProperties.DOOR_HINGE) || state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT;
        BlockUseFunction function = (l, p, parentState, myPos, myState, player, hand, hit) -> {
            if (p.equals(myPos)) return InteractionResult.SUCCESS;
            BlockState state1 = generateCompanyState(direction, leftHinge ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT, open);
            level.setBlock(myPos, state1, 10);
            CompanyTrainPanel.setParentBlock(myPos, level, state1, pos);
            return InteractionResult.SUCCESS;
        };
        walkAllValidPos(level, pos, state, null, null, null, function);
    }

    public BlockState generateCompanyState(Direction direction, DoorHingeSide hingeSide, boolean open) {
        return CompanyRegistry.COMPANY_TRAIN_PANEL.getEntry().defaultBlockState()
                .setValue(CompanyTrainPanel.FACING, direction)
                .setValue(BlockStateProperties.DOOR_HINGE, hingeSide);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pLevel instanceof ServerLevel serverLevel && !pState.is(pNewState.getBlock())) {
            MultiCellStructureManager.removeOwned(serverLevel, pPos, multiCellFootprint);
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        removeCompanyBlock(pLevel, pState, pPos, pIsMoving);
        if(pNewState.getBlock().getClass().equals(pState.getBlock().getClass())) return;
        pLevel.removeBlock(pPos, pIsMoving);
    }

    public void specialRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        onRemove(state, level, pos, newState, isMoving);
        if (!isMoving) dropResources(state, level, pos);
    }

    public void removeCompanyBlock(Level level, BlockState state, BlockPos pos, boolean isMoving) {
        BlockUseFunction function = (l, p, parentState, myPos, myState, player, hand, hit) -> {
            if (p.equals(myPos)) return InteractionResult.SUCCESS;
            l.removeBlockEntity(myPos);
            l.removeBlock(myPos, isMoving);
            return InteractionResult.SUCCESS;
        };
        walkAllValidPos(level, pos, state, null, null, null, function);
    }

    public void walkAllValidPos(Level level, BlockPos blockPos, BlockState parentState,
                                Player player, InteractionHand hand, BlockHitResult hit, BlockUseFunction function) {
        Direction direction = parentState.getValue(FACING);
        BlockPos firstPos = blockPos;
        boolean leftHinge = !parentState.hasProperty(BlockStateProperties.DOOR_HINGE) ||
                parentState.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT;
        firstPos = DirectionUtil.left(firstPos, direction, (int) (leftHinge ? - beginPos.x : beginPos.x));
        firstPos = firstPos.offset(0, (int) beginPos.y, 0);
        int length = (int) (endPos.x - beginPos.x),
                height = (int) (endPos.y - beginPos.y);

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < height; j++) {
                function.apply(level, blockPos, parentState, firstPos, level.getBlockState(firstPos), player, hand, hit);
                firstPos = firstPos.above();
            }
            firstPos = firstPos.offset(0, - height, 0);
            firstPos = DirectionUtil.right(firstPos, direction, leftHinge ? 1 : -1);
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    public interface BlockUseFunction {
        InteractionResult apply(Level level, BlockPos parentPos, BlockState parentState, BlockPos myPos,
                               BlockState myState, Player player, InteractionHand hand, BlockHitResult hit);
    }
}
