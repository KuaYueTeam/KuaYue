package cn.kycraft.kuayue.parts.carriages.c25;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.LevelWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class C25ZPanel {

    public static final RegistryGroup GROUP_C25Z =
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

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_25Z =
            new PanelReg<>("door_25z", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage25z/door/original_25z_door_bottom_hinge"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage25z/door/original_25z_door_top_hinge")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage25z/door/original_25z_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage25z/door/original_25z_door_top")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, -.07),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            ))
                    .setParent(GROUP_C25Z);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25Z_1 =
            new PanelReg<>("end_face_25z_1", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.SLIDE_3,
                    "carriage/carriage25z/end_face/end_face_door_sliding_original_25z_right",
                    "carriage/carriage25z/end_face/end_face_door_sliding_original_25z_left",
                    "carriage/carriage25z/end_face/end_face_original_25z_1"
            ))
                    .setParent(GROUP_C25Z);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25Z_2 =
            new PanelReg<>("end_face_25z_2", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.SLIDE_3,
                    "carriage/carriage25z/end_face/end_face_door_sliding_original_25z_right",
                    "carriage/carriage25z/end_face/end_face_door_sliding_original_25z_left",
                    "carriage/carriage25z/end_face/end_face_original_25z_2"
            ))
                    .setParent(GROUP_C25Z);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25Z_3 =
            new PanelReg<>("end_face_25z_3", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.SLIDE_3,
                    "carriage/carriage25z/end_face/end_face_door_sliding_original_25z_right",
                    "carriage/carriage25z/end_face/end_face_door_sliding_original_25z_left",
                    "carriage/carriage25z/end_face/end_face_original_25z_3"
            ))
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_LINE_25Z =
            new PanelReg<>("panel_bottom_line_25z", TrainPanelBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_SIDE_25Z =
            new PanelReg<>("panel_bottom_side_25z", TrainPanelBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_SYMBOL_25Z =
            new PanelReg<>("panel_bottom_symbol_25z", TrainPanelBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<LevelPanelBlock> PANEL_MIDDLE_25Z =
            new PanelReg<>("panel_middle_25z", p -> new LevelPanelBlock(p, new Vec2(0, 0), new Vec2(1, 1)))
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainHingePanelBlock> PANEL_MIDDLE_SIDE_25Z =
            new PanelReg<>("panel_middle_side_25z", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_25Z =
            new PanelReg<>("panel_top_25z", TrainPanelBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_25Z =
            new PanelReg<>("window_oc_25z", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<LevelWindowBlock> WINDOW_OC_LEVEL_25Z =
            new PanelReg<>("window_oc_level_25z", LevelWindowBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_25Z_2 =
            new PanelReg<>("window_oc_small_25z_2", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_25Z =
            new PanelReg<>("window_oc_toilet_25z", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25Z);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_25Z =
            new PanelReg<>("window_oc_wide_25z", p -> new TrainOpenableWindowBlock(p, 2))
                    .setParent(GROUP_C25Z);

    public static final SlabReg<SlabBlock> FLOOR_25Z =
            new SlabReg<>("floor_25z", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25Z);

    public static final SlabReg<SlabBlock> TOILET_DD_25Z =
            new SlabReg<>("toilet_dd_25z", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25Z);

    public static final SlabReg<LadderBlock> LADDER_25Z =
            new SlabReg<>("ladder_25z", LadderBlock::new)
                    .setParent(GROUP_C25Z);

    public static final SlabReg<SlabBlock> CARPORT_25Z =
            new SlabReg<>("carport_25z", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25Z);

    public static final SlabReg<HingeSlabBlock> CARPORT_AC_25Z =
            new SlabReg<>("carport_ac_25z", p -> new HingeSlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25Z);

    public static final SlabReg<SlabBlock> CARPORT_CENTER_25Z =
            new SlabReg<>("carport_center_25z", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25Z);

    public static final SlabReg<HingeSlabBlock> CARPORT_SIDE_AC_25Z =
            new SlabReg<>("carport_side_ac_25z", p -> new HingeSlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25Z);
}
