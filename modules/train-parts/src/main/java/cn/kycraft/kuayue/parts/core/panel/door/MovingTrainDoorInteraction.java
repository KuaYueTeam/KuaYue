package cn.kycraft.kuayue.parts.core.panel.door;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.SimpleBlockMovingInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** Uses Create's native state packet and collider invalidation; the client only acknowledges. */
public final class MovingTrainDoorInteraction extends SimpleBlockMovingInteraction {
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
                || !info.state().hasProperty(door.doorOpenProperty())) {
            return false;
        }
        if (entity.level().isClientSide()) {
            return true;
        }
        if (!super.handlePlayerInteraction(player, hand, localPos, entity)) {
            return false;
        }
        BlockState updated = contraption.getBlocks().get(localPos).state();
        boolean open = updated.getValue(door.doorOpenProperty());
        BlockPos soundPos = BlockPos.containing(entity.toGlobalVector(Vec3.atCenterOf(localPos), 1f));
        entity.level().levelEvent(null,
                open ? TrainDoorBlock.getOpenSound(updated) : TrainDoorBlock.getCloseSound(updated),
                soundPos, 0);
        entity.level().gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, soundPos);
        return true;
    }

    @Override
    protected BlockState handle(Player player, Contraption contraption, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof OpenableTrainDoor door
                ? state.cycle(door.doorOpenProperty()) : state;
    }

    @Override
    protected boolean updateColliders() {
        return true;
    }
}
