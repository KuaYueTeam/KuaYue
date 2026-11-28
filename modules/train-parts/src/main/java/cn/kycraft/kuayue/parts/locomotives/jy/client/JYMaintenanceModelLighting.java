package cn.kycraft.kuayue.parts.locomotives.jy.client;

import lib.kasuga.rendering.models.mc.api.ModelLightingDefaults;
import lib.kasuga.rendering.models.uml.backend.ModelLightingMode;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Presentation policy shared by JY's world, assembled-train and item model instances. */
public final class JYMaintenanceModelLighting {
    private static final List<String> MODELS = List.of(
            "jy_maintenance_head",
            "jy_maintenance_carport",
            "jy_maintenance_console",
            "jy_maintenance_outtrigger_left",
            "jy_maintenance_outtrigger_right",
            "jy_maintenance_endface",
            "jy_maintenance_door_left",
            "jy_maintenance_door_right",
            "jy_maintenance_panel_left",
            "jy_maintenance_panel_right",
            "jy_maintenance_tail"
    );

    private JYMaintenanceModelLighting() {
    }

    public static void registerDefaults() {
        for (String model : MODELS) {
            ModelLightingDefaults.register(ResourceLocation.fromNamespaceAndPath(
                    "kuayue", "models/block/jy_maintenance/" + model + ".bbmodel"),
                    ModelLightingMode.BLOCK);
        }
    }
}
