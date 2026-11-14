package cn.kycraft.kuayue;

import io.micronaut.context.ApplicationContext;
import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.KuaYueContentRegistrationProvider;
import lib.kasuga.KasugaLib;
import lib.kasuga.KasugaLibRegistry;
import lib.kasuga.inject.ModApplicationContext;
import lib.kasuga.core.resource.ResourceSystem;
import lib.kasuga.registration.Registry;
import lib.kasuga.registration.data_driven.DataDrivenRegistrationProviders;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(KuaYue.MODID)
public class KuaYue {
    public static final ApplicationContext context = ModApplicationContext.create(KuaYue.class);
    public static final String MODID = "kuayue";

    public static final Registry REGISTRY = KasugaLibRegistry.getRegistryOf(MODID);
    public KuaYue(IEventBus modEventBus, ModContainer modContainer) {
        KuaYuePackBootstrap.initialize(FMLPaths.GAMEDIR.get(), modContainer, modEventBus);
        DataDrivenRegistrationProviders.install(
                MODID,
                KuaYueContentRegistrationProvider::build
        );
        ModApplicationContext.init(context, modEventBus, modContainer);
        KasugaLib.afterRunning(
                modEventBus,
                () -> KuaYuePackBootstrap.installResources(
                        KasugaLib.getBean(ResourceSystem.class)
                )
        );
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
