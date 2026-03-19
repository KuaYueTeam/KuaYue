package cn.kycraft.kuayue.parts.locomotives.ss;

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
import net.minecraft.world.phys.Vec3;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class ISS3Panel {
    public static final RegistryGroup GROUP_SS3 =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block.noOcclusion().strength(1.5f, 3f).mapColor(DyeColor.BLUE))
            ).configure(
                    ItemRegConfigurations.adopt(item -> item.stacksTo(8).tabTo(TRAIN_PANEL_TAB))
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final PanelReg<TrainHingePanelBlock> SS3_PANEL_BOTTOM = new PanelReg<>("ss3_panel_bottom", TrainHingePanelBlock::new).setParent(GROUP_SS3);
    public static final PanelReg<TrainHingePanelBlock> SS3_PANEL_UPPER = new PanelReg<>("ss3_panel_upper", TrainHingePanelBlock::new).setParent(GROUP_SS3);
    public static final PanelReg<TrainHingePanelBlock> SS3_PANEL_UPPER_WINDOWS = new PanelReg<>("ss3_panel_upper_windows", TrainHingePanelBlock::new).setParent(GROUP_SS3);
    public static final PanelReg<TrainHingePanelBlock> SS3_PANEL_BOTTOM_AD = new PanelReg<>("ss3_panel_bottom_ad", TrainHingePanelBlock::new).setParent(GROUP_SS3);
    public static final PanelReg<TrainHingePanelBlock> SS3_PANEL_UPPER_AD = new PanelReg<>("ss3_panel_upper_ad", TrainHingePanelBlock::new).setParent(GROUP_SS3);

    public static final SlabReg<SlabBlock> SS3_CARPORT = new SlabReg<>("ss3_carport", p -> new SlabBlock(p, true)).setParent(GROUP_SS3);
    public static final SlabReg<SlabBlock> SS3_CARPORT_CENTER = new SlabReg<>("ss3_carport_center", p -> new SlabBlock(p, true)).setParent(GROUP_SS3);
    public static final SlabReg<HingeSlabBlock> SS3_ROOF = new SlabReg<>("ss3_roof", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS3);
    public static final SlabReg<HingeSlabBlock> SS3_CARPORT_AD = new SlabReg<>("ss3_carport_ad", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS3);
    public static final SlabReg<SlabBlock> SS3_FLOOR = new SlabReg<>("ss3_floor", p -> new SlabBlock(p, false)).setParent(GROUP_SS3);
    public static final SlabReg<SlabBlock> SS3_CARPORT_SKYLIGHT = new SlabReg<>("ss3_carport_skylight", p -> new SlabBlock(p, false)).setParent(GROUP_SS3);
    public static final SlabReg<HingeSlabBlock> SS3_ROOF_VENT = new SlabReg<>("ss3_roof_vent", p -> new HingeSlabBlock(p, true)).setParent(GROUP_SS3);
    public static final SlabReg<SlabBlock> SS3_FLOOR_CENTER = new SlabReg<>("ss3_floor_center", p -> new SlabBlock(p, false)).setParent(GROUP_SS3);
    public static final SlabReg<SlabBlock> SS3_TRANSFORMER = new SlabReg<>("ss3_transformer", p -> new SlabBlock(p, false)).setParent(GROUP_SS3);

    public static final BlockReg<HeadBlock> SS3_HEAD = BlockReg.of("ss3_head", HeadBlock::new).setParent(GROUP_SS3);

    public static final PanelReg<CustomRenderedDoorBlock> SS3_DOOR =
            new PanelReg<>("ss3_door", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss3/ss3_door_bottom_lh"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss3/ss3_door_upper_lh")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss3/ss3_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "ss3/ss3_door_upper")
                    ),
                    new Vec3(-.1875, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            )).setParent(GROUP_SS3);

    public static final BlockReg<SS3CowCatcherBlock> SS3_COWCATCHER = BlockReg.of("ss3_cowcatcher", SS3CowCatcherBlock::new).setParent(GROUP_SS3);
}
