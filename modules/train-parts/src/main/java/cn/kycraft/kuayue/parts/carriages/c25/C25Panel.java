package cn.kycraft.kuayue.parts.carriages.c25;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.HingeSlabBlock;
import cn.kycraft.kuayue.parts.core.panel.SlabBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableSmallWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.minecraft.world.item.DyeColor;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class C25Panel {

    public static final RegistryGroup GROUP_C25 =
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

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_LINE_MARSHALLED_25 =
            new PanelReg<>("panel_bottom_line_marshalled_25", TrainPanelBlock::new)
                    .setParent(GROUP_C25);

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_MARSHALLED_25 =
            new PanelReg<>("panel_bottom_marshalled_25", TrainPanelBlock::new)
                    .setParent(GROUP_C25);

    public static final PanelReg<TrainPanelBlock> PANEL_MIDDLE_MARSHALLED_25 =
            new PanelReg<>("panel_middle_marshalled_25", TrainPanelBlock::new)
                    .setParent(GROUP_C25);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_MARSHALLED_25 =
            new PanelReg<>("panel_top_marshalled_25", TrainPanelBlock::new)
                    .setParent(GROUP_C25);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_MARSHALLED_25_2 =
            new PanelReg<>("panel_top_marshalled_25_2", TrainPanelBlock::new)
                    .setParent(GROUP_C25);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_25 =
            new PanelReg<>("window_oc_25", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_25 =
            new PanelReg<>("window_oc_sealed_25", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25);

    public static final SlabReg<SlabBlock> CARPORT_25BGZK =
            new SlabReg<>("carport_25bgzk", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25);

    public static final SlabReg<SlabBlock> CARPORT_AIR_CONDITION_25BGZK =
            new SlabReg<>("carport_air_condition_25bgzk", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25);

    public static final SlabReg<HingeSlabBlock> CARPORT_AIR_CONDITION_SIDE_25BGZK =
            new SlabReg<>("carport_air_condition_side_25bgzk", p -> new HingeSlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25);

    public static final SlabReg<SlabBlock> CARPORT_CENTER_25BGZKT =
            new SlabReg<>("carport_center_25bgzkt", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25);

    public static final SlabReg<SlabBlock> CARPORT_WATER_TANK_25BGZKT =
            new SlabReg<>("carport_water_tank_25bgzkt", p -> new SlabBlock(p, true))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25);

    public static final SlabReg<SlabBlock> FLOOR_MARSHALLED_25 =
            new SlabReg<>("floor_marshalled_25", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25);

    public static final SlabReg<SlabBlock> PANEL_SLAB_UNIVERSAL_25 =
            new SlabReg<>("panel_slab_universal_25", p -> new SlabBlock(p, false))
                    .mapColor(DyeColor.GREEN)
                    .setParent(GROUP_C25);
}
