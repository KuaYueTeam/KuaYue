package cn.kycraft.kuayue.parts.core.panel.door;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.create.CreateModBeans;
import net.minecraft.world.level.block.Block;

/** Capability-based registrations also cover blocks created by runtime preset factories. */
@Context
@CreateModBeans
public final class TrainDoorContraptionModule {
    @PostConstruct
    void init() {
        register(MovingInteractionBehaviour.REGISTRY);
    }

    static void register(SimpleRegistry<Block, MovingInteractionBehaviour> registry) {
        var door = new MovingTrainDoorInteraction();
        registry.registerProvider(block -> block instanceof OpenableTrainDoor ? door : null);
        registry.invalidate();
    }
}
