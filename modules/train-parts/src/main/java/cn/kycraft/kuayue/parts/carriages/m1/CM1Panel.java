package cn.kycraft.kuayue.parts.carriages.m1;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.MeterCarriageEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class CM1Panel {

    public static final RegistryGroup GROUP_M1 =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block
                            .noOcclusion()
                            .strength(1.5f, 3f)
                            .mapColor(DyeColor.BLUE)
                    )
            ).configure(
                    ItemRegConfigurations.adopt(
                            item -> item.stacksTo(8)
                                    .tabTo(TRAIN_PANEL_TAB)
                    )
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final SlabReg<SlabBlock> FLOOR_M1 =
            new SlabReg<>("floor_m1", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainPanelBlock> BOTTOM_SLAB_M1 =
            new PanelReg<>("slab_bottom_m1", TrainPanelBlock::new)
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_LARGE_M1 =
            new PanelReg<>("window_large_m1", p -> new TrainOpenableWindowBlock(p, -1, 1, 2))
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_LARGE_BETWEEN_M1 =
            new PanelReg<>("window_large_between_m1", p -> new TrainSmallWindowBlock(p, 2))
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_SMALL_M1 =
            new PanelReg<>("window_small_m1", p -> new TrainOpenableWindowBlock(p, -1, 1, 2))
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_DOUBLE_SMALL_M1 =
            new PanelReg<>("window_double_small_m1", p -> new TrainOpenableWindowBlock(p, -1, 1, 2))
                    .setParent(GROUP_M1);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_M1 =
            new PanelReg<>("door_m1", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_m1/door/m1_door_bottom_lh"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_m1/door/m1_door_upper_lh")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_m1/door/m1_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_m1/door/m1_door_upper")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            ))
                    .setParent(GROUP_M1);

    public static final SlabReg<LadderBlock> LADDER_M1 =
            new SlabReg<>("ladder_m1", p -> new LadderBlock(p, true))
                    .setParent(GROUP_M1);

    public static final PanelReg<MeterCarriageEndFaceBlock> END_FACE_M1 =
            new PanelReg<>("end_face_m1", p ->
                    new MeterCarriageEndFaceBlock(p, TrainPanelProperties.DoorType.NO_DOOR, true)
            ).setParent(GROUP_M1);

    public static final PanelReg<MeterCarriageEndFaceBlock> END_FACE_MIDDLE_M1 =
            new PanelReg<>("end_face_middle_m1", p ->
                    new MeterCarriageEndFaceBlock(
                            p,
                            TrainPanelProperties.DoorType.SLIDE,
                            "carriage/carriage_m1/end_face/m1_middle_end_face_door",
                            null,
                            "carriage/carriage_m1/end_face/m1_middle_end_face_frame",
                            false
                    )
            ).setParent(GROUP_M1);

    public static final SlabReg<SlabBlock> GENERAL_CARPORT_M1 =
            new SlabReg<>("carport_general_m1", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_M1);

    public static final SlabReg<SlabBlock> AIR_VENT_CARPORT_M1 =
            new SlabReg<>("carport_air_vent_m1", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_M1);

    public static final SlabReg<SlabBlock> AIR_VENT_CARPORT_M1_2 =
            new SlabReg<>("carport_air_vent_m1_2", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_M1);

    public static final SlabReg<SlabBlock> MIDDLE_CARPORT_M1 =
            new SlabReg<>("carport_middle_m1", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_LARGE_JY30 =
            new PanelReg<>("window_large_jy30", p -> new TrainOpenableWindowBlock(p, -1, 1, 2))
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_LARGE_BETWEEN_JY30 =
            new PanelReg<>("window_large_between_jy30", p -> new TrainSmallWindowBlock(p, 2))
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainPanelBlock> BOTTOM_SLAB_JY30 =
            new PanelReg<>("slab_bottom_jy30", TrainPanelBlock::new)
                    .setParent(GROUP_M1);

    public static final SlabReg<HingeSlabBlock> FLOOR_BATTERY_M1 =
            new SlabReg<>("floor_battery_m1", p -> new HingeSlabBlock(p, true))
                    .setParent(GROUP_M1);

    public static final SlabReg<SlabBlock> FLOOR_RESERVOIR_BOX_M1 =
            new SlabReg<>("floor_reservoir_box_m1", p -> new SlabBlock(p, true))
                    .setParent(GROUP_M1);

    public static final PanelReg<TrainUnOpenableWindowBlock> SLAB_TOP_M1 =
            new PanelReg<>("slab_top_m1", p -> new TrainUnOpenableWindowBlock(p, 2, 0, 2))
                    .setParent(GROUP_M1);
}
