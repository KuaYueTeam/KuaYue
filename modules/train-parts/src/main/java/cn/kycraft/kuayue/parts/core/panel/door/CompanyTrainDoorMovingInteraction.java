package cn.kycraft.kuayue.parts.core.panel.door;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Legacy door and endface company cells forward one click to their captured primary door. */
final class CompanyTrainDoorMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity entity) {
        var contraption = entity.getContraption();
        if (contraption == null) {
            return false;
        }
        BlockPos parentPos = TrainDoorContraptionState.companyParent(contraption, localPos);
        if (parentPos == null) {
            return false;
        }
        var parent = contraption.getInteractors().get(parentPos);
        var info = contraption.getBlocks().get(parentPos);
        return parent != null && parent != this && parent == REGISTRY.get(info.state().getBlock())
                && parent.handlePlayerInteraction(player, hand, parentPos, entity);
    }
}
