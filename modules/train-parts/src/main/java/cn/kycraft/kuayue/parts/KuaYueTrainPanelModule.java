package cn.kycraft.kuayue.parts;

import cn.kycraft.kuayue.KuaYue;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.Registry;
import lib.kasuga.registration.RegistryGroup;

@Context
public class KuaYueTrainPanelModule {
    /** Always-registered infrastructure shared by runtime content and preset packs. */
    public static final RegistryGroup RUNTIME_REGISTRY_GROUP = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);
}
