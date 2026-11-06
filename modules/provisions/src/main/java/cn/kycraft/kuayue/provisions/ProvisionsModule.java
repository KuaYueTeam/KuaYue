package cn.kycraft.kuayue.provisions;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.provisions.food.DrinkRegistration;
import cn.kycraft.kuayue.provisions.food.fluid.FluidRegistration;
import cn.kycraft.kuayue.provisions.init.ProvisionsFactories;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lib.kasuga.registration.RegistryGroup;
import net.neoforged.bus.api.IEventBus;

/**
 * 餐食模块：盒饭、饮料、泡面与食材。
 *
 * <p>内容通过 KasugaLib data-driven 管线注册：{@code data/kuayue/kasuga_lib/data_driven/provisions.json}
 * 声明索引，{@code data/kuayue/kasugalib/provisions.json} 声明注册组与条目，工厂类型映射在
 * {@link ProvisionsFactories}。</p>
 */
@Context
public class ProvisionsModule {

    /** 本模块的根注册组，挂在模组主注册树下面。 */
    public static final RegistryGroup REGISTRY_GROUP = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    @Inject
    @Named("modEventBus")
    IEventBus modEventBus;

    @PostConstruct
    public void init() {
        // 工厂必须先于 data-driven 内容阶段注册，否则 JSON 里的 type 查不到实现。
        ProvisionsFactories.register();
        // 流体与饮料桶走 NeoForge DeferredRegister：KasugaLibNext 目前没有流体注册接口。
        FluidRegistration.register(modEventBus);
        DrinkRegistration.register(modEventBus);
    }
}
