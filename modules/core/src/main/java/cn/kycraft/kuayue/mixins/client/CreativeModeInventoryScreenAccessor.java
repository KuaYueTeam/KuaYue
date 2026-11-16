package cn.kycraft.kuayue.mixins.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Accessor("selectedTab")
    static CreativeModeTab kuayue$selectedTab() {
        throw new AssertionError();
    }

    @Invoker("refreshSearchResults")
    void kuayue$refreshSearchResults();

    @Invoker("refreshCurrentTabContents")
    void kuayue$refreshCurrentTabContents(Collection<ItemStack> items);
}
