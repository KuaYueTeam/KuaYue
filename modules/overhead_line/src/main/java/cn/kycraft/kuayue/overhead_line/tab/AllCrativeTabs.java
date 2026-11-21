package cn.kycraft.kuayue.overhead_line.tab;

import cn.kycraft.kuayue.KuaYue;
import lib.kasuga.registration.minecraft.creative_tab.CreativeTabReg;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AllCrativeTabs {
    public static final CreativeTabReg POWER_GRID = new CreativeTabReg("power_grid")
            .title(Component.translatable("itemGroup.kuayue.power_grid"))
            .icon(() -> new ItemStack(Items.LIGHTNING_ROD))
            .setParent(KuaYue.REGISTRY);

    private AllCrativeTabs() {}
    public static void register(){}
}
