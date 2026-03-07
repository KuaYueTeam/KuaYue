package cn.kycraft.kuayue;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lib.kasuga.inject.class_loader.BeanOnlyIn;
import net.neoforged.bus.api.IEventBus;

@Context()
@BeanOnlyIn.Client()
public class KuaYueClientApplication {
    @Inject() @Named("modEventBus")
    IEventBus modEventBus;
    @PostConstruct
    public void init() {
        KuaYue.REGISTRY.registerClient(modEventBus);
    }
}
