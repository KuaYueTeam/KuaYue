package cn.kycraft.kuayue.parts.core.multiblock;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.create.CreateModBeans;
import net.minecraft.world.level.block.Block;

/** Shared companion routing for doors, outriggers and other captured parent behaviours. */
@Context
@CreateModBeans
public final class MultiCellContraptionModule {
    @PostConstruct
    void init() {
        register(MovingInteractionBehaviour.REGISTRY);
    }

    static void register(SimpleRegistry<Block, MovingInteractionBehaviour> registry) {
        var interaction = new MultiCellMovingInteraction();
        registry.registerProvider(block -> block instanceof MultiCellCompanionBlock
                ? interaction : null);
        registry.invalidate();
    }
}
