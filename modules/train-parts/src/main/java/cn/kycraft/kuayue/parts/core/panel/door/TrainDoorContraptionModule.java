package cn.kycraft.kuayue.parts.core.panel.door;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.core.panel.company.CompanyTrainPanel;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lib.kasuga.create.CreateModBeans;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Set;

/** Capability-based registrations also cover blocks created by runtime preset factories. */
@Context
@CreateModBeans
public final class TrainDoorContraptionModule {
    /** Passenger boarding doors; endfaces, cab doors, equipment doors and sideboards stay manual. */
    private static final Set<String> PASSENGER_DOOR_IDS = Set.of(
            "22_door", "door_25b", "door_25g", "door_25k", "door_25t", "door_25z",
            "door_sliding_25g", "door_sliding_25k", "door_sliding_25t",
            "door_marshalled_cr200j", "door_marshalled_cr200j_2", "door_m1"
    );

    @PostConstruct
    void init() {
        register(MovingInteractionBehaviour.REGISTRY);
        registerMovement(MovementBehaviour.REGISTRY);
    }

    static void register(SimpleRegistry<Block, MovingInteractionBehaviour> registry) {
        var door = new MovingTrainDoorInteraction();
        var companion = new CompanyTrainDoorMovingInteraction();
        registry.registerProvider(block -> OpenableTrainDoor.isDoor(block) ? door
                : block instanceof CompanyTrainPanel ? companion : null);
        registry.invalidate();
    }

    static void registerMovement(SimpleRegistry<Block, MovementBehaviour> registry) {
        var door = new TrainDoorMovementBehaviour();
        registry.registerProvider(block -> hasAutomaticDoorControl(block) ? door : null);
        registry.invalidate();
    }

    static boolean hasAutomaticDoorControl(Block block) {
        if (!OpenableTrainDoor.isDoor(block)) {
            return false;
        }
        var id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && id.getNamespace().equals(KuaYue.MODID)
                && PASSENGER_DOOR_IDS.contains(id.getPath());
    }
}
