package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.door.TrainDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class JY290DoorBlock extends TrainDoorBlock {
    public JY290DoorBlock(Properties properties, Vec2 beginPos, Vec2 endPos) {
        super(properties, beginPos, endPos);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return JYMaintenanceDoorShapes.getJY290(
                state.getValue(FACING),
                state.getValue(HINGE),
                state.getValue(OPEN)
        );
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return JYMaintenanceDoorShapes.getJY290Collision(
                state.getValue(FACING),
                state.getValue(HINGE),
                state.getValue(OPEN)
        );
    }
}
