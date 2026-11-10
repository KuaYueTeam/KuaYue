package cn.kycraft.kuayue.parts.bogeys.core;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lib.kasuga.KasugaLib;
import lib.kasuga.create.CreateModBeans;
import lib.kasuga.create.client.registration.ClientBogeyRendererRegistry;
import lib.kasuga.create.client.rendering.ProxyBogeyVisual;
import lib.kasuga.inject.class_loader.BeanOnlyIn;
import net.neoforged.bus.api.IEventBus;

/** Registers KuaYue's shared proxy visualizer with Create on the client. */
@Context
@CreateModBeans
@BeanOnlyIn.Client
public final class KuaYueBogeyClient {

    @Inject
    @Named("modEventBus")
    IEventBus modEventBus;

    @PostConstruct
    void init() {
        // KuaYue starts its context before KasugaLib, so resolve the library-owned registry lazily.
        KasugaLib.afterRunning(modEventBus, this::registerRenderers);
    }

    private void registerRenderers() {
        ClientBogeyRendererRegistry renderers =
                KasugaLib.getBean(ClientBogeyRendererRegistry.class);
        KuaYueBogeyRenderers.install(binding -> register(renderers, binding));
    }

    private void register(
            ClientBogeyRendererRegistry renderers,
            KuaYueBogeyRenderers.Binding binding
    ) {
        renderers.register(
                binding.handle(),
                ProxyBogeyVisual.NoopRenderer::new,
                (context, partialTick, inContraption) -> new ProxyBogeyVisual(
                        context,
                        binding.size().getEntry(),
                        new KuaYueContraptionBogeyProxy()
                )
        );
    }
}
