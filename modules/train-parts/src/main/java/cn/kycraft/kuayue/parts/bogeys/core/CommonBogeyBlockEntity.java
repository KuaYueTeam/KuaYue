package cn.kycraft.kuayue.parts.bogeys.core;

import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import lib.kasuga.rendering.models.mc.proxies.ProxySessionHost;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyAttachToken;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxySession;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CommonBogeyBlockEntity extends AbstractBogeyBlockEntity
        implements ProxySessionHost {

    private final ProxyAttachToken attachToken = ProxyAttachToken.random();
    @Nullable private ProxySession<?, ?, ?, ?, ?> proxySession;

    public CommonBogeyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CommonBogeyBlockEntity(BlockPos pos, BlockState state) {
        this(CommonBogeyBlock.GLOBAL_BE_TYPE.get(), pos, state);
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        if (getBlockState().getBlock() instanceof CommonBogeyBlock block)
            return block.getDefaultStyle();
        return AllBogeyStyles.STANDARD;
    }

    @Override
    public ProxyAttachToken proxyAttachToken() {
        return attachToken;
    }

    @Override
    public @Nullable ProxySession<?, ?, ?, ?, ?> proxySession() {
        return proxySession;
    }

    @Override
    public void proxySession(@Nullable ProxySession<?, ?, ?, ?, ?> session) {
        if (session != null && proxySession != null && proxySession != session) {
            throw new IllegalStateException("KuaYue bogey already has a proxy session");
        }
        proxySession = session;
    }

    @Override
    public void setRemoved() {
        closeProxySession();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        closeProxySession();
        super.onChunkUnloaded();
    }
}
