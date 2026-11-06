package cn.kycraft.kuayue.meals;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.meals.init.MealsFactories;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.registration.RegistryGroup;

/**
 * 餐食模块：盒饭、饮料、泡面与食材。
 *
 * <p>内容通过 KasugaLib data-driven 管线注册：{@code data/kuayue/kasuga_lib/data_driven/provisions.json}
 * 声明索引，{@code data/kuayue/kasugalib/provisions.json} 声明注册组与条目，工厂类型映射在
 * {@link MealsFactories}。</p>
 */
@Context
public class MealsModule {

    /** 本模块的根注册组，挂在模组主注册树下面。 */
    public static final RegistryGroup REGISTRY_GROUP = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    @PostConstruct
    public void init() {
        MealsFactories.register();
    }
}
