package cn.kycraft.kuayue.parts.core.panel.door;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;

/** Create's automatic door control for passenger boarding doors, without vanilla door halves. */
public final class TrainDoorMovementBehaviour extends SlidingDoorMovementBehaviour {
    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide()) {
            return;
        }
        var info = context.contraption.getBlocks().get(context.localPos);
        // Also reject stale actor callbacks captured before the registration scope was narrowed.
        if (info == null || !TrainDoorContraptionModule.hasAutomaticDoorControl(info.state().getBlock())) {
            return;
        }
        var door = (OpenableTrainDoor) info.state().getBlock();
        if (!info.state().hasProperty(door.doorOpenProperty())) {
            return;
        }
        boolean shouldOpen = shouldOpen(context);
        if (shouldUpdate(context, shouldOpen)) {
            TrainDoorContraptionState.setOpen(context.contraption.entity, context.localPos, shouldOpen, null);
        }
    }
}
