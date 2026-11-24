package cn.kycraft.kuayue.parts.core.panel.proxy;

import lib.kasuga.rendering.models.mc.proxies.ProxyModelBlockEntity;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelBlockFactories;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelInput;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelProxy;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** KuaYue-owned proxy-model BE for TrainPanel-compatible block behavior. */
public final class TrainPanelProxyBlockEntity extends ProxyModelBlockEntity {
    public TrainPanelProxyBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            ProxyModelBlockFactories.ModelSettings settings
    ) {
        super(
                type,
                pos,
                state,
                settings.selection(),
                settings.yawOffset(),
                settings.facingYaw(),
                settings.lightingMode()
        );
    }

    public ProxyComposition<TrainPanelProxyBlockEntity, ProxyModelInput, Void, Void, Void>
    createComposition() {
        if (getBlockState().getBlock() instanceof TrainPanelProxyBlock block) {
            return block.createProxyComposition(this);
        }
        return ProxyModelProxy.defaultComposition(this);
    }
}
