package cn.kycraft.kuayue.parts.locomotives.jy;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.create.CreateModBeans;
import net.minecraft.world.level.block.Block;

/** Registers runtime-created JY outriggers independently of the door module. */
@Context
@CreateModBeans
public final class JYMaintenanceOutriggerContraptionModule {
    @PostConstruct
    void init() {
        register(MovingInteractionBehaviour.REGISTRY);
    }

    static void register(SimpleRegistry<Block, MovingInteractionBehaviour> registry) {
        var interaction = new JYMaintenanceOutriggerMovingInteraction();
        registry.registerProvider(block -> block instanceof JYMaintenanceOutriggerBlock
                ? interaction : null);
        registry.invalidate();
    }
}
