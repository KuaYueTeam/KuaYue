package cn.kycraft.kuayue.parts.tracks;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.AllCarriageBogeys;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.creative_tab.CreativeTabReg;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

@Context
public class AllTemporaryTab {

    public static final CreativeTabReg KUAYUE_TEMP_TAB = new CreativeTabReg("kuayue_temp_tab")
            .title(Component.literal("Kuayue Test"))
            .icon(() -> AllCarriageBogeys.CW2_BLOCK.getEntry().asItem().getDefaultInstance())
            .displayItems((params, output) -> {
                addIfNotNull(output, AllTracks.STANDARD_TRACK);
                addIfNotNull(output, AllTracks.BALLASTLESS_TRACK);
                addIfNotNull(output, AllTracks.TIELESS_TRACK);
                addIfNotNull(output, AllTracks.METER_TRACK);
                addIfNotNull(output, AllTracks.GUARD_TRACK);
            })
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    private static void addIfNotNull(CreativeModeTab.Output output, BlockReg<?> blockReg) {
        var block = blockReg.getEntry();
        if (block != null) {
            output.accept(block);
        }
    }
}
