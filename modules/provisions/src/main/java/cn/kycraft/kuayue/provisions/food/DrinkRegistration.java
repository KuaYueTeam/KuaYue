package cn.kycraft.kuayue.provisions.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

/**
 * 饮料罐的食物数值。
 *
 * <p>物品本身由 data-driven 的 {@code drink} 工厂产出（{@link cn.kycraft.kuayue.provisions.block.ParticlesDrinkBlock}
 * 方块 + {@link DrinkCanItem} 物品），这里只保留两份 {@link FoodProperties} 供工厂引用，
 * 避免同一个 id 被两条注册管线重复注册。</p>
 */
public final class DrinkRegistration {

    /** 可乐：营养 1 / 饱和 0.5 / 可随时食用 / 迅捷 I 3 分钟。 */
    public static final FoodProperties KUA_COLA_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 1), 1.0F)
            .build();

    /** 蓝牛：营养 1 / 饱和 0.5 / 可随时食用 / 跳跃提升 I 6 分钟。 */
    public static final FoodProperties BLUE_BULL_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.JUMP, 7200, 1), 1.0F)
            .build();

    private DrinkRegistration() {
    }
}
