package cn.kycraft.kuayue.packs.bootstrap;

import lib.kasuga.core.resource.lifecycle.LogicalSide;
import lib.kasuga.core.resource.lifecycle.ResourceEvent;
import lib.kasuga.core.resource.pack.OrderedResourcePackGroup;
import lib.kasuga.core.resource.routing.ResourceRouting;
import lib.kasuga.core.resource.source.ResourceSource;
import lib.kasuga.core.resource.source.ResourceSourceDescriptor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Adapts frozen KuaYue roots to one ordered Minecraft resource-pack group. */
final class KuaYueMcPackProjection {
    private static final String GROUP_ID = "kuayue-managed";

    private KuaYueMcPackProjection() {
    }

    static OrderedResourcePackGroup create(List<ResourceSource> sources) {
        List<OrderedResourcePackGroup.Layer> layers = new ArrayList<>(sources.size());
        for (ResourceSource source : sources) {
            ResourceSourceDescriptor descriptor = source.descriptor();
            ResourceRouting routing = new ResourceRouting(List.of(
                    new ResourceRouting.Binding(
                            "binding-" + descriptor.rootId(),
                            descriptor.sourceId(),
                            "",
                            List.of(new ResourceRouting.Output(
                                    "minecraft",
                                    ResourceRouting.MINECRAFT_ENTRY_POINT,
                                    ResourceEvent.RESOURCE_RELOAD,
                                    LogicalSide.BOTH,
                                    ""
                            ))
                    )
            ));
            layers.add(new OrderedResourcePackGroup.Layer(
                    registrationId(descriptor),
                    source,
                    routing
            ));
        }
        // Keep the pre-group registration placement; D2 relative pack policy remains unchanged.
        OrderedResourcePackGroup.Placement placement = new OrderedResourcePackGroup.Placement(
                Pack.Position.TOP,
                true
        );
        return new OrderedResourcePackGroup(
                GROUP_ID,
                Component.literal("KuaYue managed resources"),
                List.copyOf(layers),
                Map.of(
                        PackType.CLIENT_RESOURCES,
                        placement,
                        PackType.SERVER_DATA,
                        placement
                )
        );
    }

    static String childPackId(ResourceSourceDescriptor descriptor) {
        return "kasuga/" + GROUP_ID + "/" + descriptor.sourceId() + "/" + descriptor.rootId();
    }

    private static String registrationId(ResourceSourceDescriptor descriptor) {
        return descriptor.sourceId() + "-" + descriptor.rootId();
    }
}
