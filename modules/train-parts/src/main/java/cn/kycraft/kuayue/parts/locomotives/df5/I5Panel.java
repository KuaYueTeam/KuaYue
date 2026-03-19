package cn.kycraft.kuayue.parts.locomotives.df5;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec2;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class I5Panel {

    public static final RegistryGroup GROUP_DF5 =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block.noOcclusion().strength(1.5f, 3f).mapColor(DyeColor.BLUE))
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final BlockReg<FullShapeDirectionalBlock> DF5_END_FACE_1 =
            BlockReg.of("df5_end_face_1", FullShapeDirectionalBlock::new).setParent(GROUP_DF5);
    public static final BlockReg<FullShapeDirectionalBlock> DF5_END_FACE_2 =
            BlockReg.of("df5_end_face_2", FullShapeDirectionalBlock::new).setParent(GROUP_DF5);
    public static final BlockReg<DF5CowCatcherBlock> DF5_COWCATCHER =
            BlockReg.of("df5_cowcatcher", DF5CowCatcherBlock::new).setParent(GROUP_DF5);
    public static final BlockReg<FullShapeDirectionalBlock> DF5_FUEL_TANK =
            BlockReg.of("df5_fuel_tank", FullShapeDirectionalBlock::new).setParent(GROUP_DF5);

    public static final SlabReg<SlabBlock> DF5_FLOOR =
            new SlabReg<>("df5_floor", p -> new SlabBlock(p, false)).setParent(GROUP_DF5);
    public static final SlabReg<SlabBlock> DF5_CENTER_FLOOR =
            new SlabReg<>("df5_center_floor", p -> new SlabBlock(p, false)).setParent(GROUP_DF5);
    public static final SlabReg<HingeSlabBlock> DF5_TRANSIT_FLOOR =
            new SlabReg<>("df5_transit_floor", p -> new HingeSlabBlock(p, false)).setParent(GROUP_DF5);
    public static final SlabReg<DF5ChimneyBlock> DF5_CHIMNEY =
            new SlabReg<>("df5_chimney", p -> new DF5ChimneyBlock(p, false)).setParent(GROUP_DF5);
    public static final SlabReg<HingeSlabBlock> DF5_CARPORT_CABIN =
            new SlabReg<>("df5_carport_cabin", p -> new HingeSlabBlock(p, false)).setParent(GROUP_DF5);
    public static final SlabReg<SlabBlock> DF5_COOLING_FAN =
            new SlabReg<>("df5_cooling_fan", p -> new SlabBlock(p, true)).setParent(GROUP_DF5);

    public static final PanelReg<TrainPanelBlock> DF5_HANDRAIL =
            new PanelReg<>("df5_handrail", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_PANEL_BOTTOM =
            new PanelReg<>("df5_panel_bottom", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_PANEL_TOP =
            new PanelReg<>("df5_panel_top", TrainPanelBlock::new).setParent(GROUP_DF5);

    public static final PanelReg<TrainPanelBlock> DF5_CARPORT_1 =
            new PanelReg<>("df5_carport_1", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_CARPORT_2 =
            new PanelReg<>("df5_carport_2", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainHingePanelBlock> DF5_CARPORT_3 =
            new PanelReg<>("df5_carport_3", TrainHingePanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainHingePanelBlock> DF5_CARPORT_4 =
            new PanelReg<>("df5_carport_4", TrainHingePanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_CARPORT_5 =
            new PanelReg<>("df5_carport_5", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_CARPORT_GRILLE =
            new PanelReg<>("df5_carport_grille", TrainPanelBlock::new).setParent(GROUP_DF5);

    public static final PanelReg<TrainPanelBlock> DF5_AIR_INTAKE =
            new PanelReg<>("df5_air_intake", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_AIR_INTAKE_2 =
            new PanelReg<>("df5_air_intake_2", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_AIR_INTAKE_3 =
            new PanelReg<>("df5_air_intake_3", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_RADIATOR_GRID =
            new PanelReg<>("df5_radiator_grid", p -> new TrainPanelBlock(p, new Vec2(0, 0), new Vec2(1, 2))).setParent(GROUP_DF5);

    public static final PanelReg<TrainHingePanelBlock> DF5_ENGINE_ACCESS_DOOR =
            new PanelReg<>("df5_engine_access_door", TrainHingePanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainHingePanelBlock> DF5_EQUIP_DOOR_1 =
            new PanelReg<>("df5_equip_door_1", TrainHingePanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainHingePanelBlock> DF5_EQUIP_DOOR_2 =
            new PanelReg<>("df5_equip_door_2", TrainHingePanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainHingePanelBlock> DF5_CABIN_DOOR =
            new PanelReg<>("df5_cabin_door", TrainHingePanelBlock::new).setParent(GROUP_DF5);

    public static final PanelReg<TrainPanelBlock> DF5_HEAD_END_FACE =
            new PanelReg<>("df5_head_end_face", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_HEAD_END_FACE_2 =
            new PanelReg<>("df5_head_end_face_2", TrainPanelBlock::new).setParent(GROUP_DF5);
    public static final PanelReg<TrainPanelBlock> DF5_HEAD_PANEL =
            new PanelReg<>("df5_head_panel", p -> new TrainPanelBlock(p, new Vec2(0, 0), new Vec2(1, 2))).setParent(GROUP_DF5);
    public static final PanelReg<TrainHingePanelBlock> DF5_COWCATCHER_LADDER =
            new PanelReg<>("df5_cowcatcher_ladder", TrainHingePanelBlock::new).setParent(GROUP_DF5);
}
