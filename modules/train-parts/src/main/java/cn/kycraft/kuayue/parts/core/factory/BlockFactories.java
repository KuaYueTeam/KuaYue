package cn.kycraft.kuayue.parts.core.factory;

import cn.kycraft.kuayue.parts.core.panel.AirVentBlock;
import cn.kycraft.kuayue.parts.core.panel.FreightC70EndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.FreightNX70EndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.HingeSlabBlock;
import cn.kycraft.kuayue.parts.core.panel.LadderBlock;
import cn.kycraft.kuayue.parts.core.panel.LevelPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.SkirtBlock;
import cn.kycraft.kuayue.parts.core.panel.SlabBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainHingePanelBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.DoubleRotateDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.MeterCarriageEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SkirtReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.LevelWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableSmallWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.factory.FactoryRegistry;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Context
public class BlockFactories {

    public static FactoryRegistry.BlockFactory TRAIN_PANEL = (id, params) -> new PanelReg<>(id, p -> {
        if (params != null && (params.has("begin_x") || params.has("end_x"))) {
            float bx = FactoryParams.getFloatOr(params, "begin_x", 0f);
            float by = FactoryParams.getFloatOr(params, "begin_y", 0f);
            float ex = FactoryParams.getFloatOr(params, "end_x", 1f);
            float ey = FactoryParams.getFloatOr(params, "end_y", 1f);
            return new TrainPanelBlock(p, new Vec2(bx, by), new Vec2(ex, ey));
        }
        return new TrainPanelBlock(p);
    });

    public static FactoryRegistry.BlockFactory TRAIN_OPENABLE_WINDOW = (id, params) -> new PanelReg<>(id, p -> {
        if (params != null && (params.has("left") || params.has("right") || params.has("height"))) {
            int left = FactoryParams.getIntOr(params, "left", 0);
            int right = FactoryParams.getIntOr(params, "right", 1);
            int height = FactoryParams.getIntOr(params, "height", 1);
            return new TrainOpenableWindowBlock(p, left, right, height);
        }
        int wide = params != null ? FactoryParams.getIntOr(params, "wide", 1) : 1;
        return new TrainOpenableWindowBlock(p, wide);
    });

    public static FactoryRegistry.BlockFactory SLAB = (id, params) -> new SlabReg<>(id, p -> new SlabBlock(p, false));
    public static FactoryRegistry.BlockFactory SLAB_TOP = (id, params) -> new SlabReg<>(id, p -> new SlabBlock(p, true));

    public static FactoryRegistry.BlockFactory LADDER = (id, params) -> new SlabReg<>(id, p -> {
        boolean isMeter = FactoryParams.getBooleanOr(params, "is_meter", false);
        return new LadderBlock(p, isMeter);
    });

    public static FactoryRegistry.BlockFactory TRAIN_SMALL_WINDOW = (id, params) -> new PanelReg<>(id, p -> {
        int height = FactoryParams.getIntOr(params, "height", 1);
        if (height != 1) return new TrainSmallWindowBlock(p, height);
        return new TrainSmallWindowBlock(p);
    });

    public static FactoryRegistry.BlockFactory TRAIN_HINGE_PANEL = (id, params) -> new PanelReg<>(id, p -> {
        if (params != null && (params.has("begin_x") || params.has("end_x"))) {
            float bx = FactoryParams.getFloatOr(params, "begin_x", 0f);
            float by = FactoryParams.getFloatOr(params, "begin_y", 0f);
            float ex = FactoryParams.getFloatOr(params, "end_x", 1f);
            float ey = FactoryParams.getFloatOr(params, "end_y", 1f);
            return new TrainHingePanelBlock(p, new Vec2(bx, by), new Vec2(ex, ey));
        }
        return new TrainHingePanelBlock(p);
    });

    public static FactoryRegistry.BlockFactory AIR_VENT = (id, params) -> new SlabReg<>(id, AirVentBlock::new);

    public static FactoryRegistry.BlockFactory CUSTOM_RENDERED_DOOR = (id, params) -> new PanelReg<>(id, p -> new CustomRenderedDoorBlock(
            p,
            FactoryParams.getModelCouple(params, "left_models"),
            FactoryParams.getModelCouple(params, "right_models"),
            FactoryParams.getVec3(params, "offset", Vec3.ZERO),
            FactoryParams.getVec3(params, "open_offset", Vec3.ZERO),
            FactoryParams.getRenderShape(params, "render_shape", RenderShape.ENTITYBLOCK_ANIMATED),
            FactoryParams.getBooleanOr(params, "is_slide_door", false)
    ));

    public static FactoryRegistry.BlockFactory CUSTOM_RENDERED_END_FACE = (id, params) -> new PanelReg<>(id, p -> new CustomRenderedEndFaceBlock(
            p,
            TrainPanelProperties.DoorType.fromString(FactoryParams.getStringOr(params, "door_type", "no_door")),
            FactoryParams.getString(params, "left_model"),
            FactoryParams.getString(params, "right_model"),
            FactoryParams.getStringOr(params, "frame_model", "")
    ));

    public static FactoryRegistry.BlockFactory TRAIN_UNOPENABLE_SMALL_WINDOW = (id, params) ->
            new PanelReg<>(id, TrainUnOpenableSmallWindowBlock::new);

    public static FactoryRegistry.BlockFactory TRAIN_UNOPENABLE_WINDOW = (id, params) -> new PanelReg<>(id, p -> {
        int wide = FactoryParams.getIntOr(params, "wide", 1);
        int beginY = FactoryParams.getIntOr(params, "begin_y", 0);
        int endY = FactoryParams.getIntOr(params, "end_y", 1);
        return new TrainUnOpenableWindowBlock(p, wide, beginY, endY);
    });

    public static FactoryRegistry.BlockFactory HINGE_SLAB = (id, params) -> new SlabReg<>(id, p -> {
        boolean isCarport = FactoryParams.getBooleanOr(params, "is_carport", false);
        int width = FactoryParams.getIntOr(params, "width", 1);
        return new HingeSlabBlock(p, isCarport, width);
    });

    public static FactoryRegistry.BlockFactory SKIRT = (id, params) -> new SkirtReg<>(id, SkirtBlock::new);

    public static FactoryRegistry.BlockFactory LEVEL_PANEL = (id, params) -> new PanelReg<>(id, p -> {
        float bx = FactoryParams.getFloatOr(params, "begin_x", 0f);
        float by = FactoryParams.getFloatOr(params, "begin_y", 0f);
        float ex = FactoryParams.getFloatOr(params, "end_x", 1f);
        float ey = FactoryParams.getFloatOr(params, "end_y", 1f);
        return new LevelPanelBlock(p, new Vec2(bx, by), new Vec2(ex, ey));
    });

    public static FactoryRegistry.BlockFactory LEVEL_WINDOW = (id, params) ->
            new PanelReg<>(id, LevelWindowBlock::new);

    public static FactoryRegistry.BlockFactory DOUBLE_ROTATE_DOOR = (id, params) -> new PanelReg<>(id, p -> new DoubleRotateDoorBlock(
            p,
            TrainPanelProperties.DoorType.fromString(FactoryParams.getStringOr(params, "door_type", "no_door")),
            FactoryParams.getString(params, "left_model"),
            FactoryParams.getString(params, "right_model"),
            FactoryParams.getStringOr(params, "frame_model", "")
    ));

    public static FactoryRegistry.BlockFactory FREIGHT_END_FACE = (id, params) -> new PanelReg<>(id, p -> {
        String type = FactoryParams.getStringOr(params, "freight_type", "c70");
        if ("nx70".equals(type)) {
            return new FreightNX70EndFaceBlock(p, FreightNX70EndFaceBlock.FreightType.NX70);
        }
        return new FreightC70EndFaceBlock(p, FreightC70EndFaceBlock.FreightType.C70);
    });

    public static FactoryRegistry.BlockFactory METER_CARRIAGE_END_FACE = (id, params) -> new PanelReg<>(id, p -> {
        TrainPanelProperties.DoorType doorType = TrainPanelProperties.DoorType.fromString(
                FactoryParams.getStringOr(params, "door_type", "no_door"));
        boolean outside = FactoryParams.getBooleanOr(params, "outside", false);
        String leftModel = FactoryParams.getString(params, "left_model");
        String rightModel = FactoryParams.getString(params, "right_model");
        String frameModel = FactoryParams.getString(params, "frame_model");
        if (leftModel != null || rightModel != null || frameModel != null) {
            return new MeterCarriageEndFaceBlock(p, doorType, leftModel, rightModel, frameModel, outside);
        }
        return new MeterCarriageEndFaceBlock(p, doorType, outside);
    });

    static {
        FactoryRegistry.register("train_panel", TRAIN_PANEL, true);
        FactoryRegistry.register("slab", SLAB, true);
        FactoryRegistry.register("slab_top", SLAB_TOP, true);
        FactoryRegistry.register("ladder", LADDER, true);
        FactoryRegistry.register("train_small_window", TRAIN_SMALL_WINDOW, true);
        FactoryRegistry.register("train_openable_window", TRAIN_OPENABLE_WINDOW, true);
        FactoryRegistry.register("train_hinge_panel", TRAIN_HINGE_PANEL, true);
        FactoryRegistry.register("air_vent", AIR_VENT, true);
        FactoryRegistry.register("custom_rendered_door", CUSTOM_RENDERED_DOOR, true);
        FactoryRegistry.register("custom_rendered_end_face", CUSTOM_RENDERED_END_FACE, true);
        FactoryRegistry.register("train_unopenable_small_window", TRAIN_UNOPENABLE_SMALL_WINDOW, true);
        FactoryRegistry.register("train_unopenable_window", TRAIN_UNOPENABLE_WINDOW, true);
        FactoryRegistry.register("hinge_slab", HINGE_SLAB, true);
        FactoryRegistry.register("skirt", SKIRT, true);
        FactoryRegistry.register("level_panel", LEVEL_PANEL, true);
        FactoryRegistry.register("level_window", LEVEL_WINDOW, true);
        FactoryRegistry.register("double_rotate_door", DOUBLE_ROTATE_DOOR, true);
        FactoryRegistry.register("freight_end_face", FREIGHT_END_FACE, true);
        FactoryRegistry.register("meter_carriage_end_face", METER_CARRIAGE_END_FACE, true);
    }
}
