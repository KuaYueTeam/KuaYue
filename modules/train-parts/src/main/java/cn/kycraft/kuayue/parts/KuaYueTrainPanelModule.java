package cn.kycraft.kuayue.parts;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.Registry;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.creative_tab.CreativeTabReg;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

@Context
public class KuaYueTrainPanelModule {
    /** Always-registered infrastructure that must survive corepack being disabled. */
    public static final RegistryGroup RUNTIME_REGISTRY_GROUP = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    public static RegistryGroup REGISTRY_GROUP = new RegistryGroup()
            .dispatchWhen(PackRuntimeState::corepackEnabled)
            .setParent(RUNTIME_REGISTRY_GROUP);

    public static final CreativeTabReg TRAIN_PANEL_TAB = new CreativeTabReg("train_panel_tab")
            .title(Component.translatable("train_panel_tab")).icon(() -> Items.IRON_INGOT.getDefaultInstance())
            .setParent(RUNTIME_REGISTRY_GROUP);
}
