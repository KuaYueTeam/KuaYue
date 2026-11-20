package cn.kycraft.kuayue.overhead_line;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.init.OverheadLineFactoies;
import cn.kycraft.kuayue.overhead_line.tab.AllCrativeTabs;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.RegistryGroup;

@Context
public class OverheadLineModule {

    public static final RegistryGroup REGISTRY_GROUP = new RegistryGroup().setParent(KuaYue.REGISTRY);

    @PostConstruct
    public void init(){
        AllCrativeTabs.register();
        OverheadLineFactoies.register();
    }
}
