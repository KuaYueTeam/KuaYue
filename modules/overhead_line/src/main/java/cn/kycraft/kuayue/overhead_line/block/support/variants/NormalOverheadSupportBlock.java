package cn.kycraft.kuayue.overhead_line.block.support.variants;

import cn.kycraft.kuayue.overhead_line.block.support.OverheadLineAnchorTransform;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineSync;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineWireKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Objects;

import java.util.List;
import java.util.Optional;

public class NormalOverheadSupportBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public enum TrussShape {
        FULL_BLOCK, SMALL, BIG, PILLAR
    }

    private static final VoxelShape SMALL_TRUSS_NS = Shapes.box(0.4375, 0.0, 0.0, 0.5625, 1.0, 1.0);
    private static final VoxelShape SMALL_TRUSS_WE = Shapes.box(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5625);
    private static final VoxelShape BIG_TRUSS_NS = Shapes.box(0.25, 0.0, 0.0, 0.75, 1.0, 1.0);
    private static final VoxelShape BIG_TRUSS_WE = Shapes.box(0.0, 0.0, 0.25, 1.0, 1.0, 0.75);
    private static final VoxelShape PILLAR_TRUSS_NS = Shapes.box(0.25, 0.25, 0.0, 0.75, 0.75, 1.0);
    private static final VoxelShape PILLAR_TRUSS_WE = Shapes.box(0.0, 0.25, 0.25, 1.0, 0.75, 0.75);

    private final TrussShape trussShape;
    private final List<Vec3> connectionPoints;
    private final int connectionMax;

    public NormalOverheadSupportBlock(BlockBehaviour.Properties properties, List<Vec3> connectionPoints) {
        this(properties, TrussShape.FULL_BLOCK, connectionPoints);
    }

    public NormalOverheadSupportBlock(BlockBehaviour.Properties properties, TrussShape trussShape, List<Vec3> connectionPoints) {
        this(properties, trussShape, connectionPoints, Integer.MAX_VALUE);
    }

    public NormalOverheadSupportBlock(BlockBehaviour.Properties properties, TrussShape trussShape,
                                      List<Vec3> connectionPoints, int connectionMax) {
        super(properties);
        this.trussShape = Objects.requireNonNull(trussShape, "trussShape");
        this.connectionPoints = List.copyOf(connectionPoints);
        this.connectionMax = connectionMax;
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.EAST));
    }

    public int getConnectionMax() {
        return connectionMax;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, trussShape == TrussShape.FULL_BLOCK ? direction : direction.getOpposite());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        super.onRemove(state, level, pos, replacement, movedByPiston);
        if (!replacement.is(this) && level instanceof ServerLevel serverLevel) {
            OverheadLineSync.removeAt(serverLevel, pos);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(trussShape, state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return trussShape == TrussShape.FULL_BLOCK ? super.getCollisionShape(state, level, pos, context) : shapeOf(trussShape, state.getValue(FACING));
    }

    private static VoxelShape shapeOf(TrussShape trussShape, Direction facing) {
        boolean westEast = facing == Direction.EAST || facing == Direction.WEST;
        return switch (trussShape) {
            case FULL_BLOCK -> Shapes.block();
            case BIG -> westEast ? BIG_TRUSS_WE : BIG_TRUSS_NS;
            case PILLAR -> westEast ? PILLAR_TRUSS_WE : PILLAR_TRUSS_NS;
            case SMALL -> westEast ? SMALL_TRUSS_WE : SMALL_TRUSS_NS;
        };
    }

    public Optional<Vec3> resolveAnchor(BlockPos pos, BlockState state, int pointIndex,
                                        OverheadLineWireKind kind) {
        if (pointIndex < 0 || pointIndex >= connectionPoints.size()) return Optional.empty();
        Vec3 anchor = OverheadLineAnchorTransform.toWorld(pos, state.getValue(FACING), connectionPoints.get(pointIndex));
        return Optional.of(adjustWireAnchor(anchor, state, kind));
    }

    protected Vec3 adjustWireAnchor(Vec3 anchor, BlockState state,
                                    OverheadLineWireKind kind) {
        return anchor;
    }

    public int getConnectionPointCount(){
        return connectionPoints.size();
    }
}
