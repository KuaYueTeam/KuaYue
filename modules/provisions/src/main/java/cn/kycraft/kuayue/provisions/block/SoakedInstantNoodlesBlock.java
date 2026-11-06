package cn.kycraft.kuayue.provisions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 已冲泡的方便面：三状态机。
 *
 * <table>
 *   <tr><th>WITH_FORK</th><td>初始，右键发出 WOOL_BREAK 音效进入 OPENING；踩踏受伤</td></tr>
 *   <tr><th>OPENING</th><td>每 tick 冒蒸汽，右键食用（营养 10 + 温暖 2min）进入 FINISH_EATING；踩踏受伤</td></tr>
 *   <tr><th>FINISH_EATING</th><td>空碗，右键 WOOL_BREAK 后移除；踩踏不受伤</td></tr>
 * </table>
 *
 * <p>1.0 里这个方块继承 {@code PlacementFoodBlock}，因此自带 {@code FACING}；资源也有
 * facing × noodles_state 共 12 个变体，所以这里必须同样注册朝向属性。</p>
 */
public class SoakedInstantNoodlesBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<NoodlesState> NOODLES_STATE = NoodlesState.PROPERTY;

    public SoakedInstantNoodlesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.EAST)
                .setValue(NOODLES_STATE, NoodlesState.WITH_FORK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NOODLES_STATE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(NOODLES_STATE, NoodlesState.WITH_FORK);
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
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                                 BlockPos pos, Player player,
                                                                 net.minecraft.world.InteractionHand hand,
                                                                 net.minecraft.world.phys.BlockHitResult hit) {
        return eat(level, pos, state, player).consumesAction()
                ? net.minecraft.world.ItemInteractionResult.SUCCESS
                : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult eat(Level level, BlockPos pos, BlockState state, Player player) {
        NoodlesState current = state.getValue(NOODLES_STATE);
        switch (current) {
            case WITH_FORK -> {
                level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.setBlock(pos, state.setValue(NOODLES_STATE, NoodlesState.OPENING), 11);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                return InteractionResult.SUCCESS;
            }
            case OPENING -> {
                if (!player.canEat(true)) {
                    return InteractionResult.PASS;
                }
                player.eat(level, state.getBlock().asItem().getDefaultInstance());
                level.gameEvent(player, GameEvent.EAT, pos);
                level.setBlock(pos, state.setValue(NOODLES_STATE, NoodlesState.FINISH_EATING), 11);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                return InteractionResult.SUCCESS;
            }
            case FINISH_EATING -> {
                level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.removeBlock(pos, false);
                level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
                return InteractionResult.SUCCESS;
            }
            default -> {
                return InteractionResult.FAIL;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(NOODLES_STATE) == NoodlesState.OPENING) {
            double d1 = 0.1 * random.nextFloat();
            double d2 = 0.1 * random.nextFloat();
            double d3 = 0.1 * random.nextFloat();
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.4 + d1, pos.getY() + 0.75 + d2, pos.getZ() + 0.4 + d3,
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof LivingEntity living && state.getValue(NOODLES_STATE) != NoodlesState.FINISH_EATING) {
            living.hurt(level.damageSources().source(DamageTypes.HOT_FLOOR), 0.5F);
            if (level.isClientSide() && entity instanceof Player player) {
                player.displayClientMessage(
                        Component.translatable("msg.kuayue.step_on_soaked_instant_noodles"), true);
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
