package cn.kycraft.kuayue.content.gameplay.food.effect;

import cn.kycraft.kuayue.KuaYue;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.effect.EffectReg;
import lib.kasuga.registration.minecraft.effect.EffectRegModifiers;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 状态效果注册。
 */
@Context
public class AllEffects {

    public static final RegistryGroup GROUP_EFFECTS = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    /** 温暖: 暗红色 (127,0,0), 有益效果. */
    public static final EffectReg<WarmEffect> WARM = EffectReg.of("warm",
                    (category, color) -> new WarmEffect(category, color))
            .configure(EffectRegModifiers.Category.of("category",
                    original -> MobEffectCategory.BENEFICIAL))
            .configure(EffectRegModifiers.Color.of("color", original -> 0x7F0000))
            .setParent(GROUP_EFFECTS);

    /** 泡面食用后的温暖效果时长，2 分钟 = 2400 ticks。 */
    public static final int WARM_DURATION_2MIN = 2400;
}
