package cn.kycraft.kuayue.parts.core.panel.custom_rendered;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import cn.kycraft.kuayue.parts.core.panel.block_entity.IContraptionMovementBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CustomRenderedEndFaceEntity extends SmartBlockEntity implements IContraptionMovementBlockEntity {

    public boolean open = false;
    public TrainPanelProperties.DoorType type = TrainPanelProperties.DoorType.ROTATE;
    public float counter = 0;

    public CustomRenderedEndFaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CustomRenderedEndFaceEntity(BlockPos pos, BlockState state) {
        this(CustomRenderedRegistry.CUSTOM_RENDERED_END_FACE_ENTITY.getEntry(), pos, state);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("open", isOpen());
        tag.putString("type", getDoorType().getSerializedName());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        open = tag.getBoolean("open");
        type = TrainPanelProperties.DoorType.fromString(tag.getString("type"));
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isOpen() {
        return getBlockState().getOptionalValue(DoorBlock.OPEN).orElse(open);
    }

    public TrainPanelProperties.DoorType getDoorType() {
        return getBlockState().getBlock() instanceof CustomRenderedEndFaceBlock block
                ? block.DOOR_TYPE : type;
    }

    public boolean isRotateDoor() {
        return getDoorType().isRotating();
    }

    public boolean isSingleSided() {
        return getDoorType() == TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        if(level == null) return;
        BlockState state = level.getBlockState(this.getBlockPos());
        if (!(state.getBlock() instanceof CustomRenderedEndFaceBlock block)) return;
        this.open = level.getBlockState(this.getBlockPos()).getValue(DoorBlock.OPEN);
        this.type = block.DOOR_TYPE;
    }

    public Couple<PartialModel> getModels() {
        if (this.level == null) return null;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (!(state.getBlock() instanceof CustomRenderedEndFaceBlock block)) return null;
        return block.models;
    }

    public PartialModel getFrameModel() {
        if (this.level == null) return null;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (!(state.getBlock() instanceof CustomRenderedEndFaceBlock block)) return null;
        return block.frameModel;
    }

    public double getFrameOffsetY() {
        if (this.level == null) return 0;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (!(state.getBlock() instanceof CustomRenderedEndFaceBlock block)) return 0;
        return block.frameOffsetY;
    }

    @Override
    public void update(StructureTemplate.StructureBlockInfo info, Player player, BlockPos pos, AbstractContraptionEntity entity) {
        this.open = !open;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return AABB.ofSize(Vec3.atCenterOf(this.getBlockPos()), 5, 5, 5);
    }
}
