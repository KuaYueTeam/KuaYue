package cn.kycraft.kuayue.parts.locomotives.jy.client;

import cn.kycraft.kuayue.KuaYue;
import lib.kasuga.rendering.models.mc.api.item.ItemModelRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.Map;

/** Renders JY maintenance proxy-backed block items with their native pipeline models. */
@EventBusSubscriber(modid = KuaYue.MODID, value = Dist.CLIENT)
public final class JYMaintenanceItemClientExtensions {
    private static final Map<ResourceLocation, Preview> MODELS = Map.ofEntries(
            entry("jy_maintenance_head", "jy_maintenance_head.bbmodel", 0.30f),
            entry("jy_maintenance_carport", "jy_maintenance_carport.bbmodel", 1.0f),
            entry("jy_maintenance_console", "jy_maintenance_console.bbmodel", 1.0f),
            entry("jy_maintenance_outrigger", "jy_maintenance_outtrigger_left.bbmodel", 1.0f),
            entry("jy_maintenance_endface", "jy_maintenance_endface.bbmodel", 0.65f),
            entry("jy_maintenance_door", "jy_maintenance_door_left.bbmodel", 0.72f),
            entry("jy_maintenance_panel", "jy_maintenance_panel_left.bbmodel", 0.72f),
            entry("jy_maintenance_tail", "jy_maintenance_tail.bbmodel", 1.0f)
    );

    private JYMaintenanceItemClientExtensions() {
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        JYMaintenanceModelLighting.registerDefaults();
        MODELS.forEach((itemId, preview) -> {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) {
                return;
            }
            ItemModelRenderers.register(item, preview.model(), null, preview.guiScale());
            event.registerItem(ItemModelRenderers.clientExtensions(), item);
        });
    }

    private static Map.Entry<ResourceLocation, Preview> entry(
            String itemPath,
            String modelFile,
            float guiScale
    ) {
        return Map.entry(
                ResourceLocation.fromNamespaceAndPath("kuayue", itemPath),
                new Preview(
                        ResourceLocation.fromNamespaceAndPath(
                                "kuayue",
                                "models/block/jy_maintenance/" + modelFile
                        ),
                        guiScale
                )
        );
    }

    private record Preview(ResourceLocation model, float guiScale) {
    }
}
