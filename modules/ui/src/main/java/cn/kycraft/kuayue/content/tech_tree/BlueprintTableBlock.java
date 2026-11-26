package cn.kycraft.kuayue.content.tech_tree;

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
        // No yaw to store: the block state carries the facing, and BlueprintTableBlockEntity.yaw() derives
        // the board rotation from it on both sides.
        if (level.isClientSide) {
            // The board is permanent furniture: mount it as soon as the table exists instead of waiting for
            // an interaction.
            mountBoard(level, pos);
        }
    }

    /**
     * Mounts the board on a table that was just loaded rather than placed.
     *
     * <p>{@code setPlacedBy} only runs for a player placement; a table in a saved chunk needs the same
     * mount, which the client tick does once the block entity is actually in the level.</p>
     */
    static void mountBoard(Level level, BlockPos pos) {
        if (!level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) {
            TechTreeTableSurface.mount(table);
        }
    }

    /** Degrees used by the board transform for each facing. */
    /**
     * Board rotation for a facing, in degrees, where SOUTH is the orientation that reads upright.
     *
     * <p>The table cannot be a plain clockwise count from NORTH: the board is anchored on SOUTH, so NORTH is
     * the far side of the cycle. Expressing it against the enum instead of four hard-coded cases keeps the
     * relationship in one place — {@code clockwise} is 90&deg; each step, {@code getOpposite} is 180&deg;.</p>
     *
     * <p>Consumers: {@code BlueprintTableBlockEntity.yaw()} feeds this straight into a {@code rotateY}, and
     * {@code TechTreeTableSurface.tableTransform} adds a fixed 180 on top. Change one, check the other.</p>
     */
    public static float yawOf(Direction facing) {
        // Rotate from the reference facing toward the requested one, one quarter turn at a time.
        Direction cursor = REFERENCE_FACING;
        for (int quarter = 0; quarter < 4; quarter++) {
            if (cursor == facing) return quarter * 90f;
            cursor = cursor.getClockWise();
        }
        return 0f;
    }

    /** The facing whose board orientation reads upright to the player; the yaw cycle starts here. */
    public static final Direction REFERENCE_FACING = Direction.SOUTH;

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintTableBlockEntity(pos, state);
    }

    /**
     * Right click opens the tech tree board flat on the table top.
     *
     * <p>Client half only: the board is a view of data the client already holds, so nothing is exchanged
     * with the server to open it. The handshake is the one the world UI backend expects — the surface is
     * registered, then the invisible pointer screen takes the cursor so the plates can be hit; Escape
     * hands the cursor back to mouse-look.</p>
     *
     * <p>Sneak-right-click still toggles the blueprint model, which is the temporary entry point of
     * {@link CarModelUiDemo}.</p>
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof BlueprintTableBlockEntity table) {
                if (player.isShiftKeyDown()) {
                    // TEMPORARY entry point for the standalone car scene, replaced by the real integration.
                    CarModelUiDemo.toggle(table);
                } else {
                    TechTreeTableSurface.focusTable(table);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Nothing to do on the server: the snapshot comes back over the RPC the client sends once the
        // surface is mounted, and the tree has no container to open.
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
            TechTreeTableSurface.pruneNow(pos.immutable());
        }
    }
}
