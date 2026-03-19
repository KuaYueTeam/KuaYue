package cn.kycraft.kuayue.parts.carriages.c25;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableSmallWindowBlock;
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
public class C25KPanel {

    public static final RegistryGroup GROUP_C25K =
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

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_25K =
            new PanelReg<>("door_25k", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25k_door_bottom_hinge"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25k_door_top_hinge")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25k_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25k_door_top")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, -.124),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_SLIDING_25K =
            new PanelReg<>("door_sliding_25k", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25k_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25k_upper")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25k_bottom_lh"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25k_upper_lh")
                    ),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    true
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25K_1 =
            new PanelReg<>("end_face_25k_1", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25k/end_face/end_face_door_original_25k",
                    null,
                    "carriage/carriage25k/end_face/end_face_original_25k_1"
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25K_2 =
            new PanelReg<>("end_face_25k_2", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25k/end_face/end_face_door_original_25k",
                    null,
                    "carriage/carriage25k/end_face/end_face_original_25k_2"
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25K_3 =
            new PanelReg<>("end_face_25k_3", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25k/end_face/end_face_door_original_25k",
                    null,
                    "carriage/carriage25k/end_face/end_face_original_25k_3"
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_SLIDING_25K_1 =
            new PanelReg<>("end_face_sliding_25k_1", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.SLIDE_2,
                    "carriage/carriage25k/end_face/end_face_sliding_door_original_25k_right",
                    "carriage/carriage25k/end_face/end_face_sliding_door_original_25k_left",
                    "carriage/carriage25k/end_face/end_face_sliding_original_25k_1"
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_SLIDING_25K_2 =
            new PanelReg<>("end_face_sliding_25k_2", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.SLIDE_2,
                    "carriage/carriage25k/end_face/end_face_sliding_door_original_25k_right",
                    "carriage/carriage25k/end_face/end_face_sliding_door_original_25k_left",
                    "carriage/carriage25k/end_face/end_face_sliding_original_25k_2"
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_SLIDING_25K_3 =
            new PanelReg<>("end_face_sliding_25k_3", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.SLIDE_2,
                    "carriage/carriage25k/end_face/end_face_sliding_door_original_25k_right",
                    "carriage/carriage25k/end_face/end_face_sliding_door_original_25k_left",
                    "carriage/carriage25k/end_face/end_face_sliding_original_25k_3"
            ))
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_25K =
            new PanelReg<>("panel_bottom_25k", TrainPanelBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_LINE_25K =
            new PanelReg<>("panel_bottom_line_25k", TrainPanelBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainPanelBlock> PANEL_MIDDLE_25K =
            new PanelReg<>("panel_middle_25k", TrainPanelBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainHingePanelBlock> PANEL_SYMBOL_25K =
            new PanelReg<>("panel_symbol_25k", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainHingePanelBlock> PANEL_SYMBOL_MARSHALLED_25K =
            new PanelReg<>("panel_symbol_marshalled_25k", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainHingePanelBlock> PANEL_SYMBOL_MARSHALLED_25K_B =
            new PanelReg<>("panel_symbol_marshalled_25k_b", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_25K =
            new PanelReg<>("panel_top_25k", TrainPanelBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_SMALL_25K =
            new PanelReg<>("window_oc_sealed_small_25k", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_25K =
            new PanelReg<>("window_oc_small_25k", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_25K =
            new PanelReg<>("window_oc_toilet_25k", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_25K =
            new PanelReg<>("window_oc_wide_25k", p -> new TrainOpenableWindowBlock(p,  2))
                    .setParent(GROUP_C25K);

    public static final PanelReg<TrainUnOpenableWindowBlock> WINDOW_OC_WIDE_SEALED_25K =
            new PanelReg<>("window_oc_wide_sealed_25k", p -> new TrainUnOpenableWindowBlock(p, 2))
                    .setParent(GROUP_C25K);

    public static final SlabReg<SlabBlock> FLOOR_25K =
            new SlabReg<>("floor_25k", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25K);

    public static final SlabReg<SlabBlock> TOILET_DD_25K =
            new SlabReg<>("toilet_dd_25k", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25K);

    public static final SlabReg<LadderBlock> LADDER_SLIDING_25K =
            new SlabReg<>("ladder_sliding_25k", LadderBlock::new)
                    .setParent(GROUP_C25K);
}
