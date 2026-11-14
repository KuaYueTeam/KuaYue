package cn.kycraft.kuayue.content.techtree;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/**
 * When an advancement is earned, re-evaluate the achievement-gated tech tree views and push the
 * refreshed snapshot to the player. This is the client-visible half of the achievement unlock branch.
 */
public final class AdvancementProgressListener {

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UnlockState state = PlayerProgress.of(player);
        if (state == null) return;
        if (Visibility.refresh(player, state)) {
            TechTreePushes.send(player);
        }
    }
}
