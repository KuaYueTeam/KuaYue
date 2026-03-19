package cn.kycraft.kuayue.parts.locomotives.ss;

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
public class ISS8Panel {
    public static final RegistryGroup GROUP_SS8 =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block.noOcclusion().strength(1.5f, 3f).mapColor(DyeColor.BLUE))
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final PanelReg<TrainUnOpenableWindowBlock> SS8_WINDOWS_SMALL =
            new PanelReg<>("ss8_windows_small", p -> new TrainUnOpenableWindowBlock(p, 1)).setParent(GROUP_SS8);

    public static final BlockReg<SS8CowCatcherBlock> SS8_COWCATCHER =
            BlockReg.of("ss8_cowcatcher", SS8CowCatcherBlock::new).setParent(GROUP_SS8);

    public static final SlabReg<HingeSlabBlock> SS8_CARPORT = new SlabReg<>("ss8_carport", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS8);
    public static final SlabReg<HingeSlabBlock> SS8_CARPORT_AD = new SlabReg<>("ss8_carport_ad", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS8);
    public static final SlabReg<HingeSlabBlock> SS8_CARPORT_FAN = new SlabReg<>("ss8_carport_fan", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS8);
    public static final SlabReg<HingeSlabBlock> SS8_CARPORT_LINK = new SlabReg<>("ss8_carport_link", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS8);
    public static final SlabReg<SlabBlock> SS8_FLOOR = new SlabReg<>("ss8_floor", p -> new SlabBlock(p, false)).setParent(GROUP_SS8);

    public static final PanelReg<TrainHingePanelBlock> SS8_WINDOWS_BIG = new PanelReg<>("ss8_windows_big", p -> new TrainHingePanelBlock(p, new Vec2(-1, 0), new Vec2(2, 1))).setParent(GROUP_SS8);
    public static final PanelReg<TrainHingePanelBlock> SS8_PANEL_BOTTOM_AD = new PanelReg<>("ss8_panel_bottom_ad", TrainHingePanelBlock::new).setParent(GROUP_SS8);
    public static final PanelReg<TrainHingePanelBlock> SS8_PANEL_UPPER_AD = new PanelReg<>("ss8_panel_upper_ad", TrainHingePanelBlock::new).setParent(GROUP_SS8);
    public static final PanelReg<TrainHingePanelBlock> SS8_PANEL_BOTTOM = new PanelReg<>("ss8_panel_bottom", TrainHingePanelBlock::new).setParent(GROUP_SS8);
    public static final PanelReg<TrainHingePanelBlock> SS8_PANEL_UPPER = new PanelReg<>("ss8_panel_upper", TrainHingePanelBlock::new).setParent(GROUP_SS8);

    public static final BlockReg<HeadBlock> SS8_HEAD = BlockReg.of("ss8_head", HeadBlock::new).setParent(GROUP_SS8);
    public static final BlockReg<FullShapeDirectionalBlock> SS8_TRANSFORMER = BlockReg.of("ss8_transformer", FullShapeDirectionalBlock::new).setParent(GROUP_SS8);

    public static final SlabReg<HingeSlabBlock> SS8_ROOF = new SlabReg<>("ss8_roof", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS8);
    public static final SlabReg<SlabBlock> SS8_CARPORT_CENTER = new SlabReg<>("ss8_carport_center", p -> new SlabBlock(p, true)).setParent(GROUP_SS8);
    public static final SlabReg<HingeSlabBlock> SS8_SANDBOX_FRONT = new SlabReg<>("ss8_sandbox_front", p -> new HingeSlabBlock(p, false)).setParent(GROUP_SS8);

    public static final PanelReg<CustomRenderedDoorBlock> SS8_DOOR =
            new PanelReg<>("ss8_door", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss8/ss8_door_bottom_lh"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss8/ss8_door_upper_lh")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss8/ss8_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss8/ss8_door_upper")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            )).setParent(GROUP_SS8);

    public static final PanelReg<TrainPanelBlock> KM240_MONUMENT =
            new PanelReg<>("240km_monument", TrainPanelBlock::new).setParent(GROUP_SS8);
}
