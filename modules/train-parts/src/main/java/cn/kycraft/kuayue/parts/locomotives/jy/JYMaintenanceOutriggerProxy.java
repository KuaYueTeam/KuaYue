package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlockEntity;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelInput;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;

/** Java-side state and playback policy for the JY maintenance outrigger. */
final class JYMaintenanceOutriggerProxy {
    private JYMaintenanceOutriggerProxy() {
    }

    static ProxyComposition<TrainPanelProxyBlockEntity, ProxyModelInput, Void, Void, Void> create(
            TrainPanelProxyBlockEntity element
    ) {
        return JYMaintenanceBooleanClipProxy.create(
                element,
                JYMaintenanceOutriggerBlock.EXTENDED,
                "outtrigger_on",
                "outrigger"
        );
    }
}
