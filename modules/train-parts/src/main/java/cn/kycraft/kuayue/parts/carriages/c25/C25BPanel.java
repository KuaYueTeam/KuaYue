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
public class C25BPanel {

    public static final RegistryGroup GROUP_C25B =
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

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_25B =
            new PanelReg<>("panel_bottom_25b", TrainPanelBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainPanelBlock> PANEL_MIDDLE_25B =
            new PanelReg<>("panel_middle_25b", TrainPanelBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainHingePanelBlock> PANEL_SYMBOL_MARSHALLED_25B =
            new PanelReg<>("panel_symbol_marshalled_25b", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_25B =
            new PanelReg<>("panel_top_25b", TrainPanelBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_25B_2 =
            new PanelReg<>("panel_top_25b_2", TrainPanelBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_25B =
            new PanelReg<>("window_oc_25b", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_25B =
            new PanelReg<>("window_oc_small_25b", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_25B =
            new PanelReg<>("window_oc_toilet_25b", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_25B =
            new PanelReg<>("window_oc_wide_25b", p -> new TrainOpenableWindowBlock(p, 2))
                    .setParent(GROUP_C25B);

    public static final PanelReg<TrainUnOpenableWindowBlock> WINDOW_OC_WIDE_SEALED_25B =
            new PanelReg<>("window_oc_wide_sealed_25b", p -> new TrainUnOpenableWindowBlock(p, 2))
                    .setParent(GROUP_C25B);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_25B =
            new PanelReg<>("door_25b", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25b_door_bottom_hinge"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25b_door_top_hinge")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25b_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "door/original_25b_door_top")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, -.124),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            ))
                    .setParent(GROUP_C25B);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25B_1 =
            new PanelReg<>("end_face_25b_1", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25b/end_face/end_face_door_original_25b",
                    null,
                    "carriage/carriage25b/end_face/end_face_original_25b_1"
            ))
                    .setParent(GROUP_C25B);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25B_2 =
            new PanelReg<>("end_face_25b_2", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25b/end_face/end_face_door_original_25b",
                    null,
                    "carriage/carriage25b/end_face/end_face_original_25b_2"
            ))
                    .setParent(GROUP_C25B);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_25B_3 =
            new PanelReg<>("end_face_25b_3", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                    "carriage/carriage25b/end_face/end_face_door_original_25b",
                    null,
                    "carriage/carriage25b/end_face/end_face_original_25b_3"
            ))
                    .setParent(GROUP_C25B);

    public static final SlabReg<SlabBlock> FLOOR_25B =
            new SlabReg<>("floor_25b", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25B);

    public static final SlabReg<SlabBlock> TOILET_DD_25B =
            new SlabReg<>("toilet_dd_25b", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25B);

    public static final SlabReg<LadderBlock> LADDER_25B =
            new SlabReg<>("ladder_25b", LadderBlock::new)
                    .setParent(GROUP_C25B);
}
