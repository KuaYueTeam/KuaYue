package cn.kycraft.kuayue.parts.running_gear;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.minecraft.creative_tab.CreativeTabReg;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * Creative tabs supplied by the required running-gear preset.
 */
@Context
public class KuaYueRunningGearCreativeTabs {
    public static final CreativeTabReg CARRIAGE_TAB =
            tab("kuayue_carriage", "itemGroup.kuayue_carriage");

    public static final CreativeTabReg LOCO_TAB =
            tab("kuayue_loco", "itemGroup.kuayue_loco");

    private static CreativeTabReg tab(String id, String titleKey) {
        return new CreativeTabReg(id)
                .title(Component.translatable(titleKey))
                .icon(() -> Items.MINECART.getDefaultInstance())
                .setParent(KuaYueTrainPanelModule.RUNTIME_REGISTRY_GROUP);
    }
}
