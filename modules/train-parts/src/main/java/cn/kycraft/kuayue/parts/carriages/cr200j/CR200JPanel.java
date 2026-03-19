package cn.kycraft.kuayue.parts.carriages.cr200j;

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
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.RenderShape;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class CR200JPanel {

    public static final RegistryGroup GROUP_CR200J =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block
                            .noOcclusion()
                            .strength(1.5f, 3f)
                            .mapColor(DyeColor.GREEN)
                    )
            ).configure(
                    ItemRegConfigurations.adopt(
                            item -> item.stacksTo(8)
                                    .tabTo(TRAIN_PANEL_TAB)
                    )
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_CR200J =
            new PanelReg<>("door_marshalled_cr200j", p ->
                    new CustomRenderedDoorBlock(
                            p,
                            Couple.create(
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_bottom_lh"),
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_top_lh")
                            ),
                            Couple.create(
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_bottom"),
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_top")
                            ),
                            RenderShape.ENTITYBLOCK_ANIMATED,
                            false
                    )
            ).setParent(GROUP_CR200J);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_CR200J_2 =
            new PanelReg<>("door_marshalled_cr200j_2", p ->
                    new CustomRenderedDoorBlock(
                            p,
                            Couple.create(
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_bottom_lh_2"),
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_top_lh")
                            ),
                            Couple.create(
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_bottom_2"),
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_top")
                            ),
                            RenderShape.ENTITYBLOCK_ANIMATED,
                            true
                    )
            ).setParent(GROUP_CR200J);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_CABIN_MARSHALLED_CR200J =
            new PanelReg<>("door_cabin_marshalled_cr200j", p ->
                    new CustomRenderedDoorBlock(
                            p,
                            Couple.create(
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_bottom_lh"),
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_top_lh_2")
                            ),
                            Couple.create(
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_bottom"),
                                    ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_marshalled_cr200j/door/door_top_2")
                            ),
                            RenderShape.ENTITYBLOCK_ANIMATED,
                            true
                    )
            ).setParent(GROUP_CR200J);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_MARSHALLED_CR200J =
            new PanelReg<>("end_face_marshalled_cr200j", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.SLIDE,
                    "carriage/carriage_marshalled_cr200j/end_face/end_face_door_lh",
                    "carriage/carriage_marshalled_cr200j/end_face/end_face_door",
                    "carriage/carriage_marshalled_cr200j/end_face/end_face_frame"
            ))
                    .setParent(GROUP_CR200J);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_MARSHALLED_CR200J_2 =
            new PanelReg<>("end_face_marshalled_cr200j_2", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.ROTATE,
                    "carriage/carriage_marshalled_cr200j/end_face/end_face_door_lh",
                    "carriage/carriage_marshalled_cr200j/end_face/end_face_door",
                    "carriage/carriage_marshalled_cr200j/end_face/end_face_frame"
            ))
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_MARSHALLED_CR200J =
            new PanelReg<>("panel_bottom_marshalled_cr200j", TrainPanelBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainHingePanelBlock> PANEL_MIDDLE_LOGO_CR_MARSHALLED_CR200J =
            new PanelReg<>("panel_middle_logo_cr_marshalled_cr200j", TrainHingePanelBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_SEALED_MARSHALLED_CR200J =
            new PanelReg<>("window_sealed_marshalled_cr200j", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainHingePanelBlock> PANEL_MIDDLE_MARSHALLED_CR200J =
            new PanelReg<>("panel_middle_marshalled_cr200j", TrainHingePanelBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_MARSHALLED_CR200J =
            new PanelReg<>("panel_top_marshalled_cr200j", TrainPanelBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainHingePanelBlock> PANEL_TOP_MARSHALLED_CR200J_2 =
            new PanelReg<>("panel_top_marshalled_cr200j_2", TrainHingePanelBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_MARSHALLED_CR200J_3 =
            new PanelReg<>("panel_top_marshalled_cr200j_3", TrainPanelBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainHingePanelBlock> PANEL_WIDE_FU_XING_MARSHALLED_CR200J =
            new PanelReg<>("panel_wide_fu_xing_marshalled_cr200j", p -> new TrainHingePanelBlock(p, new net.minecraft.world.phys.Vec2(-1, 0), new net.minecraft.world.phys.Vec2(2, 1)))
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainHingePanelBlock> PANEL_WIDE_LOGO_CR_MARSHALLED_CR200J =
            new PanelReg<>("panel_wide_logo_cr_marshalled_cr200j", p -> new TrainHingePanelBlock(p, new net.minecraft.world.phys.Vec2(-1, 0), new net.minecraft.world.phys.Vec2(2, 2)))
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainHingePanelBlock> PANEL_WIDE_MARSHALLED_CR200J =
            new PanelReg<>("panel_wide_marshalled_cr200j", p -> new TrainHingePanelBlock(p, new net.minecraft.world.phys.Vec2(-1, 0), new net.minecraft.world.phys.Vec2(2, 2)))
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_TOILET_MARSHALLED_CR200J =
            new PanelReg<>("window_toilet_marshalled_cr200j", TrainSmallWindowBlock::new)
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_WIDE_MARSHALLED_CR200J =
            new PanelReg<>("window_wide_marshalled_cr200j", p -> new TrainOpenableWindowBlock(p, 2))
                    .setParent(GROUP_CR200J);

    public static final PanelReg<TrainUnOpenableWindowBlock> WINDOW_WIDE_SEALED_MARSHALLED_CR200J =
            new PanelReg<>("window_wide_sealed_marshalled_cr200j", p -> new TrainUnOpenableWindowBlock(p, 2))
                    .setParent(GROUP_CR200J);

    public static final SkirtReg<SkirtBlock> SKIRT_MARSHALLED_CR200J =
            new SkirtReg<>("skirt_marshalled_cr200j", SkirtBlock::new)
                    .setParent(GROUP_CR200J);

    public static final SlabReg<LadderBlock> LADDER_MARSHALLED_CR200J =
            new SlabReg<>("ladder_marshalled_cr200j", LadderBlock::new)
                    .setParent(GROUP_CR200J);

    public static final SlabReg<SlabBlock> FLOOR_MARSHALLED_CR200J =
            new SlabReg<>("floor_marshalled_cr200j", p -> new SlabBlock(p, false))
                    .setParent(GROUP_CR200J);

    public static final SlabReg<SlabBlock> TOILET_DD_MARSHALLED_CR200J =
            new SlabReg<>("toilet_dd_marshalled_cr200j", p -> new SlabBlock(p, false))
                    .setParent(GROUP_CR200J);

    public static final SlabReg<SlabBlock> CARPORT_CENTER_MARSHALLED_CR200J =
            new SlabReg<>("carport_center_marshalled_cr200j", p -> new SlabBlock(p, true))
                    .setParent(GROUP_CR200J);

    public static final SlabReg<SlabBlock> CARPORT_MARSHALLED_CR200J =
            new SlabReg<>("carport_marshalled_cr200j", p -> new SlabBlock(p, true))
                    .setParent(GROUP_CR200J);
}
