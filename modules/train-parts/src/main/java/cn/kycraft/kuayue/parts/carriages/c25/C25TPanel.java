package cn.kycraft.kuayue.parts.carriages.c25;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SkirtReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableSmallWindowBlock;
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

@Context
public class C25TPanel {

    public static final RegistryGroup GROUP_C25T =
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

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_25T =
            new PanelReg<>("door_25t", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25t_door_bottom_hinge"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25t_door_top_hinge")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25t_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25t_door_top")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, -.124),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            ))
                    .setParent(GROUP_C25T);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_SLIDING_25T =
            new PanelReg<>("door_sliding_25t", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25t_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25t_upper")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25t_bottom_lh"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/sliding_door_25t_upper_lh")
                    ),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    true
            ))
                    .setParent(GROUP_C25T);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25T_1 =
            new PanelReg<>("end_face_25t_1", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25t/end_face/end_face_door_original_25t",
                    null,
                    "carriage/carriage25t/end_face/end_face_original_rubber_25t_1"
            ))
                    .setParent(GROUP_C25T);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25T_2 =
            new PanelReg<>("end_face_25t_2", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25t/end_face/end_face_door_original_25t",
                    null,
                    "carriage/carriage25t/end_face/end_face_original_rubber_25t_2"
            ))
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_25T =
            new PanelReg<>("panel_bottom_25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainHingePanelBlock> PANEL_BOTTOM_25T_2 =
            new PanelReg<>("panel_bottom_25t_2", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainPanelBlock> PANEL_MIDDLE_25T =
            new PanelReg<>("panel_middle_25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainHingePanelBlock> PANEL_MIDDLE_25T_2 =
            new PanelReg<>("panel_middle_25t_2", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_25T =
            new PanelReg<>("panel_top_25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainHingePanelBlock> PANEL_TOP_25T_2 =
            new PanelReg<>("panel_top_25t_2", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_XL25T =
            new PanelReg<>("panel_top_xl25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_BLUE_25T =
            new PanelReg<>("window_oc_sealed_blue_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_SMALL_25T =
            new PanelReg<>("window_oc_sealed_small_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_SMALL_BLUE_25T =
            new PanelReg<>("window_oc_sealed_small_blue_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_SMALL_BLUE_BSP25T =
            new PanelReg<>("window_oc_sealed_small_blue_bsp25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_BLUE_25T =
            new PanelReg<>("window_oc_blue_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_25T =
            new PanelReg<>("window_oc_small_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_BLUE_25T =
            new PanelReg<>("window_oc_small_blue_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_BLUE_BSP25T =
            new PanelReg<>("window_oc_small_blue_bsp25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_25T =
            new PanelReg<>("window_oc_toilet_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_BLUE_25T =
            new PanelReg<>("window_oc_toilet_blue_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_BLUE_BSP25T =
            new PanelReg<>("window_oc_toilet_blue_bsp25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_25T =
            new PanelReg<>("window_oc_wide_25t", TrainOpenableWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_BLUE_25T =
            new PanelReg<>("window_oc_wide_blue_25t", TrainOpenableWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_BLUE_BSP25T =
            new PanelReg<>("window_oc_wide_blue_bsp25t", TrainOpenableWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_WIDE_SEALED_25T =
            new PanelReg<>("window_oc_wide_sealed_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_WIDE_SEALED_BLUE_25T =
            new PanelReg<>("window_oc_wide_sealed_blue_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final SkirtReg<SkirtBlock> SKIRT_25T =
            new SkirtReg<>("skirt_25t", SkirtBlock::new)
                    .setParent(GROUP_C25T);

    public static final SkirtReg<SkirtBlock> SKIRT_MARSHALLED_25T =
            new SkirtReg<>("skirt_marshalled_25t", SkirtBlock::new)
                    .setParent(GROUP_C25T);

    public static final SlabReg<LadderBlock> LADDER_SLIDING_25T =
            new SlabReg<>("ladder_sliding_25t", LadderBlock::new)
                    .setParent(GROUP_C25T);

    public static final SlabReg<SlabBlock> FLOOR_25T =
            new SlabReg<>("floor_25t", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25T);

    public static final SlabReg<SlabBlock> TOILET_DD_25T =
            new SlabReg<>("toilet_dd_25t", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25T);

    public static final SlabReg<SlabBlock> CARPORT_25T =
            new SlabReg<>("carport_25t", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25T);

    public static final SlabReg<SlabBlock> CARPORT_CENTER_BSP25T =
            new SlabReg<>("carport_center_bsp25t", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25T);

    public static final SlabReg<SlabBlock> CARPORT_TOILET_25T =
            new SlabReg<>("carport_toilet_25t", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25T);

    public static final SlabReg<SlabBlock> AIR_CONDITION_BSP25T =
            new SlabReg<>("air_condition_bsp25t", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25T);
}
