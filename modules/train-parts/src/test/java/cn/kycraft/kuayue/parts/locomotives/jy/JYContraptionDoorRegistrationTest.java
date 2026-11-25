package cn.kycraft.kuayue.parts.locomotives.jy;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JYContraptionDoorRegistrationTest {
    @Test
    void maintenanceSideDoorsHaveAnAssembledInteraction() {
        var door = JYContraptionDoorFixtures.sideDoor;
        assertNotNull(door, "FML must load the fixture during the block registry event");
        assertNotNull(MovingInteractionBehaviour.REGISTRY.get(door),
                "Create captures only registered moving interactions");
    }

    @Test
    void maintenanceEndfacesHaveAnAssembledInteraction() {
        var endface = JYContraptionDoorFixtures.endface;
        assertNotNull(endface, "FML must load the fixture during the block registry event");
        assertNotNull(MovingInteractionBehaviour.REGISTRY.get(endface),
                "The proxy endface is not a vanilla DoorBlock");
    }
}
