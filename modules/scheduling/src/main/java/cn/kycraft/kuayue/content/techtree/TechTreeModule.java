package cn.kycraft.kuayue.content.techtree;

import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lib.kasuga.registration.Registry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Tech tree bootstrap.
 *
 * <p>Order matters: the table (the tree's only entry point), the RPC endpoints and the player
 * attachment are registered before the first resource reload resolves any {@code tech_tree} json.</p>
 */
@Context
public class TechTreeModule {

    @Inject
    @Named("modEventBus")
    IEventBus modEventBus;

    @PostConstruct
    public void init() {
        TechTreeAttachments.init(modEventBus);
        TechTreeRpc.register(TechTreeSystem.REGISTRY);
        BlueprintTableRegistry.init();
        TechTreeRegistryRoot.attach(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.register(AdvancementProgressListener.class);
        TechTreeSystem.LOGGER.info("KuaYue tech tree ready.");
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(TechTreeManager.get());
    }

    /** Exposes the registry root so the main mod can hang it under its own tree if it wants to. */
    static Registry registry() {
        return TechTreeSystem.REGISTRY;
    }
}
