package cn.kycraft.kuayue;

import io.micronaut.context.ApplicationContext;
import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.KuaYueContentRegistrationProvider;
import cn.kycraft.kuayue.packs.network.PackSetConfigurationGate;
import lib.kasuga.KasugaLib;
import lib.kasuga.KasugaLibRegistry;
import lib.kasuga.inject.ModApplicationContext;
import lib.kasuga.core.resource.ResourceSystem;
import lib.kasuga.registration.Registry;
import lib.kasuga.registration.data_driven.DataDrivenRegistrationProviders;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(KuaYue.MODID)
public class KuaYue {
    public static final ApplicationContext context = ModApplicationContext.create(KuaYue.class);
    public static final String MODID = "kuayue";

    public static final Registry REGISTRY = KasugaLibRegistry.getRegistryOf(MODID);
    public KuaYue(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, PackSetConfigurationGate::registerPayloads);
        KuaYuePackBootstrap.initialize(FMLPaths.GAMEDIR.get(), modContainer, modEventBus);
        DataDrivenRegistrationProviders.install(
                MODID,
                KuaYueContentRegistrationProvider::build
        );
        ModApplicationContext.init(context, modEventBus, modContainer);
        modEventBus.addListener(AddPackFindersEvent.class, event ->
                KuaYuePackBootstrap.installResources(
                        KasugaLib.getBean(ResourceSystem.class)
                ));
        // Keep the setup hook for launch paths that do not build a PackRepository.
        modEventBus.addListener(FMLCommonSetupEvent.class, event ->
                event.enqueueWork(() -> KuaYuePackBootstrap.installResources(
                        KasugaLib.getBean(ResourceSystem.class)
                )));
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
