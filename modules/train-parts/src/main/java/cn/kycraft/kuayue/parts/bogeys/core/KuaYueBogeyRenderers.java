package cn.kycraft.kuayue.parts.bogeys.core;

import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.create.registration.bogey.BogeyRendererHandle;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Registry of per-size client renderer handles used by data-driven KuaYue bogeys. */
public final class KuaYueBogeyRenderers {
    private static final Map<ResourceLocation, Binding> BINDINGS = new LinkedHashMap<>();
    private static Consumer<Binding> registrationSink;

    private KuaYueBogeyRenderers() {
    }

    /**
     * Creates or returns the renderer binding for one stable bogey size ID.
     */
    public static synchronized Binding bind(ResourceLocation sizeId, BogeySizeReg size) {
        Binding existing = BINDINGS.get(sizeId);
        if (existing != null) {
            return existing;
        }
        ResourceLocation rendererId = ResourceLocation.fromNamespaceAndPath(
                sizeId.getNamespace(),
                "proxy/" + sizeId.getPath()
        );
        Binding binding = new Binding(
                BogeyRendererHandle.of(rendererId),
                size
        );
        BINDINGS.put(sizeId, binding);
        if (registrationSink != null) {
            registrationSink.accept(binding);
        }
        return binding;
    }

    /**
     * Installs the client registration sink and registers all bindings created so far.
     */
    public static synchronized void install(Consumer<Binding> sink) {
        if (registrationSink != null) {
            throw new IllegalStateException("Bogey renderer registration sink is already installed");
        }
        registrationSink = sink;
        BINDINGS.values().forEach(sink);
    }

    /**
     * A renderer handle paired with the Create size used by the client proxy visual.
     */
    public record Binding(BogeyRendererHandle handle, BogeySizeReg size) {
    }
}
