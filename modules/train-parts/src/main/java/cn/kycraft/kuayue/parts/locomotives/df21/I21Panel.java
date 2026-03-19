package cn.kycraft.kuayue.parts.locomotives.df21;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
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
public class I21Panel {
    public static final RegistryGroup GROUP_DF21 =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block.noOcclusion().strength(1.5f, 3f).mapColor(DyeColor.RED))
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final BlockReg<DF21HeadBlock> HEAD_DF21 =
            BlockReg.of("head_df21", DF21HeadBlock::new).setParent(GROUP_DF21);

    public static final BlockReg<DF21HeadEarlyBlock> HEAD_EARLY_DF21 =
            BlockReg.of("head_early_df21", DF21HeadEarlyBlock::new).setParent(GROUP_DF21);

    public static final SlabReg<TrainPanelShapes.CarportHingeSlabBlock> EXHAUST_FAN_DF21 =
            new SlabReg<>("exhaust_fan_df21", p -> new TrainPanelShapes.CarportHingeSlabBlock(p, true)).setParent(GROUP_DF21);

    public static final SlabReg<TrainPanelShapes.CarportHingeSlabBlock> DYNAMIC_CARPORT_DF21 =
            new SlabReg<>("dynamic_carport_df21", p -> new TrainPanelShapes.CarportHingeSlabBlock(p, true)).setParent(GROUP_DF21);

    public static final SlabReg<TrainPanelShapes.CarportHingeSlabBlock> DYNAMIC_TRANSITION_CARPORT_DF21 =
            new SlabReg<>("dynamic_transition_carport_df21", p -> new TrainPanelShapes.CarportHingeSlabBlock(p, true)).setParent(GROUP_DF21);

    public static final SlabReg<TrainPanelShapes.CarportHingeSlabBlock> DYNAMIC_RADIATOR_DF21 =
            new SlabReg<>("dynamic_radiator_df21", p -> new TrainPanelShapes.CarportHingeSlabBlock(p, true)).setParent(GROUP_DF21);

    public static final SlabReg<TrainPanelShapes.CarportHingeSlabBlock> GENERAL_CARPORT_DF21 =
            new SlabReg<>("general_carport_df21", p -> new TrainPanelShapes.CarportHingeSlabBlock(p, true)).setParent(GROUP_DF21);

    public static final SlabReg<SlabBlock> GENERAL_SLAB_DF21 =
            new SlabReg<>("general_slab_df21", p -> new SlabBlock(p, false)).setParent(GROUP_DF21);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_CABIN_DF21 =
            new PanelReg<>("door_cabin_df21", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_cabin_door_bottom_left"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_cabin_door_upper_left")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_cabin_door_bottom_right"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_cabin_door_upper_right")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            )).setParent(GROUP_DF21);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_EQUIP_DF21 =
            new PanelReg<>("door_equip_df21", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_equip_door_bottom_left"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_equip_door_upper_left")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_equip_door_bottom_right"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "df21/door/df21_equip_door_upper_right")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            )).setParent(GROUP_DF21);

    public static final PanelReg<TrainUnOpenableWindowBlock> WINDOW_EQUIP_DF21 =
            new PanelReg<>("window_equip_df21", p -> new TrainUnOpenableWindowBlock(p, -1, 2)).setParent(GROUP_DF21);

    public static final PanelReg<TrainUnOpenableWindowBlock> EQUIP_MIDDLE_EARLY_DF21 =
            new PanelReg<>("equip_middle_early_df21", p -> new TrainUnOpenableWindowBlock(p, 1, 0, 2)).setParent(GROUP_DF21);

    public static final PanelReg<TrainHingePanelBlock> GENERAL_BOTTOM_SLAB_DF21 =
            new PanelReg<>("general_bottom_slab_df21", p -> new TrainHingePanelBlock(p, new Vec2(0, 0), new Vec2(1, 1))).setParent(GROUP_DF21);

    public static final BlockReg<FullShapeDirectionalBlock> FUEL_TANK_DF21 =
            BlockReg.of("fuel_tank_df21", FullShapeDirectionalBlock::new).setParent(GROUP_DF21);
}
