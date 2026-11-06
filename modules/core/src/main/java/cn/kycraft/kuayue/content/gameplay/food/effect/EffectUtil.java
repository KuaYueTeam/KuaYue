package cn.kycraft.kuayue.content.gameplay.food.effect;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * MIT License
 *
 * Copyright (c) 2020 vectorwing
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * <p>Ported from Farmer's Delight {@code TextUtils.addFoodEffectTooltip}. The 1.x attribute
 * modifier section is dropped on purpose: on 1.21.1 attribute modifiers moved to the
 * {@code ItemAttributeModifiers} data component and {@code MobEffect} no longer exposes them.</p>
 */
public final class EffectUtil {

    private EffectUtil() {
    }

    /**
     * 物品栏显示食物回复量与效果说明。
     *
     * @param hasField {@code true} 时显示一条前导的 "可食用" 说明
     */
    public static void addFoodEffectToTooltip(ItemStack stack, Item.TooltipContext context,
                                              List<Component> tooltip, TooltipFlag flag, boolean hasField) {
        FoodProperties food = foodOf(stack);
        if (food == null) {
            return;
        }
        if (hasField) {
            tooltip.add(Component.translatable("item.kuayue.tooltip.edible")
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        tooltip.add(Component.translatable("item.kuayue.tooltip.restores_health", food.nutrition())
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("item.kuayue.tooltip.restores_saturation",
                        Math.round(food.saturation() * 10.0) / 10.0)
                .withStyle(ChatFormatting.DARK_GREEN));

        List<FoodProperties.PossibleEffect> effects = food.effects();
        if (effects.isEmpty()) {
            return;
        }
        for (FoodProperties.PossibleEffect possible : effects) {
            MobEffectInstance instance = possible.effect();
            MutableComponent translation = Component.translatable(instance.getDescriptionId());
            if (instance.getAmplifier() > 0) {
                translation = Component.translatable("potion.withAmplifier", translation,
                        Component.translatable("potion.potency." + instance.getAmplifier()));
            }
            if (instance.getDuration() > 20) {
                translation = Component.translatable("potion.withDuration", translation,
                        MobEffectUtil.formatDuration(instance, 1.0F, 20));
            }
            tooltip.add(translation.withStyle(instance.getEffect().value().getCategory()
                    .getTooltipFormatting()));
        }
    }

    /** 简写：供物品 {@code appendHoverText} 直接调用。 */
    public static void addFoodEffectToTooltip(ItemStack stack, Item.TooltipContext context,
                                              List<Component> tooltip, TooltipFlag flag) {
        addFoodEffectToTooltip(stack, context, tooltip, flag, false);
    }

    private static @Nullable FoodProperties foodOf(ItemStack stack) {
        Item item = stack.getItem();
        // 1.21.1: food data可以挂在 Item.Properties 上，也可能通过数据组件提供。
        return stack.get(net.minecraft.core.component.DataComponents.FOOD);
    }
}
