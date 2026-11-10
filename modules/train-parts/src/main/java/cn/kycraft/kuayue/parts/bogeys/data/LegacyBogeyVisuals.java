package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyModels;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Stage-one visual bridge from JSON visual IDs to the existing Java bogey model catalogue.
 */
public final class LegacyBogeyVisuals {
    private static final Map<ResourceLocation, Supplier<List<ModelPart>>> VISUALS =
            new LinkedHashMap<>();

    static {
        register("cw2", () -> KuaYueBogeyModels.cw2(false));
        register("cw2_andesite", () -> KuaYueBogeyModels.cw2(true));
        register("pk209p_forward", () -> KuaYueBogeyModels.pk209p(false, false));
        register("pk209p_backward", () -> KuaYueBogeyModels.pk209p(true, false));
        register("pk209p_forward_andesite", () -> KuaYueBogeyModels.pk209p(false, true));
        register("pk209p_backward_andesite", () -> KuaYueBogeyModels.pk209p(true, true));
        register("pk209p_no_motor", () -> KuaYueBogeyModels.pk209pNoMotor(false, false));
        register("pk209p_no_motor_andesite", () -> KuaYueBogeyModels.pk209pNoMotor(false, true));
        register("sw220k", () -> KuaYueBogeyModels.sw220k(false));
        register("sw220k_andesite", () -> KuaYueBogeyModels.sw220k(true));
        register("sw160", () -> KuaYueBogeyModels.sw160(false));
        register("sw160_andesite", () -> KuaYueBogeyModels.sw160(true));
        register("zk6", () -> KuaYueBogeyModels.zk6(false));
        register("zk6_andesite", () -> KuaYueBogeyModels.zk6(true));
        register("mkz", KuaYueBogeyModels::mkz);
        register("tkz1", KuaYueBogeyModels::tkz1);
        register("tkz2", KuaYueBogeyModels::tkz2);
        register("tkz2_motor", () -> KuaYueBogeyModels.tkz2Motor(false));
        register("tkz2_motor_backward", () -> KuaYueBogeyModels.tkz2Motor(true));

        register("df11g", () -> KuaYueBogeyModels.df11g(false, false));
        register("df11g_backward", () -> KuaYueBogeyModels.df11g(true, false));
        register("df11g_andesite", () -> KuaYueBogeyModels.df11g(false, true));
        register("qj_main", () -> KuaYueBogeyModels.qjMain(false));
        register("qj_main_andesite", () -> KuaYueBogeyModels.qjMain(true));
        register("qj_guide", () -> KuaYueBogeyModels.qjGuide(false));
        register("qj_guide_andesite", () -> KuaYueBogeyModels.qjGuide(true));
        register("ss3", () -> KuaYueBogeyModels.ss3(false, false));
        register("ss3_backward", () -> KuaYueBogeyModels.ss3(true, false));
        register("ss3_andesite", () -> KuaYueBogeyModels.ss3(false, true));
        register("hxd3d", () -> KuaYueBogeyModels.hxd3d(false, false));
        register("hxd3d_backward", () -> KuaYueBogeyModels.hxd3d(true, false));
        register("hxd3d_andesite", () -> KuaYueBogeyModels.hxd3d(false, true));
        register("ss8", () -> KuaYueBogeyModels.ss8(false, false));
        register("ss8_backward", () -> KuaYueBogeyModels.ss8(true, false));
        register("ss8_andesite", () -> KuaYueBogeyModels.ss8(false, true));
        register("dfh21_standard", () -> KuaYueBogeyModels.dfh21(true));
        register("dfh21", () -> KuaYueBogeyModels.dfh21(false));
        register("jy290", () -> KuaYueBogeyModels.jy290(false));
        register("jy290_backward", () -> KuaYueBogeyModels.jy290(true));
        register("df5", () -> KuaYueBogeyModels.df5(false));
        register("df5_backward", () -> KuaYueBogeyModels.df5(true));
        register("df21", () -> KuaYueBogeyModels.df21(false));
        register("df21_backward", () -> KuaYueBogeyModels.df21(true));
    }

    private LegacyBogeyVisuals() {
    }

    /**
     * Resolves a visual supplier or fails when the schema names an unknown bridge profile.
     */
    public static Supplier<List<ModelPart>> require(ResourceLocation id) {
        Supplier<List<ModelPart>> visual = VISUALS.get(id);
        if (visual == null) {
            throw new IllegalArgumentException("Unknown legacy bogey visual: " + id);
        }
        return visual;
    }

    private static void register(String path, Supplier<List<ModelPart>> visual) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "kuayue",
                "legacy/" + path
        );
        if (VISUALS.putIfAbsent(id, visual) != null) {
            throw new IllegalStateException("Duplicate legacy bogey visual: " + id);
        }
    }
}
