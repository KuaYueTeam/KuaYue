package cn.kycraft.kuayue.parts.tracks;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.tracks.data.TrackTypeHandler;
import com.simibubi.create.AllBlocks;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.registration.data_driven.TypeHandlerRegistry;
import lib.kasuga.registration.minecraft.creative_tab.CreativeTabReg;
import net.minecraft.network.chat.Component;

/**
 * Registers KuaYue's track data-driven handler and owns the creative tab used by track items.
 *
 * <p>The handler is registered while the Micronaut context is initialized. Kasuga's JSON loader
 * resolves handlers later, when the mod registry is dispatched, so the track definition is
 * available during the normal data-driven registration pass.</p>
 */
@Context
public final class TrackDataDrivenModule {

    /** Creative tab receiving data-driven track items that do not override {@code item_properties.tab}. */
    public static final CreativeTabReg KUAYUE_TEMP_TAB = new CreativeTabReg("kuayue_temp_tab")
            .title(Component.literal("Kuayue Test"))
            .icon(() -> AllBlocks.RAILWAY_CASING.asStack())
            .displayItems((params, output) -> {
            })
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    /**
     * Registers the {@code tracks} handler before Kasuga processes the mod's JSON index.
     */
    @PostConstruct
    public void registerHandler() {
        TypeHandlerRegistry.register(new TrackTypeHandler());
    }
}
