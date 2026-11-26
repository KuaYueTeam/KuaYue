package cn.kycraft.kuayue.content.tech_tree;

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

    /**
     * Board rotation, derived from the block state.
     *
     * <p>The facing is authoritative and already present on both sides; a stored float had to be written
     * from {@code setPlacedBy} and synced through {@code getUpdateTag()}, and any gap in either path left the
     * client reading 0 — which is exactly what the board showed for every facing.</p>
     */
    public float yaw() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlueprintTableBlock.FACING)) {
            return BlueprintTableBlock.yawOf(state.getValue(BlueprintTableBlock.FACING));
        }
        return 0f;
    }

    /**
     * Kept for source compatibility; the rotation now comes from the block state.
     *
     * @deprecated the facing is the single source of truth
     */
    @Deprecated
    public void setYaw(float yaw) {
        // no-op: see yaw()
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("selected_group", selectedGroup);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedGroup = tag.getString("selected_group");
        // "yaw" is deliberately not read back: rotation follows the block state now. Old tables keep the
        // stale float in their NBT, which simply goes unused.
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
