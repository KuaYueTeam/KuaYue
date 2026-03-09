package cn.kycraft.kuayue.parts.core.panel.company;

import cn.kycraft.kuayue.utils.DirectionUtil;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CompanyTrainBlockEntity extends BlockEntity {
    private BlockPos parentPos = BlockPos.ZERO;
    private Direction direction = Direction.EAST;

    public CompanyTrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("px", parentPos.getX());
        tag.putInt("py", parentPos.getY());
        tag.putInt("pz", parentPos.getZ());
        NBTHelper.writeEnum(tag, "direction", direction);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.parentPos = new BlockPos(tag.getInt("px"), tag.getInt("py"), tag.getInt("pz"));
        this.direction = NBTHelper.readEnum(tag, "direction", Direction.class);
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
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        this.loadAdditional(tag, lookupProvider);
    }

    public CompanyTrainBlockEntity(BlockPos pos, BlockState state) {
        this(CompanyRegistry.COMPANY_TRAIN_BLOCK_ENTITY.getEntry(), pos, state);
    }

    public Direction getOldDirection() {
        return direction;
    }

    public void updateDirection(Direction newDirection) {
        if (direction == newDirection) return;
        this.parentPos = DirectionUtil.rotate(this.parentPos, direction, newDirection);
        this.direction = newDirection;
        this.setChanged();
        if (hasLevel() && !level.isClientSide) {
            ((ServerLevel) level).getChunkSource().blockChanged(this.getParentPos());
        }
    }

    public void setParentPos(BlockPos pos, Direction direction) {
        this.parentPos = pos;
        this.direction = direction;
        this.setChanged();
        if (hasLevel() && !level.isClientSide) {
            ((ServerLevel) level).getChunkSource().blockChanged(this.getBlockPos());
        }
    }

    public BlockPos getParentPos() {
        return parentPos;
    }
}
