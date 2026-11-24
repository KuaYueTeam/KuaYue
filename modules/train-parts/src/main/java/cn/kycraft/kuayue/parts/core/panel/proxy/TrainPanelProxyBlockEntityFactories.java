package cn.kycraft.kuayue.parts.core.panel.proxy;

import com.google.gson.JsonObject;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.factory.FactoryRegistry;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import lib.kasuga.rendering.models.mc.proxies.BlockEntityProxyDefinitions;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelBlockFactories;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

@Context
public final class TrainPanelProxyBlockEntityFactories {
    public static final String BE_TYPE = "train_panel_proxy_be";

    @PostConstruct
    public void init() {
        FactoryRegistry.registerBlockEntity(BE_TYPE, TrainPanelProxyBlockEntityFactories::create);
    }

    private static Reg<?, ?> create(
            String id,
            Supplier<Block[]> validBlocks,
            @Nullable JsonObject params
    ) {
        ProxyModelBlockFactories.ModelSettings settings =
                ProxyModelBlockFactories.parseModelSettings(params);
        BlockEntityReg<TrainPanelProxyBlockEntity> registration =
                new BlockEntityReg<>(
                        id,
                        reg -> (pos, state) -> new TrainPanelProxyBlockEntity(
                                reg.getEntry(),
                                pos,
                                state,
                                settings
                        )
                );
        registration.withProperty(Collection.class, values -> {
            values.addAll(Arrays.asList(validBlocks.get()));
            return values;
        });
        registration.onRegistered(type -> {
            for (Block block : validBlocks.get()) {
                settings.selection().validate(block.defaultBlockState());
            }
            BlockEntityProxyDefinitions.register(
                    type,
                    registration.getResourceLocation(),
                    TrainPanelProxyBlockEntity::createComposition
            );
        });
        return registration;
    }
}
