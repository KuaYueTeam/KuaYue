package cn.kycraft.kuayue.parts.core.panel.window;

import cn.kycraft.kuayue.parts.core.panel.PanelBlockItem;
import cn.kycraft.kuayue.parts.core.panel.SlabBlockItem;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.utils.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TrainOpenableWindowBlock extends TrainPanelBlock {
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public TrainOpenableWindowBlock(Properties pProperties, int wide) {
        super(pProperties, new Vec2(-wide + 1, 0), new Vec2(1, 1));
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(TrainPanelBlock.FACING, Direction.EAST)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
        );
    }

    public TrainOpenableWindowBlock(Properties pProperties, int left, int right) {
        super(pProperties, new Vec2(left, 0), new Vec2(right, 1));
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(TrainPanelBlock.FACING, Direction.EAST)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
        );
    }

    public TrainOpenableWindowBlock(Properties pProperties, int left, int right, int height) {
        super(pProperties, new Vec2(left, 0), new Vec2(right, height));
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(TrainPanelBlock.FACING, Direction.EAST)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
        );
    }

    public TrainOpenableWindowBlock(Properties properties) {
        super(properties, new Vec2(0, 0), new Vec2(1, 1));
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(TrainPanelBlock.FACING, Direction.EAST)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(OPEN, HINGE));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState()
                .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
                .setValue(HINGE, getHinge(pContext)).setValue(OPEN, false);
    }

    public static DoorHingeSide getHinge(BlockPlaceContext context) {
        Vec3 location = context.getClickLocation();
        Direction direction = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos(),
                leftPos = DirectionUtil.left(pos, direction, 1),
                rightPos = DirectionUtil.right(pos, direction, 1);
        Vec3 leftCenter = new Vec3(((float) leftPos.getX()) + .5f, leftPos.getY(), ((float) leftPos.getZ()) + .5f),
                rightCenter = new Vec3(((float) rightPos.getX()) + .5f, rightPos.getY(), ((float) rightPos.getZ()) + .5f);
        return location.distanceToSqr(leftCenter) > location.distanceToSqr(rightCenter) ?
                DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return TrainSmallWindowBlock.windowUse(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(stack.getItem() instanceof PanelBlockItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        TrainSmallWindowBlock.windowUse(state, level, pos, player, hand, hitResult);
        return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }
}
