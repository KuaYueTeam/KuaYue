package cn.kycraft.kuayue.parts;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.carriages.c25.C25TPanel;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.Registry;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.creative_tab.CreativeTabReg;
import net.minecraft.network.chat.Component;

@Context
public class KuaYueTrainPanelModule {
    public static RegistryGroup REGISTRY_GROUP = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    public static final CreativeTabReg TRAIN_PANEL_TAB = new CreativeTabReg("train_panel_tab")
            .title(Component.translatable("train_panel_tab")).icon(()->C25TPanel.PANEL_BOTTOM_25T.getItem().getDefaultInstance())
            .setParent(REGISTRY_GROUP);
}
