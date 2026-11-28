package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.locomotives.jy.client.JYMaintenanceModelLighting;
import lib.kasuga.rendering.models.mc.api.ModelLightingDefaults;
import lib.kasuga.rendering.models.uml.backend.ModelLightingMode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JYMaintenanceModelLightingTest {
    @Test
    void enablesAllJYModelVariantsWithoutChangingOtherVehicles() {
        JYMaintenanceModelLighting.registerDefaults();
        for (String model : List.of("head", "carport", "console", "outtrigger_left", "outtrigger_right",
                "endface", "door_left", "door_right", "panel_left", "panel_right", "tail")) {
            assertEquals(ModelLightingMode.BLOCK, ModelLightingDefaults.resolve(ResourceLocation.parse(
                    "kuayue:models/block/jy_maintenance/jy_maintenance_" + model + ".bbmodel")), model);
        }
        assertEquals(ModelLightingMode.PBR, ModelLightingDefaults.resolve(ResourceLocation.parse(
                "kuayue:models/block/bogey/df11g/df11g_frame.bbmodel")));
    }
}
