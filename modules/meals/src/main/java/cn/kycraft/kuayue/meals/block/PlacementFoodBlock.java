package cn.kycraft.kuayue.meals.block;

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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 放置类食物的基类：地面上的小方块，右键食用。
 *
 * <p>Ported from the 1.0 {@code PlacementFoodBlock}. The 1.0 base relied on
 * {@code ExternalRemainderBlockItem}; here the returned item is asked from the block itself through
 * {@link #remainderAfterEating(BlockState)}.</p>
 */
public class PlacementFoodBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected final FoodType foodType;

    public PlacementFoodBlock(Properties properties, FoodType foodType) {
        super(properties);
        this.foodType = foodType;
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.EAST));
    }

    public FoodType foodType() {
        return foodType;
    }

    /** 吃完后返还的物品；默认什么也不返还。 */
    protected ItemStack remainderAfterEating(BlockState state) {
        return ItemStack.EMPTY;
    }

    /** 该方块“对应”的食物物品栈（用于读取食物数值）。 */
    protected ItemStack foodStack(BlockState state) {
        return state.getBlock().asItem().getDefaultInstance();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return foodType.shape();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return foodType.shape();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // 1.21.1 中“方块被右键”走 useItemOn；转交给 1.0 风格的 use() 逻辑。
        return toItemResult(use(state, level, pos, player, hand, hit));
    }

    private static ItemInteractionResult toItemResult(InteractionResult result) {
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    /**
     * 1.0 风格的使用入口。子类覆盖 {@link #eat}，其余情况按原逻辑返回。
     */
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (eat(level, pos, state, player).consumesAction()) {
            return InteractionResult.SUCCESS;
        }
        return stack.isEmpty() ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    protected InteractionResult eat(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(true)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = foodStack(state);
        ItemStack remainder = remainderAfterEating(state);
        player.eat(level, stack);
        level.gameEvent(player, GameEvent.EAT, pos);
        level.removeBlock(pos, false);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        giveOrDrop(remainder, player);
        return InteractionResult.SUCCESS;
    }

    protected void giveOrDrop(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
