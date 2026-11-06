package cn.kycraft.kuayue.provisions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

/**
 * 火车盒饭：两阶段食物方块（满 → 空）。
 *
 * <p>第一口吃掉内容物并把 {@code empty} 置 true；第二口移除方块并返还餐盒。</p>
 */
public class TrainDietBoxBlock extends PlacementFoodBlock {

    public static final BooleanProperty EMPTY = BooleanProperty.create("empty");

    public TrainDietBoxBlock(Properties properties, FoodType foodType) {
        super(properties, foodType);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, net.minecraft.core.Direction.EAST)
                .setValue(EMPTY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EMPTY);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(EMPTY, false);
    }

    @Override
    protected InteractionResult eat(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(true)) {
            return InteractionResult.PASS;
        }
        if (!state.getValue(EMPTY)) {
            ItemStack stack = foodStack(state);
            player.eat(level, stack);
            level.gameEvent(player, GameEvent.EAT, pos);
            BlockState changed = state.setValue(EMPTY, true);
            level.setBlock(pos, changed, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            return InteractionResult.SUCCESS;
        }
        // 已吃完：移除方块并返还餐盒
        level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.removeBlock(pos, false);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        giveOrDrop(remainderAfterEating(state), player);
        return InteractionResult.SUCCESS;
    }
}
