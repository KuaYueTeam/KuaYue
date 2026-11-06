package cn.kycraft.kuayue;

import com.mojang.logging.LogUtils;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
        KuaYue.REGISTRY.register(modEventBus);
    }
}
