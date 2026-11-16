package cn.kycraft.kuayue.mixins.client;

import cn.kycraft.kuayue.client.creative.CreativeFilterController;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void kuayue$afterSelectTab(CreativeModeTab tab, CallbackInfo callback) {
        CreativeFilterController.onTabChanged((CreativeModeInventoryScreen) (Object) this);
    }

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void kuayue$afterRefresh(CallbackInfo callback) {
        CreativeFilterController.afterRefresh((CreativeModeInventoryScreen) (Object) this);
    }
}
