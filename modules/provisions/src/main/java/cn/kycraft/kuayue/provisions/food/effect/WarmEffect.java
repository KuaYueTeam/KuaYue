package cn.kycraft.kuayue.provisions.food.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * 温暖: keeps topping up saturation while the player is still hungry.
 *
 * <p>Ported from the 1.0 {@code willow.train.kuayue.block.food.effect.WarmEffect}. Every tick, if
 * the food level is below 20 and saturation is below 5, saturation goes up by 1 point.</p>
 */
public class WarmEffect extends MobEffect {

    private static final int SATURATION_THRESHOLD = 5;

    public WarmEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof Player player) {
            FoodData foodData = player.getFoodData();
            float saturationLevel = foodData.getSaturationLevel();
            if (foodData.getFoodLevel() < 20 && saturationLevel < SATURATION_THRESHOLD) {
                foodData.setSaturation(saturationLevel + 1);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
