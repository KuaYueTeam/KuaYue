package cn.kycraft.kuayue.provisions;

import cn.kycraft.kuayue.KuaYue;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;

/**
 * 餐食模块：盒饭、饮料、泡面与食材。
 *
 * <p>All content in this module is registered through the KasugaLib data-driven pipeline:
 * {@code data/kuayue/kasugalib/provisions.json} declares the registry groups and entries, and
 * {@code ProvisionsFactories} maps each {@code type} to a concrete block/item class.</p>
 */
@Context
public class ProvisionsModule {

    /** 本模块的根注册组，挂在模组主注册树下面。 */
    public static final RegistryGroup REGISTRY_GROUP = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);
}
