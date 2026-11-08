package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Item id -> stack resolution.
 *
 * <p>Ids are resolved lazily, when the registry is already frozen, and unknown ids are dropped instead
 * of crashing: a tech tree that references a mod which is not installed still loads.</p>
 */
public final class ItemStacks {

    private ItemStacks() {
    }

    public static ItemStack resolve(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Adds registry-backed entries, keeping the declared order. */
    public static void collect(Map<String, Integer> source, Map<ItemStack, Integer> target) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            ItemStack stack = resolve(entry.getKey());
            if (stack.isEmpty()) continue;
            target.put(stack, entry.getValue());
        }
    }
}
