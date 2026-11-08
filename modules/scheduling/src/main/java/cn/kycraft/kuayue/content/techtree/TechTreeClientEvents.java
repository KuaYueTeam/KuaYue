package cn.kycraft.kuayue.content.techtree;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;

/** Client glue: keeps the tech tree snapshot fresh and drops the board when leaving the world. */
@EventBusSubscriber(modid = "kuayue", value = Dist.CLIENT)
public final class TechTreeClientEvents {

    private static int tickCounter;

    private TechTreeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientPlayerNetworkEvent.LoggingIn event) {
        tickCounter = 0;
    }

    /** Throttled snapshot refresh: keeps the board correct for visibility changed while opened. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (event.getEntity() != net.minecraft.client.Minecraft.getInstance().player) return;
        if (++tickCounter % 40 != 0) return;
        if (TechTreeBoard.isUiOpen()) {
            TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(false))
                    .thenAccept(ClientTreeState::apply);
        }
    }
}
