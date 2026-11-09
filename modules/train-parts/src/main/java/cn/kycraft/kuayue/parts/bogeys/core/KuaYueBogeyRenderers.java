package cn.kycraft.kuayue.parts.bogeys.core;

import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.create.registration.bogey.BogeyRendererHandle;
import net.minecraft.resources.ResourceLocation;

/** Client renderer handles for KuaYue 2.0 bogey sizes. */
public final class KuaYueBogeyRenderers {

    private KuaYueBogeyRenderers() {
    }

    public static Binding of(ResourceLocation sizeId, BogeySizeReg size) {
        ResourceLocation rendererId = ResourceLocation.fromNamespaceAndPath(
                sizeId.getNamespace(),
                "proxy/" + sizeId.getPath()
        );
        return new Binding(
                BogeyRendererHandle.of(rendererId),
                size
        );
    }

    public record Binding(BogeyRendererHandle handle, BogeySizeReg size) {
    }
}
