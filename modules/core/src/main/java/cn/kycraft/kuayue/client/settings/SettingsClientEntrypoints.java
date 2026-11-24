package cn.kycraft.kuayue.client.settings;

import cn.kycraft.kuayue.client.creative.CreativeFilterClientEvents;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only registration points for KuaYue settings screens. */
public final class SettingsClientEntrypoints {
    private SettingsClientEntrypoints() {
    }

    public static void install(ModContainer container) {
        CreativeFilterClientEvents.install();
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (modContainer, parent) ->
                        KuaYueSettingsScreens.mainScreen(parent)
        );
    }
}
