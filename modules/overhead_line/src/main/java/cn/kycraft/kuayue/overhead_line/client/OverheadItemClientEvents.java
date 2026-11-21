package cn.kycraft.kuayue.overhead_line.client;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.block.support.variants.ModelledOverheadSupportBlock;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = KuaYue.MODID, value = Dist.CLIENT)
public final class OverheadItemClientEvents {
    private static final ResourceLocation A1 = ResourceLocation.fromNamespaceAndPath(
            KuaYue.MODID, "overhead_line_support_a1");

    private OverheadItemClientEvents() {}

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Item item = BuiltInRegistries.ITEM.get(A1);
        if (!(item instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ModelledOverheadSupportBlock)) {return;};

        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new OverheadModelItemRenderer();
                return renderer;
            }
        }, item);
    }
}
