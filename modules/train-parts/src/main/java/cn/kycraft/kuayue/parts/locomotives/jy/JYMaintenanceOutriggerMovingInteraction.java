package cn.kycraft.kuayue.parts.locomotives.jy;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.SimpleBlockMovingInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Independent outrigger operation using Create's authoritative local state and native packet. */
public final class JYMaintenanceOutriggerMovingInteraction extends SimpleBlockMovingInteraction {
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
        if (info == null || !supports(info.state())) {
            return false;
        }
        if (entity.level().isClientSide()) {
            return true;
        }
        return super.handlePlayerInteraction(player, hand, localPos, entity);
    }

    @Override
    protected BlockState handle(Player player, Contraption contraption, BlockPos pos, BlockState state) {
        return supports(state) ? state.cycle(JYMaintenanceOutriggerBlock.EXTENDED) : state;
    }

    private static boolean supports(BlockState state) {
        return state.getBlock() instanceof JYMaintenanceOutriggerBlock
                && state.hasProperty(JYMaintenanceOutriggerBlock.EXTENDED);
    }

    // The outrigger's collision remains a full cube in both states, so the server cache stays valid.
}
