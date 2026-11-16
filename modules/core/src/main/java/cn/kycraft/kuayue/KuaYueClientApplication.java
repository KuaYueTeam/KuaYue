package cn.kycraft.kuayue;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import cn.kycraft.kuayue.client.settings.SettingsClientEntrypoints;
import lib.kasuga.inject.class_loader.BeanOnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

@Context()
@BeanOnlyIn.Client()
public class KuaYueClientApplication {
    @Inject() @Named("modEventBus")
    IEventBus modEventBus;
    @Inject()
    ModContainer modContainer;
    @PostConstruct
    public void init() {
        KuaYue.REGISTRY.registerClient(modEventBus);
        SettingsClientEntrypoints.install(modContainer);
    }
}
