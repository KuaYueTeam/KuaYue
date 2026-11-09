package cn.kycraft.kuayue.parts.bogeys.core;

import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import lib.kasuga.create.proxy.BogeyModels;
import lib.kasuga.create.proxy.BogeyRoots;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import lib.kasuga.rendering.models.uml.math.Transform;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * KuaYue's 2.0 bogey model catalogue.
 *
 * <p>The catalogue is deliberately separate from the registration classes: Create owns a style's
 * size/block mapping, while {@link BogeyModels} owns the geometry that KuaYue's proxy pipeline
 * draws for a given style and size. Local transforms are copied from the retired 1.0
 * renderer/visual pairs, relative to Create's model origin.
 */
public final class KuaYueBogeyModels {

    private static final float FRAME_SCALE = 1f - 1f / 512f;
    private static final float ANDESITE_SCALE = 1.2f;

    private KuaYueBogeyModels() {
    }

    public static void register(
            BogeyStyle style,
            BogeySizes.BogeySize size,
            List<ModelPart> parts
    ) {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(size, "size");
        BogeyModels.register(
                style.id,
                size,
                List.copyOf(Objects.requireNonNull(parts, "parts")),
                BogeyRoots.CREATE_Y_OFFSET
        );
    }

    public static List<ModelPart> cw2(boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("cw2/bogey_cw2_temple", false, scale, 0, 0, 0, 0, 0, FRAME_SCALE),
                at("915mm_carriage_general_wheel", false, scale, 0, 0.805f, -1.18f, 0, 0, 1),
                at("915mm_carriage_general_wheel", false, scale, 0, 0.805f, 1.18f, 0, 0, 1)
        );
    }

    public static List<ModelPart> pk209p(boolean backward, boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        float wheelZ = backward ? 1.2f : -1.2f;
        float motorX = backward ? -1.117f : 1.117f;
        float motorZ = backward ? -2.165f : 2.165f;
        return List.of(
                at("pk209p/pk209p_main", backward, scale, 0, 0.91f, 0, 0, 0, FRAME_SCALE),
                at("pk209p/pk209p_wheel", backward, scale, 0, 0.8f, -wheelZ, 0, 0, 1),
                at("915mm_carriage_general_wheel", backward, scale, 0, 0.8f, wheelZ, 0, 0, 1),
                at("pk209p/pk209_motorwheel", backward, scale, motorX, 0.82f, motorZ, 0, 0, 1)
        );
    }

    public static List<ModelPart> pk209pNoMotor(boolean backward, boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("pk209p/pk209_nomotor", backward, scale, 0, 0.91f, 0, 0, 0, FRAME_SCALE),
                at("915mm_carriage_general_wheel", backward, scale, 0, 0.8f, -1.2f, 0, 0, 1),
                at("915mm_carriage_general_wheel", backward, scale, 0, 0.8f, 1.2f, 0, 0, 1)
        );
    }

    public static List<ModelPart> sw220k(boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("sw220k/sw220k_frame", false, scale, 0, 0.925f, 0, 0, 0, FRAME_SCALE),
                at("sw220k/sw220k_wheel", false, scale, 0, 0.8f, -1.18f, 0, 0, 1),
                at("sw220k/sw220k_wheel", false, scale, 0, 0.8f, 1.18f, 0, 0, 1)
        );
    }

    public static List<ModelPart> sw160(boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("sw160/sw160_frame", false, scale, 0, 0.91f, 0, 0, 0, FRAME_SCALE),
                at("sw160/sw160_wheel", false, scale, 0, 0.8f, -1.18f, 0, 0, 1),
                at("sw160/sw160_wheel", false, scale, 0, 0.8f, 1.18f, 0, 0, 1)
        );
    }

    public static List<ModelPart> zk6(boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("zk6/zk6_frame", false, scale, 0, 0.38f, 0, 0, 0, FRAME_SCALE),
                at("zk6/zk6_wheel", false, scale, 0, 0.745f, -0.917f, 0, 0, 1),
                at("zk6/zk6_wheel", false, scale, 0, 0.745f, 0.917f, 0, 0, 1)
        );
    }

    public static List<ModelPart> mkz() {
        return List.of(
                at("mkz/mkz_frame", false, 1, 0, 0.32f, 0, 0, 0, 1),
                at("mkz/mkz_wheel", false, 1, 0, 0.695f, -0.733f, 0, 0, 1),
                at("mkz/mkz_wheel", false, 1, 0, 0.695f, 0.733f, 0, 0, 1)
        );
    }

    public static List<ModelPart> tkz1() {
        return List.of(
                at("tkz1/tkz1_frame", false, 1, 0, 0.14f, 0, 0, 0, 1),
                at("tkz2/tkz2_wheel", false, 1, 0, 0.695f, -0.97f, 0, 0, 1),
                at("tkz2/tkz2_wheel", false, 1, 0, 0.695f, 0.97f, 0, 0, 1)
        );
    }

    public static List<ModelPart> tkz2() {
        return List.of(
                at("tkz2/tkz2_frame", false, 1, 0, 0.195f, 0, 0, 0, 1),
                at("tkz2/tkz2_wheel", false, 1, 0, 0.695f, -0.97f, 0, 0, 1),
                at("tkz2/tkz2_wheel", false, 1, 0, 0.695f, 0.97f, 0, 0, 1)
        );
    }

    public static List<ModelPart> tkz2Motor(boolean backward) {
        return List.of(
                at("tkz2/tkz2_motor_frame", backward, 1, 0, 0.129f, 0, 0, 0, 1),
                at("tkz2/tkz2_wheel", backward, 1, 0, 0.695f, 0.9761f, 0, 0, 1),
                at("tkz2/tkz2_motor_wheel", backward, 1, 0, 0.695f, -0.9761f, 0, 0, 1),
                at("tkz2/tkz2_motor_pony_wheel", backward, 1, 0, 0.7235f, -1.94f, 0, 0, 1)
        );
    }

    public static List<ModelPart> df11g(boolean backward, boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("df11g/df11g_bogey_temple", backward, scale, 0, 0.375f, 0, 0, 0, 1),
                at("df11g/df11g_wheel", backward, scale, 0, 0.88f, -2, 0, 0, 1),
                at("df11g/df11g_wheel", backward, scale, 0, 0.88f, 0, 0, 0, 1),
                at("df11g/df11g_wheel", backward, scale, 0, 0.88f, 2, 0, 0, 1)
        );
    }

    public static List<ModelPart> df21(boolean backward) {
        return List.of(
                at("df21/df21_bogey_temple", backward, 1, 0, 0.225f, 0.17f, 0, 0, 1),
                at("df21/df21_wheel", backward, 1, 0, 0.88f, -1.805f, 0, 0, 1),
                at("df21/df21_wheel", backward, 1, 0, 0.88f, 0, 0, 0, 1),
                at("df21/df21_wheel", backward, 1, 0, 0.88f, 1.805f, 0, 0, 1)
        );
    }

    public static List<ModelPart> df5(boolean backward) {
        boolean flip = !backward;
        return List.of(
                at("df5/df5_frame", flip, 1, 0, 1, 0, 0, 0, 1),
                at("df5/df5_wheel", flip, 1, 0, 0.878f, 0, 0, 0, 1),
                at("df5/df5_wheel", flip, 1, 0, 0.878f, -1.995f, 0, 0, 1),
                at("df5/df5_wheel", flip, 1, 0, 0.878f, 1.995f, 0, 0, 1)
        );
    }

    public static List<ModelPart> dfh21(boolean standard) {
        String main = standard ? "dfh21/dfh21_main_standard" : "dfh21/dfh21_main";
        String wheel = standard ? "dfh21/dfh21_wheel_standard" : "dfh21/dfh21_wheel";
        String rod = standard ? "dfh21/dfh21_transmission_rod_standard" : "dfh21/dfh21_transmission_rod";
        float wheelY = standard ? 0.875f : 0.9f;
        return List.of(
                at(main, true, 1, 0, 0, 0, 0, 0, 1),
                at(wheel, true, 1, 0, wheelY, -1.2f, 0, 0, 1),
                at(wheel, true, 1, 0, wheelY, 1.2f, 0, 0, 1),
                at(rod, true, 1, 0, 0.85f, 0, 0, 0, 1)
        );
    }

    public static List<ModelPart> hxd3d(boolean backward, boolean andesite) {
        boolean flip = !backward;
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("hxd3d/hxd3d_frame", flip, scale, 0, 0.012f, 0, 0, 0, 1),
                at("hxd3d/hxd3d_wheel", flip, scale, 0, 0.905f, 0.23f, 0, 0, 1),
                at("hxd3d/hxd3d_wheel", flip, scale, 0, 0.905f, 2.365f, 0, 0, 1),
                at("hxd3d/hxd3d_wheel", flip, scale, 0, 0.905f, -1.69f, 0, 0, 1)
        );
    }

    public static List<ModelPart> jy290(boolean backward) {
        boolean flip = !backward;
        return List.of(
                at("jy290/jy290_frame", flip, 1, 0, -0.07f, 0, 0, 0, 1),
                at("jy290/jy290_wheel", flip, 1, 0, 0.775f, -1.195f, 0, 0, 1),
                at("jy290/jy290_wheel", flip, 1, 0, 0.775f, 1.195f, 0, 0, 1)
        );
    }

    public static List<ModelPart> qjGuide(boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("qj/qj_guide_frame", false, scale, 0, 0, 0.3f, 0, 0, 1),
                at("qj/qj_guide_wheel", false, scale, 0, 0.77f, -2, 0, 0, 1)
        );
    }

    public static List<ModelPart> qjMain(boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        List<ModelPart> parts = new ArrayList<>();
        parts.add(at("qj/qj_bogey_frame", false, scale, 0, 0, 0, 0, 0, 1));
        parts.add(at("qj/main_drive_wheel", false, scale, 0, 1.1f, 0, 0, 180, 1));
        parts.add(at("qj/drive_wheel_2_4", false, scale, 0, 1.1f, -1.6f, 0, 180, 1));
        parts.add(at("qj/drive_wheel_2_4", false, scale, 0, 1.1f, 1.6f, 0, 180, 1));
        parts.add(at("qj/drive_wheel_1_5", false, scale, 0, 1.1f, -3.2f, 0, 180, 1));
        parts.add(at("qj/drive_wheel_1_5", false, scale, 0, 1.1f, 3.2f, 0, 180, 1));
        parts.add(at("qj/crank", false, scale, 1.15f, 1.13f, -2.8f, 0, 0, 1));
        parts.add(at("qj/crank_otherside", false, scale, -1.15f, 1.13f, -2.8f, 0, 0, 1));
        parts.add(at("qj/expansion_link", false, scale, 1.32f, 1.74f, -1.79f, 0, 0, 1));
        parts.add(at("qj/expansion_link_otherside", false, scale, -1.32f, 1.74f, -1.79f, 0, 0, 1));
        parts.add(at("qj/connecting_rod", false, scale, 0.96f, 1.5f, 0, 0, 0, 1));
        parts.add(at("qj/connecting_rod_otherside", false, scale, -0.96f, 1.5f, 0, 0, 0, 1));
        parts.add(at("qj/eccentric_rod", false, scale, 1.36f, 1.1f, 0.23f, 0, 0, 1));
        parts.add(at("qj/eccentric_rod_otherside", false, scale, -1.36f, 1.1f, 0.23f, 0, 0, 1));
        parts.add(at("qj/crosshead", false, scale, 1.15f, 1.13f, -2.85f, 0, 0, 1));
        parts.add(at("qj/crosshead_otherside", false, scale, -1.15f, 1.13f, -2.85f, 0, 0, 1));
        parts.add(at("qj/anchor_link", false, scale, 1.3f, 0.83f, -2.85f, 0, 0, 1));
        parts.add(at("qj/anchor_link_otherside", false, scale, -1.3f, 0.83f, -2.85f, 0, 0, 1));
        parts.add(at("qj/combination_lever", false, scale, 1.3f, 1.75f, -3.33f, 0, 0, 1));
        parts.add(at("qj/combination_lever_otherside", false, scale, -1.3f, 1.75f, -3.33f, -25.95f, 0, 1));
        parts.add(at("qj/steam_valve", false, scale, 1.3f, 1.62f, -3.92f, 0, 0, 1));
        parts.add(at("qj/steam_valve_otherside", false, scale, -1.3f, 1.62f, -3.92f, 0, 0, 1));
        parts.add(at("qj/radius_rod", false, scale, 1.31f, 1.8f, -3.38f, 0, 0, 1));
        parts.add(at("qj/radius_rod_otherside", false, scale, -1.31f, 1.8f, -3.38f, 0, 0, 1));
        parts.add(at("qj/oil_press_rod", false, scale, 1.29f, 1.98f, -3.23f, 10, 0, 1));
        parts.add(at("qj/oil_press_rod_otherside", false, scale, -1.29f, 1.99f, -3.32f, 25, 0, 1));
        parts.add(at("qj/oil_press", false, scale, 1.29f, 2.16f, -3.42f, 7, 0, 1));
        parts.add(at("qj/oil_press_otherside", false, scale, -1.29f, 2.16f, -3.42f, 2, 0, 1));
        parts.add(at("qj/reversing_boom", false, scale, 1.18f, 2.18f, -2.13f, 0, 0, 1));
        parts.add(at("qj/reversing_boom_otherside", false, scale, -1.18f, 2.18f, -2.13f, 0, 0, 1));
        parts.add(at("qj/reversing", false, scale, 0, 2.15f, -1.83f, 0, 0, 1));
        return List.copyOf(parts);
    }

    public static List<ModelPart> ss3(boolean backward, boolean andesite) {
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("ss3/ss3_frame", backward, scale, 0, 0.012f, 0, 0, 0, 1),
                at("ss3/ss3_wheel", backward, scale, 0, 0.88f, -2.11f, 0, 0, 1),
                at("ss3/ss3_wheel", backward, scale, 0, 0.88f, 1.96f, 0, 0, 1),
                at("ss3/ss3_wheel", backward, scale, 0, 0.88f, -0.01f, 0, 0, 1)
        );
    }

    public static List<ModelPart> ss8(boolean backward, boolean andesite) {
        boolean flip = !backward;
        float scale = andesite ? ANDESITE_SCALE : 1f;
        return List.of(
                at("ss8/ss8_frame", flip, scale, 0, 0, 0, 0, 0, 1),
                at("ss8/ss8_wheel", flip, scale, 0, 0.905f, -1.3f, 0, 0, 1),
                at("ss8/ss8_wheel", flip, scale, 0, 0.905f, 1.304f, 0, 0, 1)
        );
    }

    private static ModelPart at(
            String relativePath,
            boolean flip,
            float rootScaleX,
            float x,
            float y,
            float z,
            float rotateX,
            float rotateY,
            float modelScale
    ) {
        Transform local = new Transform();
        if (flip) {
            local.rotate(0, 180, 0, true);
        }
        if (rootScaleX != 1) {
            local.scale(rootScaleX, 1, 1);
        }
        local.translate(x, y, z);
        if (rotateY != 0) {
            local.rotate(0, rotateY, 0, true);
        }
        if (rotateX != 0) {
            local.rotate(rotateX, 0, 0, true);
        }
        if (modelScale != 1) {
            local.scale(modelScale, modelScale, modelScale);
        }
        return new ModelPart(obj(relativePath), null, local, null);
    }

    private static ResourceLocation obj(String relativePath) {
        return ResourceLocation.fromNamespaceAndPath(
                "kuayue",
                "models/block/bogey/" + relativePath + ".obj"
        );
    }
}
