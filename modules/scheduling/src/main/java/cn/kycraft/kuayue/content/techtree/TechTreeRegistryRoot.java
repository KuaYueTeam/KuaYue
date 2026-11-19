package cn.kycraft.kuayue.content.techtree;

import net.neoforged.bus.api.IEventBus;

/** Small indirection so the registry root is registered exactly once, from the module bootstrap. */
final class TechTreeRegistryRoot {

    private static boolean registered;

    private TechTreeRegistryRoot() {
    }

    static synchronized void attach(IEventBus modEventBus) {
        if (registered) return;
        registered = true;
        TechTreeSystem.REGISTRY.register(modEventBus);
    }
}
