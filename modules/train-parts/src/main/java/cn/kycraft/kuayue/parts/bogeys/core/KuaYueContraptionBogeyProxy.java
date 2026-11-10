package cn.kycraft.kuayue.parts.bogeys.core;

import lib.kasuga.create.proxy.BogeyModels;
import lib.kasuga.create.proxy.ContraptionBogey;
import lib.kasuga.create.proxy.ContraptionBogeyProxy;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;

import java.util.List;

/** Train-mounted twin of {@link KuaYueBogeyProxyComposition}. */
public final class KuaYueContraptionBogeyProxy extends ContraptionBogeyProxy {

    @Override
    protected List<ModelPart> partsFor(
            ContraptionBogey element,
            BogeyModels.Placement placement
    ) {
        return KuaYueBogeyMotion.bind(
                element.sizeId(),
                placement.parts(),
                element::wheelAngle,
                true
        );
    }
}
