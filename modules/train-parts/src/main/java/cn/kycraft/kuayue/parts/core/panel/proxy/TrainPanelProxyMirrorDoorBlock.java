package cn.kycraft.kuayue.parts.core.panel.proxy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.door.TrainDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Proxy panel host that merges a right-hand and a left-hand door into one block.
 *
 * <p>The old pack shipped the two mirror halves as separate blocks. Here the placement side
 * picks the art and a right-click switches the pose: {@link #DOOR} carries both axes because
 * {@code proxy_model_be.model_by_state} can only select on one property at a time, while
 * {@link #OPEN} stays the collision key ({@code facing=...,open=...} in {@code params.aabb}).
 */
public class TrainPanelProxyMirrorDoorBlock extends TrainPanelProxyBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public enum Door implements StringRepresentable {
        LEFT_CLOSED("left_closed"),
        LEFT_OPEN("left_open"),
        RIGHT_CLOSED("right_closed"),
        RIGHT_OPEN("right_open");

        private final String name;

        Door(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean left() {
            return this == LEFT_CLOSED || this == LEFT_OPEN;
        }

        public Door withOpen(boolean value) {
            return left()
                    ? (value ? LEFT_OPEN : LEFT_CLOSED)
                    : (value ? RIGHT_OPEN : RIGHT_CLOSED);
        }

        public Door flipped() {
            return switch (this) {
                case LEFT_CLOSED -> RIGHT_CLOSED;
                case LEFT_OPEN -> RIGHT_OPEN;
                case RIGHT_CLOSED -> LEFT_CLOSED;
                case RIGHT_OPEN -> LEFT_OPEN;
            };
        }
    }

    public static final EnumProperty<Door> DOOR = EnumProperty.create("door", Door.class);

    public TrainPanelProxyMirrorDoorBlock(Properties properties, MultiCellFootprint footprint) {
        super(properties, footprint);
        registerDefaultState(defaultBlockState()
                .setValue(OPEN, false)
                .setValue(DOOR, Door.RIGHT_CLOSED)
                .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.RIGHT));
    }

    /**
     * The hinge side is declared as well even though {@link #DOOR} already carries it: the
     * collision plan is keyed by {@code facing=…,hinge=…,open=…}, and the two sides swing about
     * opposite edges, so they need their own boxes.
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(OPEN, DOOR, BlockStateProperties.DOOR_HINGE));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        boolean left = TrainOpenableWindowBlock.getHinge(context) == DoorHingeSide.LEFT;
        return state.setValue(OPEN, false)
                .setValue(DOOR, left ? Door.LEFT_CLOSED : Door.RIGHT_CLOSED)
                .setValue(BlockStateProperties.DOOR_HINGE,
                        left ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE
                ? mirrored
                : mirrored.setValue(DOOR, mirrored.getValue(DOOR).flipped());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return toggle(state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof TrainPanelBlock) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        toggle(state, level, pos, player, hand, hitResult);
        return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    /** Flips OPEN and the matching DOOR pose together (windowUse only cycles OPEN). */
    private static InteractionResult toggle(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (player.getItemInHand(hand).getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof TrainPanelBlock) {
            return InteractionResult.PASS;
        }
        TrainPanelBlock.BlockUseFunction function =
                (l, parentPos, parentState, myPos, myState, player1, hand1, hit1) -> {
                    if (!(myState.getBlock() instanceof TrainPanelProxyMirrorDoorBlock)) {
                        return InteractionResult.FAIL;
                    }
                    boolean open = !myState.getValue(OPEN);
                    myState = myState.setValue(OPEN, open)
                            .setValue(DOOR, myState.getValue(DOOR).withOpen(open));
                    level.setBlock(myPos, myState, 10);
                    level.levelEvent(player1,
                            open ? TrainDoorBlock.getOpenSound(myState) : TrainDoorBlock.getCloseSound(myState),
                            myPos, 0);
                    level.gameEvent(player1,
                            open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, myPos);
                    return InteractionResult.sidedSuccess(l.isClientSide);
                };
        ((TrainPanelBlock) state.getBlock()).walkAllValidPos(level, pos, state, player, hand, hit, function);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
