package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * State of one blueprint table.
 *
 * <p>The board itself is a pure client view built from {@link ClientTreeState}; the block entity only
 * remembers which group is displayed and how the board is laid out on the table top.</p>
 */
public class BlueprintTableBlockEntity extends BlockEntity {

    private String selectedGroup = "";
    private float yaw;

    public BlueprintTableBlockEntity(BlockPos pos, BlockState state) {
        super(BlueprintTableRegistry.BLOCK_ENTITY.getEntry(), pos, state);
    }

    public String selectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(String group) {
        if (selectedGroup.equals(group)) return;
        selectedGroup = group;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public float yaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("selected_group", selectedGroup);
        tag.putFloat("yaw", yaw);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedGroup = tag.getString("selected_group");
        yaw = tag.getFloat("yaw");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
}
