package cn.kycraft.kuayue.parts.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Client-synchronized ownership link from a companion cell to its anchor. */
public final class MultiCellCompanionBlockEntity extends BlockEntity {
    private BlockPos parentOffset = BlockPos.ZERO;

    public MultiCellCompanionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MultiCellCompanionBlockEntity(BlockPos pos, BlockState state) {
        this(MultiCellRegistry.MULTI_CELL_COMPANION_ENTITY.getEntry(), pos, state);
    }

    public BlockPos getParentPos() {
        return worldPosition.offset(parentOffset);
    }

    public BlockPos getParentOffset() {
        return parentOffset;
    }

    public void setParentOffset(BlockPos parentOffset) {
        this.parentOffset = parentOffset.immutable();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("parent_x", parentOffset.getX());
        tag.putInt("parent_y", parentOffset.getY());
        tag.putInt("parent_z", parentOffset.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        parentOffset = new BlockPos(
                tag.getInt("parent_x"),
                tag.getInt("parent_y"),
                tag.getInt("parent_z")
        );
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
