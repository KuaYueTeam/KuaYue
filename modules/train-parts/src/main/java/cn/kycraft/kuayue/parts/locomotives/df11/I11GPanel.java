package cn.kycraft.kuayue.parts.locomotives.df11;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockReg;
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
public class I11GPanel {

    public static final RegistryGroup GROUP_DF11G =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block.noOcclusion().strength(1.5f, 3f).mapColor(DyeColor.GREEN))
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_CABIN_DF11G =
            new PanelReg<>("door_cabin_df11g", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_cabin_bottom_df11g_right"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_cabin_upper_df11g_right")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_cabin_bottom_df11g_left"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_cabin_upper_df11g_left")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            )).setParent(GROUP_DF11G);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_EQUIP_ROOM_DF11G =
            new PanelReg<>("door_equip_room_df11g", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_equip_room_bottom_df11g_right"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_equip_room_upper_df11g_right")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_equip_room_bottom_df11g_left"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11g/door/door_equip_room_upper_df11g_left")
                    ),
                    new Vec3(-.1875, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.MODEL,
                    false
            )).setParent(GROUP_DF11G);

    public static final PanelReg<CustomRenderedEndFaceBlock> END_FACE_DF11G =
            new PanelReg<>("end_face_df11g", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.NO_DOOR,
                    null,
                    null,
                    "df11g/df11g_end_face"
            )).setParent(GROUP_DF11G);

    public static final BlockReg<DF11GCowCatcherBlock> COWCATCHER_DF11G =
            BlockReg.of("cowcatcher_df11g", DF11GCowCatcherBlock::new).setParent(GROUP_DF11G);
    public static final BlockReg<FullShapeDirectionalBlock> HEAD_DF11G_2 =
            BlockReg.of("head_df11g_2", FullShapeDirectionalBlock::new).setParent(GROUP_DF11G);
    public static final BlockReg<FullShapeDirectionalBlock> DF11G_TANK =
            BlockReg.of("df11g_tank", FullShapeDirectionalBlock::new).setParent(GROUP_DF11G);

    public static final SlabReg<SlabBlock> FLOOR_DF11G =
            new SlabReg<>("floor_df11g", p -> new SlabBlock(p, false)).setParent(GROUP_DF11G);

    public static final SlabReg<SlabBlock> DF11G_CARPORT =
            new SlabReg<>("df11g_carport", p -> new SlabBlock(p, true)).setParent(GROUP_DF11G);
    public static final SlabReg<SlabBlock> DF11G_CARPORT_CENTRE =
            new SlabReg<>("df11g_carport_center", p -> new SlabBlock(p, false)).setParent(GROUP_DF11G);
    public static final SlabReg<HingeSlabBlock> DF11G_CARPORT_CENTRE_COOLING_BLIND =
            new SlabReg<>("df11g_carport_center_cooling_blind", p -> new HingeSlabBlock(p, false)).setParent(GROUP_DF11G);
    public static final SlabReg<SlabBlock> DF11G_CARPORT_CENTRE_FAN_BLIND =
            new SlabReg<>("df11g_carport_center_fan_blind", p -> new SlabBlock(p, false)).setParent(GROUP_DF11G);
    public static final SlabReg<HingeSlabBlock> DF11G_CARPORT_EH =
            new SlabReg<>("df11g_carport_eh", p -> new HingeSlabBlock(p, true)).setParent(GROUP_DF11G);
    public static final SlabReg<HingeSlabBlock> DF11G_CARPORT_EH_AD =
            new SlabReg<>("df11g_carport_eh_ad", p -> new HingeSlabBlock(p, true)).setParent(GROUP_DF11G);
    public static final SlabReg<SlabBlock> DF11G_CARPORT_EH_BLIND =
            new SlabReg<>("df11g_carport_eh_blind", p -> new SlabBlock(p, true)).setParent(GROUP_DF11G);
    public static final SlabReg<SlabBlock> DF11G_CARPORT_FILTER_BLIND =
            new SlabReg<>("df11g_carport_filter_blind", p -> new SlabBlock(p, true)).setParent(GROUP_DF11G);
    public static final SlabReg<SlabBlock> DF11G_CARPORT_KUA =
            new SlabReg<>("df11g_carport_kua", p -> new SlabBlock(p, true)).setParent(GROUP_DF11G);
    public static final SlabReg<SlabBlock> DF11G_CARPORT_YUE =
            new SlabReg<>("df11g_carport_yue", p -> new SlabBlock(p, true)).setParent(GROUP_DF11G);
    public static final SlabReg<DF11GChimneyBlock> DF11G_CHIMNEY =
            new SlabReg<>("df11g_chimney", p -> new DF11GChimneyBlock(p, false)).setParent(GROUP_DF11G);

    public static final PanelReg<TrainPanelBlock> PANEL_KUAYUE_DF11G =
            new PanelReg<>("panel_kuayue_df11g", p -> new TrainPanelBlock(p, new Vec2(-1, 0), new Vec2(2, 1))).setParent(GROUP_DF11G);
    public static final PanelReg<TrainPanelBlock> PANEL_TOP_DF11G =
            new PanelReg<>("panel_top_df11g", TrainPanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_DF11G =
            new PanelReg<>("panel_bottom_df11g", TrainPanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainPanelBlock> ROAD_SIGN_DF11G =
            new PanelReg<>("road_sign_df11g", p -> new TrainPanelBlock(p, new Vec2(-1, 0), new Vec2(2, 1))).setParent(GROUP_DF11G);
    public static final PanelReg<TrainPanelBlock> WINDOW_EQUIP_ROOM_DF11G_1 =
            new PanelReg<>("window_equip_room_df11g_1", p -> new TrainPanelBlock(p, new Vec2(0, 0), new Vec2(1, 2))).setParent(GROUP_DF11G);
    public static final PanelReg<TrainPanelBlock> WINDOW_EQUIP_ROOM_DF11G_2 =
            new PanelReg<>("window_equip_room_df11g_2", TrainPanelBlock::new).setParent(GROUP_DF11G);

    public static final PanelReg<TrainHingePanelBlock> WINDOW_PR_DF11G =
            new PanelReg<>("window_pr_df11g", TrainHingePanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainHingePanelBlock> LOUVER_EQUIP_ROOM_DF11G_2_TOP =
            new PanelReg<>("louver_equip_room_df11g_2_top", TrainHingePanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainHingePanelBlock> LOUVER_EQUIP_ROOM_DF11G_2_BOTTOM =
            new PanelReg<>("louver_equip_room_df11g_2_bottom", TrainHingePanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainHingePanelBlock> LOUVER_EQUIP_ROOM_DF11G_1 =
            new PanelReg<>("louver_equip_room_df11g_1", TrainHingePanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainHingePanelBlock> DF11G_PANEL_BOTTOM_EH_BLIND =
            new PanelReg<>("df11g_panel_bottom_eh_blind", TrainHingePanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainHingePanelBlock> DF11G_PANEL_UPPER_PH_BLIND =
            new PanelReg<>("df11g_panel_upper_ph_blind", TrainHingePanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainHingePanelBlock> DF11G_PANEL_BOTTOM_PH_BLIND =
            new PanelReg<>("df11g_panel_bottom_ph_blind", TrainHingePanelBlock::new).setParent(GROUP_DF11G);

    public static final PanelReg<TrainPanelBlock> DF11G_PANEL_BOTTOM_BLIND =
            new PanelReg<>("df11g_panel_bottom_blind", TrainPanelBlock::new).setParent(GROUP_DF11G);
    public static final PanelReg<TrainPanelBlock> DF11G_PANEL_UPPER_BLIND =
            new PanelReg<>("df11g_panel_upper_blind", TrainPanelBlock::new).setParent(GROUP_DF11G);

    public static final PanelReg<TrainUnOpenableWindowBlock> DF11G_PANEL_BOTTOM_COOLING_BLIND =
            new PanelReg<>("df11g_panel_bottom_cooling_blind", p -> new TrainUnOpenableWindowBlock(p, 2)).setParent(GROUP_DF11G);
    public static final PanelReg<TrainUnOpenableWindowBlock> DF11G_WINDOWS_PH_EH =
            new PanelReg<>("df11g_windows_ph_eh", p -> new TrainUnOpenableWindowBlock(p, 2)).setParent(GROUP_DF11G);
    public static final PanelReg<TrainUnOpenableWindowBlock> DF11G_PANEL_UPPER_COOLING_BLIND =
            new PanelReg<>("df11g_panel_upper_cooling_blind", p -> new TrainUnOpenableWindowBlock(p, 2)).setParent(GROUP_DF11G);
}
