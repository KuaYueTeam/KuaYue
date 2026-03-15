package cn.kycraft.kuayue.parts.core.panel.custom_rendered;

import cn.kycraft.kuayue.parts.core.panel.block_entity.IContraptionMovementBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class CustomRenderedDoorEntity extends SmartBlockEntity implements IContraptionMovementBlockEntity {
    boolean isSlideDoor = false, open = false;
    Vec3 offset;
    Vec3 openoffset;
    public float animation_controller;

    public CustomRenderedDoorEntity(BlockPos pPos, BlockState pBlockState) {
        super(CustomRenderedRegistry.CUSTOM_RENDERED_DOOR_ENTITY.getEntry(), pPos, pBlockState);
        if(pBlockState.getBlock() instanceof CustomRenderedDoorBlock block) {
            this.isSlideDoor = block.isSlideDoor();
            this.offset = block.getOffset();
            this.openoffset = block.getOpenOffset();
            Boolean value = pBlockState.getValue(DoorBlock.OPEN);
            if(value != null) {
                this.open = value;
            }
        }
    }

    public CustomRenderedDoorEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isLeftSide(BlockState state) {
        return state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT;
    }

    @Override
    public void tick() {
        if(level == null) return;
        if(this.getBlockState().getBlock() instanceof CustomRenderedDoorBlock) {
            BlockState state = level.getBlockState(this.getBlockPos());
            if (state.hasProperty(DoorBlock.OPEN)) {
                this.open = state.getValue(DoorBlock.OPEN);
            }
        }
    }

    public boolean isSlideDoor() {
        return isSlideDoor;
    }

    public boolean isOpen() {
        return open;
    }

    @OnlyIn(Dist.CLIENT)
    public Couple<PartialModel> getLeftModels(BlockState state) {
        return ((CustomRenderedDoorBlock) state.getBlock()).getLeftDoorModels();
    }

    @OnlyIn(Dist.CLIENT)
    public Couple<PartialModel> getRightModels(BlockState state) {
        return ((CustomRenderedDoorBlock) state.getBlock()).getRightDoorModels();
    }

    @OnlyIn(Dist.CLIENT)
    public Couple<PartialModel> getModels(BlockState state) {
        return isLeftSide(state) ? getLeftModels(state) : getRightModels(state);
    }

    public Vec3 getOffset() {
        return offset;
    }
    public Vec3 getOpenOffset() {
        return openoffset;
    }
    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public void update(StructureTemplate.StructureBlockInfo info, Player player, BlockPos pos, AbstractContraptionEntity entity) {
        this.open = !open;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return AABB.ofSize(Vec3.atCenterOf(this.getBlockPos()), 5, 5, 5);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }
}
