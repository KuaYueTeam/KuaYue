package cn.kycraft.kuayue.parts.locomotives.hxd3d;

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
public class I3DPanel {

    public static final RegistryGroup GROUP_HXD3D =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block.noOcclusion().strength(1.5f, 3f).mapColor(DyeColor.RED))
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_CABIN_HXD3D =
            new PanelReg<>("door_cabin_hxd3d", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "hxd3d/door/door_bottom_hxd3d_left"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "hxd3d/door/door_upper_hxd3d_left")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "hxd3d/door/door_bottom_hxd3d_right"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "hxd3d/door/door_upper_hxd3d_right")
                    ),
                    new Vec3(-.3125, .25, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.MODEL,
                    false
            )).setParent(GROUP_HXD3D);

    public static final BlockReg<HeightSlabBlock> FLOOR_HXD3D =
            BlockReg.of("floor_hxd3d", HeightSlabBlock::new).setParent(GROUP_HXD3D);
    public static final BlockReg<FullShapeDirectionalBlock> HEAD_HXD3D =
            BlockReg.of("head_hxd3d", FullShapeDirectionalBlock::new).setParent(GROUP_HXD3D);
    public static final BlockReg<HXD3DCowCatcherBlock> HXD3D_COWCATCHER =
            BlockReg.of("hxd3d_cowcatcher", HXD3DCowCatcherBlock::new).setParent(GROUP_HXD3D);
    public static final BlockReg<SingleArmPantographBlock> HXD3D_PANTOGRAPH =
            BlockReg.of("hxd3d_pantograph", SingleArmPantographBlock::new).setParent(GROUP_HXD3D);

    public static final SlabReg<SlabBlock> HXD3D_CARPORT =
            new SlabReg<>("hxd3d_carport", p -> new SlabBlock(p, true)).setParent(GROUP_HXD3D);
    public static final SlabReg<SlabBlock> HXD3D_CARPORT_CENTRE =
            new SlabReg<>("hxd3d_carport_center", p -> new SlabBlock(p, true)).setParent(GROUP_HXD3D);
    public static final SlabReg<SlabBlock> HXD3D_UNDERBODY_EQUIP =
            new SlabReg<>("hxd3d_underbody_equip", p -> new SlabBlock(p, false)).setParent(GROUP_HXD3D);

    public static final PanelReg<TrainPanelBlock> PANEL_HEXIE_HXD3D =
            new PanelReg<>("panel_hexie_hxd3d", p -> new TrainPanelBlock(p, new Vec2(-1, 0), new Vec2(2, 2))).setParent(GROUP_HXD3D);
    public static final PanelReg<TrainPanelBlock> HXD3D_PANEL_BOTTOM =
            new PanelReg<>("hxd3d_panel_bottom", TrainPanelBlock::new).setParent(GROUP_HXD3D);
    public static final PanelReg<TrainPanelBlock> PANEL_LOGO_NUM_HXD3D =
            new PanelReg<>("panel_logo_num_hxd3d", p -> new TrainPanelBlock(p, new Vec2(-1, 0), new Vec2(2, 2))).setParent(GROUP_HXD3D);
    public static final PanelReg<TrainPanelBlock> HXD3D_PANEL_MIDDLE =
            new PanelReg<>("hxd3d_panel_middle", TrainPanelBlock::new).setParent(GROUP_HXD3D);

    public static final PanelReg<TrainHingePanelBlock> HXD3D_PANEL_TRANSIT =
            new PanelReg<>("hxd3d_panel_transit", p -> new TrainHingePanelBlock(p, new Vec2(0, 0), new Vec2(1, 2))).setParent(GROUP_HXD3D);
    public static final PanelReg<TrainHingePanelBlock> HXD3D_PANEL_TRANSIT_2 =
            new PanelReg<>("hxd3d_panel_transit_2", TrainHingePanelBlock::new).setParent(GROUP_HXD3D);
}
