package cn.kycraft.kuayue;

import io.micronaut.context.ApplicationContext;
import lib.kasuga.KasugaLibRegistry;
import lib.kasuga.inject.ModApplicationContext;
import lib.kasuga.registration.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(KuaYue.MODID)
public class KuaYue {
    public static final ApplicationContext context = ModApplicationContext.create(KuaYue.class);
    public static final String MODID = "kuayue";

    public static final Registry REGISTRY = KasugaLibRegistry.getRegistryOf(MODID);
    public KuaYue(IEventBus modEventBus, ModContainer modContainer) {
        ModApplicationContext.init(context, modEventBus, modContainer);
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
