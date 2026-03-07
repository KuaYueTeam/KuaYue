package cn.kycraft.kuayue.parts.carriages.c25;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
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
            new PanelReg<>("panel_bottom_25t", u->new TrainPanelBlock(u))
                    .setParent(GROUP_C25T);
}
