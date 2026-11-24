package cn.kycraft.kuayue.parts.core.multiblock;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Forwards one validated local click to the parent's captured behaviour, without recursion. */
public final class MultiCellMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(
            Player player,
            InteractionHand hand,
            BlockPos localPos,
            AbstractContraptionEntity entity
    ) {
        var contraption = entity.getContraption();
        if (contraption == null) {
            return false;
        }
        BlockPos parentPos = MultiCellContraptionParent.resolve(contraption, localPos);
        if (parentPos == null) {
            return false;
        }
        MovingInteractionBehaviour parent = contraption.getInteractors().get(parentPos);
        var parentInfo = contraption.getBlocks().get(parentPos);
        if (parent == null || parent == this
                || parent != REGISTRY.get(parentInfo.state().getBlock())) {
            return false;
        }
        return parent.handlePlayerInteraction(player, hand, parentPos, entity);
    }
}
