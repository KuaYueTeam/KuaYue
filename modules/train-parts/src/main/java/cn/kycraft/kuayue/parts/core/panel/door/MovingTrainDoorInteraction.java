package cn.kycraft.kuayue.parts.core.panel.door;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Uses Create's native state packet and collider invalidation; the client only acknowledges. */
public final class MovingTrainDoorInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(
            Player player,
            InteractionHand hand,
            BlockPos localPos,
            AbstractContraptionEntity entity
    ) {
        Contraption contraption = entity.getContraption();
        if (contraption == null) {
            return false;
        }
        var info = contraption.getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof OpenableTrainDoor door)
                || !door.hasDoor()
                || !info.state().hasProperty(door.doorOpenProperty())) {
            return false;
        }
        if (entity.level().isClientSide()) {
            return true;
        }
        return TrainDoorContraptionState.setOpen(entity, localPos,
                !info.state().getValue(door.doorOpenProperty()), player);
    }
}
