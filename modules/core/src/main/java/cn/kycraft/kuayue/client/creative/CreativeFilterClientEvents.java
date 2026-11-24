package cn.kycraft.kuayue.client.creative;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Registers the creative vehicle selector on client-only event paths. */
public final class CreativeFilterClientEvents {
    private static boolean installed;

    private CreativeFilterClientEvents() {
    }

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(CreativeFilterClientEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(CreativeFilterClientEvents::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(CreativeFilterClientEvents::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(CreativeFilterClientEvents::onLoggingOut);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;
        CreativeFilterController.refreshCatalog();
        CreativeFilterController.attach(screen, event::addListener);
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen screen) {
            CreativeFilterController.detach(screen);
        }
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        CreativeFilterController.onTagsReady();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeFilterController.disconnect();
    }
}
