package cn.kycraft.kuayue.parts.core.panel.door;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A door whose authoritative open state can also be changed on a moving structure. */
public interface OpenableTrainDoor {
    BooleanProperty doorOpenProperty();
}
