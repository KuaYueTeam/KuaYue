package cn.kycraft.kuayue.parts.carriages.c25k.client;

import cn.kycraft.kuayue.KuaYue;
import lib.kasuga.rendering.models.mc.api.ModelLightingDefaults;
import lib.kasuga.rendering.models.mc.api.item.ItemModelRenderers;
import lib.kasuga.rendering.models.uml.backend.ModelLightingMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.List;

/** Renders reworked 25K block items with their native BBModel pipeline models. */
@EventBusSubscriber(modid = KuaYue.MODID, value = Dist.CLIENT)
public final class C25KItemClientExtensions {
    private record Preview(String name, String modelName, float guiScale) {}

    private static final List<Preview> PREVIEWS = List.of(
            new Preview("new_panel_middle_25k", "panel_middle_25k", 0.842000f),  // extent 19.0 px
            new Preview("new_floor_25k", "floor_25k", 0.941000f),  // extent 17.0 px
            new Preview("new_floor_25k_2", "floor_25k_2", 1.000000f),  // extent 16.0 px
            new Preview("new_panel_bottom_25k", "panel_bottom_25k", 1.000000f),  // extent 16.0 px
            new Preview("new_panel_bottom_line_25k", "panel_bottom_line_25k", 1.000000f),  // extent 16.0 px
            new Preview("new_panel_bottom_line_25k_lh", "panel_bottom_line_25k_left", 1.000000f),  // extent 16.0 px
            new Preview("new_panel_bottom_25k_3", "panel_bottom_25k_3", 1.000000f),  // extent 16.0 px
            new Preview("new_panel_top_25k", "panel_top_25k", 1.000000f),  // extent 16.0 px
            new Preview("new_window_oc_sealed_small_25k", "window_oc_sealed_small_25k", 0.500000f),  // extent 32.0 px
            new Preview("new_window_oc_wide_sealed_25k", "window_oc_wide_sealed_25k", 1.000000f),  // extent 16.0 px
            new Preview("new_window_oc_small_25k", "window_oc_small_25k", 0.500000f),  // extent 32.0 px
            new Preview("new_window_oc_wide_25k", "window_oc_wide_25k", 0.500000f),  // extent 32.0 px
            new Preview("new_window_oc_toilet_25k", "window_oc_toilet_25k", 1.000000f),  // extent 16.0 px
            new Preview("new_window_25k_4", "window_25k_4", 0.400000f),  // extent 40.0 px
            new Preview("new_window_25k_5", "window_25k_5", 1.000000f),  // extent 16.0 px
            new Preview("new_end_face_sliding_25k_2", "end_face_sliding_25k_2", 0.321000f),  // extent 49.8 px
            new Preview("new_end_face_25k_2", "end_face_25k_2", 0.321000f),  // extent 49.8 px
            new Preview("new_end_face_sliding_25k_1", "end_face_sliding_25k_1", 0.321000f),  // extent 49.8 px
            new Preview("new_end_face_25k_1", "end_face_25k_1", 0.321000f),  // extent 49.8 px
            new Preview("new_door_sliding_25k", "door_sliding_25k", 0.640000f),  // extent 25.0 px
            new Preview("new_ladder_sliding_25k", "ladder_sliding_25k", 1.000000f),  // extent 15.5 px
            new Preview("new_door_25k", "door_25k", 1.000000f),  // extent 16.0 px
            new Preview("new_door_25k_lh", "door_25k_left", 1.000000f),  // extent 16.0 px
            new Preview("new_coupler_25k_1", "coupler_25k_1", 0.321000f),  // extent 49.8 px
            new Preview("new_coupler_25k_2", "coupler_25k_2", 0.320000f),  // extent 50.0 px
            new Preview("new_ac_25k_1", "ac_25k_1", 0.314000f),  // extent 51.0 px
            new Preview("new_ac_25k_2", "ac_25k_2", 0.314000f),  // extent 51.0 px
            new Preview("new_ac_25k_3", "ac_25k_3", 0.314000f),  // extent 51.0 px
            new Preview("new_generator_intake_25k", "generator_intake_25k", 0.842000f),  // extent 19.0 px
            new Preview("new_generator_intake_25k_lh", "generator_intake_25k_left", 0.842000f),  // extent 19.0 px
            new Preview("new_generator_intake_25k_2", "generator_intake_25k_2", 0.842000f),  // extent 19.0 px
            new Preview("new_radiator_grille_25k_1", "radiator_grille_25k_1", 0.800000f),  // extent 20.0 px
            new Preview("new_radiator_grille_25k_1_lh", "radiator_grille_25k_1_left", 0.800000f),  // extent 20.0 px
            new Preview("new_radiator_grille_25k_2", "radiator_grille_25k_2", 0.800000f),  // extent 20.0 px
            new Preview("new_radiator_fan_25k", "radiator_fan_25k", 0.276000f),  // extent 58.0 px
            new Preview("new_water_tank_25k", "water_tank_25k", 0.381000f),  // extent 42.0 px
            new Preview("new_oil_tank_25k", "oil_tank_25k", 0.235000f),  // extent 68.0 px
            new Preview("new_chimney_25k", "chimney_25k", 0.276000f),  // extent 58.0 px
            new Preview("new_generator_window_25k_2", "generator_window_25k_2", 1.000000f),  // extent 16.0 px
            new Preview("new_vent_grille_25k", "vent_grille_25k", 1.000000f),  // extent 16.0 px
            new Preview("new_vent_grille_25k_lh", "vent_grille_25k_left", 1.000000f),  // extent 16.0 px
            new Preview("new_vent_grille_25k_2", "vent_grille_25k_2", 1.000000f),  // extent 16.0 px
            new Preview("new_generator_ladder_25k", "generator_ladder_25k", 0.941000f),  // extent 17.0 px
            new Preview("new_generator_window_25k_1", "generator_window_25k_1", 0.500000f)  // extent 32.0 px
    );

    private C25KItemClientExtensions() {}

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        for (Preview preview : PREVIEWS) {
            ResourceLocation model = ResourceLocation.fromNamespaceAndPath(
                    "kuayue", "models/block/carriage/c25k/" + preview.modelName() + ".bbmodel");
            ModelLightingDefaults.register(model, ModelLightingMode.BLOCK);
            Item item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("kuayue", preview.name()));
            if (item == Items.AIR) {
                continue;
            }
            ItemModelRenderers.register(item, model, null, preview.guiScale());
            event.registerItem(ItemModelRenderers.clientExtensions(), item);
        }
    }
}
