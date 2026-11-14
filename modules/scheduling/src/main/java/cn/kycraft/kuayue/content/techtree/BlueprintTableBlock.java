package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Blueprint table.
 *
 * <p>Horizontal facing matches the reference blockstate ({@code facing=north/east/south/west}) and the
 * world-space blueprint scene is oriented with it. Plain use opens the LDLib2 table GUI, which carries the
 * slots; sneak-use toggles the standalone model scene.</p>
 *
 * <p>Sneak-use is a temporary entry point for the standalone {@link CarModelUi} scene.</p>
 */
public class BlueprintTableBlock extends HorizontalDirectionalBlock implements EntityBlock, TechTreeMenuHost {

    public static final MapCodec<BlueprintTableBlock> CODEC = simpleCodec(BlueprintTableBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public BlueprintTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // The board faces the player who places the table, like the reference block.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) {
            table.setYaw(yawOf(state.getValue(FACING)));
        }
        if (level.isClientSide) {
            // Mount the overlay as soon as the table exists, so the text never depends on using the block.
            CarModelUiDemo.scheduleOverlay(pos);
        }
    }

    /** Degrees used by the board transform for each facing. */
    public static float yawOf(Direction facing) {
        return switch (facing) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) {
                // The overlay is permanent; only the model is toggled.
                CarModelUiDemo.ensureOverlay(table);
                if (player.isShiftKeyDown()) {
                    // TEMPORARY entry point for the standalone car scene, replaced by the real integration.
                    CarModelUiDemo.toggle(table);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown()
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            TechTreeMenuHost.openTable(serverPlayer, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide) BlueprintTableRegistry.dropSlots(pos);
        if (level.isClientSide) {
            // The model and its text column live outside the block entity, so removing the table has to take
            // them down explicitly; otherwise they stay hanging where the table used to be.
            CarModelUiDemo.hide(pos.immutable());
            CarModelUiDemo.hideOverlay(pos.immutable());
            TechTreeBoard.close();
        }
    }
}
