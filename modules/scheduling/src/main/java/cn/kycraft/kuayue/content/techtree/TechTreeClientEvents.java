package cn.kycraft.kuayue.content.techtree;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;

/** Client glue: keeps the tech tree snapshot fresh, paints the projected slots and drops the board on exit. */
@EventBusSubscriber(modid = "kuayue", value = Dist.CLIENT)
public final class TechTreeClientEvents {

    private static int tickCounter;
    private static boolean registered;

    private TechTreeClientEvents() {
    }

    /**
     * Registers the world-render listener directly on the game bus.
     *
     * <p>The annotation-based subscription above is not reliable for this module: the class ships in its own
     * jar, and when that jar is not part of the mod container's event scan nothing fires - which is why the
     * table slots and the blueprint model were never drawn while the Kasuga UI sheet still was. Registering
     * by hand removes the dependency on that scan.</p>
     */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TechTreeClientEvents::onRenderLevel);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TechTreeClientEvents::onPlayerTick);
        TechTreeSystem.LOGGER.info("Tech tree client render hooks registered on the game bus");
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        tickCounter = 0;
        // Warm the tree cache on join: the menu UI renders from ClientTreeState, and without a snapshot it
        // has no groups to lay out, so the table GUI would open as an empty board.
        TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(false))
                .thenAccept(snapshot -> ClientTreeState.apply(snapshot));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TechTreeBoard.close();
        TechTreeScreen.clearMenuFocus();
        CarModelUiDemo.closeAll();
    }

    /**
     * Paints the projected slots of an open blueprint table.
     *
     * <p>{@code AFTER_ENTITIES} is past the model pass, so the slots land on the table and not under it, and
     * the world board itself is painted later by KasugaLibNext's world UI pipeline.</p>
     */
    private static int stageLogs;

    public static void onRenderLevel(RenderLevelStageEvent event) {
        // One-off diagnostic: proves the listener is reached and shows which stages actually fire.
        if (stageLogs < 8) {
            stageLogs++;
            TechTreeSystem.LOGGER.info("Blueprint render hook reached stage {}", event.getStage());
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

    }

    /** Throttled snapshot refresh: keeps the board correct for visibility changed while opened. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (event.getEntity() != Minecraft.getInstance().player) return;

        if (++tickCounter % 40 != 0) return;
        if (TechTreeBoard.isUiOpen()) {
            TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(false))
                    .thenAccept(snapshot -> {
                        ClientTreeState.apply(snapshot);
                        Minecraft.getInstance().execute(TechTreeBoard::refresh);
                    });
        }
    }
}
