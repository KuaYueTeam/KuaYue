package cn.kycraft.kuayue.parts.carriages.c25;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.TrainHingePanelBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainUnOpenableSmallWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.creative_tab.CreativeTabReg;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context
public class C25TPanel {

    public static final RegistryGroup GROUP_C25T =
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

    public static final PanelReg<TrainPanelBlock> PANEL_BOTTOM_25T =
            new PanelReg<>("panel_bottom_25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainHingePanelBlock> PANEL_BOTTOM_25T_2 =
            new PanelReg<>("panel_bottom_25t_2", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainPanelBlock> PANEL_MIDDLE_25T =
            new PanelReg<>("panel_middle_25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainHingePanelBlock> PANEL_MIDDLE_25T_2 =
            new PanelReg<>("panel_middle_25t_2", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_25T =
            new PanelReg<>("panel_top_25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainHingePanelBlock> PANEL_TOP_25T_2 =
            new PanelReg<>("panel_top_25t_2", TrainHingePanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainPanelBlock> PANEL_TOP_XL25T =
            new PanelReg<>("panel_top_xl25t", TrainPanelBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_BLUE_25T =
            new PanelReg<>("window_oc_sealed_blue_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_SMALL_25T =
            new PanelReg<>("window_oc_sealed_small_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_SMALL_BLUE_25T =
            new PanelReg<>("window_oc_sealed_small_blue_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_SEALED_SMALL_BLUE_BSP25T =
            new PanelReg<>("window_oc_sealed_small_blue_bsp25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_BLUE_25T =
            new PanelReg<>("window_oc_blue_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_25T =
            new PanelReg<>("window_oc_small_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_BLUE_25T =
            new PanelReg<>("window_oc_small_blue_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_SMALL_BLUE_BSP25T =
            new PanelReg<>("window_oc_small_blue_bsp25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_25T =
            new PanelReg<>("window_oc_toilet_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_BLUE_25T =
            new PanelReg<>("window_oc_toilet_blue_25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainSmallWindowBlock> WINDOW_OC_TOILET_BLUE_BSP25T =
            new PanelReg<>("window_oc_toilet_blue_bsp25t", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_25T =
            new PanelReg<>("window_oc_wide_25t", TrainOpenableWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_BLUE_25T =
            new PanelReg<>("window_oc_wide_blue_25t", TrainOpenableWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainOpenableWindowBlock> WINDOW_OC_WIDE_BLUE_BSP25T =
            new PanelReg<>("window_oc_wide_blue_bsp25t", TrainOpenableWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_WIDE_SEALED_25T =
            new PanelReg<>("window_oc_wide_sealed_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);

    public static final PanelReg<TrainUnOpenableSmallWindowBlock> WINDOW_OC_WIDE_SEALED_BLUE_25T =
            new PanelReg<>("window_oc_wide_sealed_blue_25t", TrainUnOpenableSmallWindowBlock::new)
                    .setParent(GROUP_C25T);
}
