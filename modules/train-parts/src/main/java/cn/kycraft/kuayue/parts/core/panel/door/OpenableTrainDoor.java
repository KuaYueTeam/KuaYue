package cn.kycraft.kuayue.parts.core.panel.door;

import lib.kasuga.create.interaction.ContraptionShapeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A door whose authoritative open state can also be changed on a moving structure. */
public interface OpenableTrainDoor extends ContraptionShapeProvider {
    BooleanProperty doorOpenProperty();

    default boolean hasDoor() {
        return true;
    }

    static boolean isDoor(Block block) {
        return block instanceof OpenableTrainDoor door && door.hasDoor();
    }

    @Override
    default VoxelShape getContraptionShape(BlockState state, BlockGetter localBlocks, BlockPos localPos) {
        return state.getShape(localBlocks, localPos);
    }
}
