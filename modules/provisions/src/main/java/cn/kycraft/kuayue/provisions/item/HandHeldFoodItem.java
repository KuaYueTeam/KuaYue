package cn.kycraft.kuayue.provisions.item;

import cn.kycraft.kuayue.provisions.food.effect.EffectUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/** 手持类食物：hover 时显示营养值与效果说明。 */
public class HandHeldFoodItem extends Item {

    public HandHeldFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        EffectUtil.addFoodEffectToTooltip(stack, context, tooltip, flag);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
