package cn.kycraft.kuayue.parts.core.multiblock;

import com.mojang.serialization.MapCodec;
import lib.kasuga.create.interaction.ContraptionShapeProvider;
import lib.kasuga.registration.minecraft.block.shape.BlockShapeBindings;
import lib.kasuga.registration.minecraft.block.shape.ShapeSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible KuaYue selection cell for an anchor block whose model extends outside its own voxel.
 */
public final class MultiCellCompanionBlock extends BaseEntityBlock implements ContraptionShapeProvider {
    public MultiCellCompanionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MultiCellCompanionBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiCellCompanionBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getContraptionShape(BlockState state, BlockGetter localBlocks, BlockPos localPos) {
        return state.getShape(localBlocks, localPos);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return Shapes.empty();
        }
        BlockState parentState = level.getBlockState(parentPos);
        if (parentState.isAir() || parentState.getBlock() instanceof MultiCellCompanionBlock) {
            return Shapes.empty();
        }
        BlockPos offset = parentPos.subtract(pos);
        VoxelShape parentShape = parentState.getShape(level, parentPos, context);
        return shapeForCell(parentShape, offset);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return Shapes.empty();
        }
        BlockState parentState = level.getBlockState(parentPos);
        if (!(parentState.getBlock() instanceof TrainPanelBlock parent)
                || !parent.usesShapeCoverage()) {
            return Shapes.empty();
        }
        VoxelShape parentCollision = BlockShapeBindings.resolveFull(
                parentState,
                ShapeSlot.COLLISION,
                () -> parentState.getCollisionShape(level, parentPos, context)
        );
        return shapeForCell(parentCollision, parentPos.subtract(pos));
    }

    @Override
    public VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return InteractionResult.PASS;
        }
        BlockState parentState = level.getBlockState(parentPos);
        if (parentState.isAir() || parentState.getBlock() instanceof MultiCellCompanionBlock) {
            return InteractionResult.PASS;
        }
        return parentState.useWithoutItem(level, player, parentHit(parentPos, hitResult));
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
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockState parentState = level.getBlockState(parentPos);
        if (parentState.isAir() || parentState.getBlock() instanceof MultiCellCompanionBlock) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return parentState.useItemOn(stack, level, player, hand, parentHit(parentPos, hitResult));
    }

    @Override
    protected float getDestroyProgress(
            BlockState state,
            Player player,
            BlockGetter level,
            BlockPos pos
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return 0f;
        }
        BlockState parentState = level.getBlockState(parentPos);
        return parentState.isAir()
                ? 0f
                : parentState.getDestroyProgress(player, level, parentPos);
    }

    @Override
    public boolean canHarvestBlock(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Player player
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return false;
        }
        BlockState parentState = level.getBlockState(parentPos);
        return !parentState.isAir()
                && parentState.canHarvestBlock(level, parentPos, player);
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            LevelReader level,
            BlockPos pos,
            Player player
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return ItemStack.EMPTY;
        }
        BlockState parentState = level.getBlockState(parentPos);
        return parentState.isAir()
                ? ItemStack.EMPTY
                : parentState.getCloneItemStack(target, level, parentPos, player);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos != null) {
            BlockState parentState = level.getBlockState(parentPos);
            if (!parentState.isAir()) {
                Block parentBlock = parentState.getBlock();
                BlockState afterWillDestroy = parentBlock.playerWillDestroy(
                        level,
                        parentPos,
                        parentState,
                        player
                );
                if (!level.isClientSide) {
                    BlockEntity parentEntity = level.getBlockEntity(parentPos);
                    if (level.removeBlock(parentPos, false)) {
                        parentBlock.destroy(level, parentPos, afterWillDestroy);
                        if (!player.isCreative()
                                && player.hasCorrectToolForDrops(afterWillDestroy)) {
                            parentBlock.playerDestroy(
                                    level,
                                    player,
                                    parentPos,
                                    afterWillDestroy,
                                    parentEntity,
                                    player.getMainHandItem().copy()
                            );
                        }
                    }
                }
            }
        }
        return state;
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid
    ) {
        BlockPos parentPos = parentPos(level, pos);
        if (parentPos == null) {
            return level.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL);
        }
        BlockState parentState = level.getBlockState(parentPos);
        if (parentState.isAir() || parentState.getBlock() instanceof MultiCellCompanionBlock) {
            return level.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL);
        }
        BlockEntity parentEntity = parentState.hasBlockEntity()
                ? level.getBlockEntity(parentPos)
                : null;
        boolean removed = parentState.onDestroyedByPlayer(
                level,
                parentPos,
                player,
                willHarvest,
                level.getFluidState(parentPos)
        );
        if (removed) {
            parentState.getBlock().destroy(level, parentPos, parentState);
            if (willHarvest
                    && !level.isClientSide
                    && !player.isCreative()
                    && player.hasCorrectToolForDrops(parentState)) {
                parentState.getBlock().playerDestroy(
                        level,
                        player,
                        parentPos,
                        parentState,
                        parentEntity,
                        player.getMainHandItem()
                );
            }
        }
        return removed;
    }

    private static BlockHitResult parentHit(BlockPos parentPos, BlockHitResult hitResult) {
        return new BlockHitResult(
                hitResult.getLocation(),
                hitResult.getDirection(),
                parentPos,
                hitResult.isInside()
        );
    }

    private static @Nullable BlockPos parentPos(BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof MultiCellCompanionBlockEntity companion
                ? companion.getParentPos()
                : null;
    }

    static VoxelShape shapeForCell(VoxelShape parentShape, BlockPos parentOffset) {
        return Shapes.join(
                parentShape.move(
                        parentOffset.getX(),
                        parentOffset.getY(),
                        parentOffset.getZ()
                ),
                Shapes.block(),
                BooleanOp.AND
        );
    }
}
