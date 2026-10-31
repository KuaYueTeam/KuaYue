package cn.kycraft.kuayue.mixins;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 倒完饮料后返回空桶。
 *
 * <p>Kept as an explicit hook so the behaviour is pinned and visible: vanilla already returns an
 * empty bucket, and this makes sure nothing (craft remainder or a future NeoForge change) swaps it
 * for something else.</p>
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    @Inject(method = "getEmptySuccessItem", at = @At("HEAD"), cancellable = true)
    private static void kuayue$returnEmptyBucket(ItemStack bucketStack, Player player,
                                                 CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(player.hasInfiniteMaterials() ? bucketStack : new ItemStack(Items.BUCKET));
    }
}
