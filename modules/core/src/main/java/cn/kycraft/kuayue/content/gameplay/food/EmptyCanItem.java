package cn.kycraft.kuayue.content.gameplay.food;

import net.minecraft.world.item.Item;

/**
 * 空易拉罐: what is left after a drink is consumed.
 *
 * <p>Deliberately a plain item - it must not be usable, otherwise the empty can could be "drunk"
 * again. Its art is the same as the filled can (see {@code models/item/empty_can.json}).</p>
 */
public class EmptyCanItem extends Item {

    public EmptyCanItem(Properties properties) {
        super(properties);
    }
}
