package cn.kycraft.kuayue;

import com.mojang.logging.LogUtils;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import cn.kycraft.kuayue.content.gameplay.food.DrinkRegistration;
import cn.kycraft.kuayue.content.gameplay.food.fluid.FluidRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Context()
public class KuaYueApplication {
    @Inject()
    ModContainer modContainer;
    Logger logger = LogUtils.getLogger();


    @Inject() @Named("modEventBus")
    IEventBus modEventBus;

    @PostConstruct
    public void init() {
        // Initialize the application
        logger.info("KuaYue initialized.");
        logger.info("Application ID:" + modContainer.getModId());
        FluidRegistration.register(modEventBus);
        DrinkRegistration.register(modEventBus);
        KuaYue.REGISTRY.register(modEventBus);
    }
}
