package cn.kycraft.kuayue.parts.locomotives.df11;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
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
public class I11Panel {

    public static final RegistryGroup GROUP_DF11 =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block
                            .noOcclusion()
                            .strength(1.5f, 3f)
                            .mapColor(DyeColor.BLUE)
                    )
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final BlockReg<DF11CowCatcherBlock> DF11_COWCATCHER =
            BlockReg.of("df11_cowcatcher", DF11CowCatcherBlock::new).setParent(GROUP_DF11);

    public static final BlockReg<FullShapeDirectionalBlock> DF11_HEAD =
            BlockReg.of("df11_head", FullShapeDirectionalBlock::new).setParent(GROUP_DF11);

    public static final SlabReg<SlabBlock> DF11_FLOOR =
            new SlabReg<>("df11_floor", p -> new SlabBlock(p, false)).setParent(GROUP_DF11);

    public static final SlabReg<HingeSlabBlock> DF11_CARPORT_EQUIP_B =
            new SlabReg<>("df11_carport_equip_b", p -> new HingeSlabBlock(p, true)).setParent(GROUP_DF11);

    public static final SlabReg<HingeSlabBlock> DF11_CARPORT_EQUIP_A =
            new SlabReg<>("df11_carport_equip_a", p -> new HingeSlabBlock(p, true)).setParent(GROUP_DF11);

    public static final SlabReg<HingeSlabBlock> DF11_CARPORT_CENTER_COOLING_FAN =
            new SlabReg<>("df11_carport_center_cooling_fan", p -> new HingeSlabBlock(p, false)).setParent(GROUP_DF11);

    public static final SlabReg<SlabBlock> DF11_CARPORT_GENERAL =
            new SlabReg<>("df11_carport_general", p -> new SlabBlock(p, true)).setParent(GROUP_DF11);

    public static final SlabReg<SlabBlock> DF11_CARPORT_EQUIP_BLINDS =
            new SlabReg<>("df11_carport_equip_blinds", p -> new SlabBlock(p, true)).setParent(GROUP_DF11);

    public static final SlabReg<SlabBlock> DF11_CARPORT_LOUVER_FAN =
            new SlabReg<>("df11_carport_louver_fan", p -> new SlabBlock(p, false)).setParent(GROUP_DF11);

    public static final SlabReg<DF11ChimneyBlock> DF11_CHIMNEY =
            new SlabReg<>("df11_chimney", p -> new DF11ChimneyBlock(p, false)).setParent(GROUP_DF11);

    public static final BlockReg<FullShapeDirectionalBlock> DF11_RESERVOIR_TANK =
            BlockReg.of("df11_reservoir_tank", FullShapeDirectionalBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<CustomRenderedDoorBlock> DF11_DOOR =
            new PanelReg<>("df11_door", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11/door/df11_door_bottom_left"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11/door/df11_door_upper_left")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11/door/df11_door_bottom_right"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df11/door/df11_door_upper_right")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            )).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> DF11_PANEL_AIR_INTAKE =
            new PanelReg<>("df11_panel_air_intake", p -> new TrainPanelBlock(p, new Vec2(-1, 0), new Vec2(2, 1))).setParent(GROUP_DF11);

    public static final PanelReg<TrainHingePanelBlock> DF11_PANEL_EQUIP_BOTTOM =
            new PanelReg<>("df11_panel_equip_bottom", TrainHingePanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainHingePanelBlock> DF11_WINDOW_EQUIP_SMALL =
            new PanelReg<>("df11_window_equip_small", TrainHingePanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_DF11 =
            new PanelReg<>("panel_bottom_df11", TrainPanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_DF11 =
            new PanelReg<>("panel_top_df11", TrainPanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainHingePanelBlock> DF11_PANEL_EQUIP_TOP =
            new PanelReg<>("df11_panel_equip_top", TrainHingePanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> DF11_PANEL_BLIND_TOP =
            new PanelReg<>("df11_panel_blind_top", TrainPanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> DF11_PANEL_BLIND_BOTTOM =
            new PanelReg<>("df11_panel_blind_bottom", TrainPanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> DF11_ROAD_SIGN =
            new PanelReg<>("df11_road_sign", p -> new TrainPanelBlock(p, new Vec2(-1, 0), new Vec2(2, 1))).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> DF11_EQUIP_ROOM_WINDOW_1 =
            new PanelReg<>("df11_equip_room_window_1", TrainPanelBlock::new).setParent(GROUP_DF11);

    public static final PanelReg<TrainPanelBlock> DF11_EQUIP_ROOM_WINDOW_2 =
            new PanelReg<>("df11_equip_room_window_2", TrainPanelBlock::new).setParent(GROUP_DF11);
}
