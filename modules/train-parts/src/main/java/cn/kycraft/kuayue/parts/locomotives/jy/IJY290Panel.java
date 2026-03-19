package cn.kycraft.kuayue.parts.locomotives.jy;

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
public class IJY290Panel {

    public static final RegistryGroup GROUP_JY290 =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block.noOcclusion().strength(1.5f, 3f).mapColor(DyeColor.RED))
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final BlockReg<FullShapeDirectionalBlock> JY290_HEAD_2 =
            BlockReg.of("jy290_head_2", FullShapeDirectionalBlock::new).setParent(GROUP_JY290);
    public static final BlockReg<FullShapeDirectionalBlock> JY290_HEAD_1 =
            BlockReg.of("jy290_head_1", FullShapeDirectionalBlock::new).setParent(GROUP_JY290);
    public static final BlockReg<JY290CowCatcherBlock> JY290_COWCATCHER =
            BlockReg.of("jy290_cowcatcher", JY290CowCatcherBlock::new).setParent(GROUP_JY290);

    public static final SlabReg<SlabBlock> JY290_FLOOR =
            new SlabReg<>("jy290_floor", p -> new SlabBlock(p, false)).setParent(GROUP_JY290);
    public static final SlabReg<SlabBlock> JY290_FLOOR_LADDER =
            new SlabReg<>("jy290_floor_ladder", p -> new SlabBlock(p, false)).setParent(GROUP_JY290);
    public static final SlabReg<SlabBlock> JY290_CARPORT =
            new SlabReg<>("jy290_carport", p -> new SlabBlock(p, true)).setParent(GROUP_JY290);

    public static final BlockReg<FullShapeDirectionalBlock> JY290_EQUIPMENT =
            BlockReg.of("jy290_equipment", FullShapeDirectionalBlock::new).setParent(GROUP_JY290);

    public static final PanelReg<TrainHingePanelBlock> JY290_PANEL =
            new PanelReg<>("jy290_panel", p -> new TrainHingePanelBlock(p, new Vec2(-1, 0), new Vec2(1, 2))).setParent(GROUP_JY290);
    public static final PanelReg<TrainHingePanelBlock> JY290_PANEL_LOGO =
            new PanelReg<>("jy290_panel_logo", p -> new TrainHingePanelBlock(p, new Vec2(-1, 0), new Vec2(1, 2))).setParent(GROUP_JY290);

    public static final BlockReg<JY290ACBlock> JY290_AC =
            BlockReg.of("jy290_ac", JY290ACBlock::new).setParent(GROUP_JY290);

    public static final SlabReg<SlabBlock> JY290_VENT =
            new SlabReg<>("jy290_vent", p -> new SlabBlock(p, true)).setParent(GROUP_JY290);

    public static final PanelReg<JY290DoorBlock> JY290_DOOR =
            new PanelReg<>("jy290_door", p -> new JY290DoorBlock(p, new Vec2(0, 0), new Vec2(1, 1))).setParent(GROUP_JY290);
}
