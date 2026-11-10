package cn.kycraft.kuayue.provisions.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * 饮料罐饮用后返还的空罐。
 *
 * <p>空罐由 data-driven 的 {@code simple_item} 注册，没有编译期句柄，因此这里按 id 延迟解析。
 * 返回 {@link Supplier} 而不是直接取物品，避免在 {@code RegisterEvent} 期间解引用未绑定的注册项。</p>
 */
public final class DrinkItems {

    public static final ResourceLocation EMPTY_CAN_ID =
            ResourceLocation.fromNamespaceAndPath("kuayue", "empty_can");
    public static final ResourceLocation EMPTY_CAN_BLUE_ID =
            ResourceLocation.fromNamespaceAndPath("kuayue", "empty_can_blue");

    private DrinkItems() {
    }

    public static Supplier<? extends Item> emptyCan() {
        return () -> resolve(EMPTY_CAN_ID);
    }

    public static Supplier<? extends Item> emptyCanBlue() {
        return () -> resolve(EMPTY_CAN_BLUE_ID);
    }

    private static Item resolve(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? Items.AIR : item;
    }
}
