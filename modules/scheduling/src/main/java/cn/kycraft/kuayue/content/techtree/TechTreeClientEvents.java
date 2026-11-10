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

    private TechTreeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        tickCounter = 0;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TechTreeBoard.close();
        TechTreeScreen.clearMenuFocus();
    }

    /**
     * Paints the projected slots of an open blueprint table.
     *
     * <p>{@code AFTER_ENTITIES} is past the model pass, so the slots land on the table and not under it, and
     * the world board itself is painted later by KasugaLibNext's world UI pipeline.</p>
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        TechTreeTableMenu menu = TechTreeTableMenu.of(mc.player);
        if (menu == null || mc.screen != null) return;
        if (!(mc.player.containerMenu instanceof TechTreeTableMenu)) return;
        if (!(menu.uiHolder instanceof com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUIHolder holder)) return;
        if (!(mc.level.getBlockEntity(holder.pos) instanceof BlueprintTableBlockEntity table)) return;
        var stack = new com.mojang.blaze3d.vertex.PoseStack();
        stack.mulPose(event.getModelViewMatrix());
        var gui = lib.kasuga.rendering.models.mc.backend.ui.UIBackend.constructGuiGraphics(stack);
        TableSlotProjection.render(table, menu, gui,
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    /** Throttled snapshot refresh: keeps the board correct for visibility changed while opened. */
    @SubscribeEvent
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
