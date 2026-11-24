package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelShapes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shapes derived from jy_maintenance_door_left/right.bbmodel.
 *
 * <p>Coordinates are Blockbench pixels after applying the authored bind transforms and proxy
 * half-block X/Z offset. The moving leaf endpoints come from door_open at time 0 and 0.25.
 * Selection keeps the visible top beam; collision omits it to preserve player head clearance.
 */
final class JYMaintenanceDoorShapes {
    private static final VoxelShape LEFT_TOP_BEAM =
            Block.box(0.0, 26.0, 15.9, 32.0, 29.0, 17.0);
    private static final VoxelShape LEFT_COLLISION_FRAME = Shapes.or(
            Block.box(0.0, 0.0, 15.9, 9.0, 26.0, 17.0),
            Block.box(23.0, 0.0, 15.9, 32.0, 26.0, 17.0),
            Block.box(7.0, 0.0, 14.9, 8.0, 24.0, 15.9),
            Block.box(24.0, 0.0, 14.9, 25.0, 24.0, 15.9)
    );
    private static final VoxelShape LEFT_FRAME = Shapes.or(LEFT_TOP_BEAM, LEFT_COLLISION_FRAME);
    private static final VoxelShape LEFT_CLOSED_LEAF =
            Block.box(9.0, 0.0, 15.9, 23.0, 26.0, 17.5);
    private static final VoxelShape LEFT_OPEN_LEAF =
            Block.box(8.9, 0.0, 16.6, 10.5, 26.0, 30.6);

    private static final VoxelShape RIGHT_TOP_BEAM =
            Block.box(-16.0, 26.0, 15.9, 16.0, 29.0, 17.0);
    private static final VoxelShape RIGHT_COLLISION_FRAME = Shapes.or(
            Block.box(7.0, 0.0, 15.9, 16.0, 26.0, 17.0),
            Block.box(-16.0, 0.0, 15.9, -7.0, 26.0, 17.0),
            Block.box(8.0, 0.0, 14.9, 9.0, 24.0, 15.9),
            Block.box(-9.0, 0.0, 14.9, -8.0, 24.0, 15.9)
    );
    private static final VoxelShape RIGHT_FRAME = Shapes.or(RIGHT_TOP_BEAM, RIGHT_COLLISION_FRAME);
    private static final VoxelShape RIGHT_CLOSED_LEAF =
            Block.box(-7.0, 0.0, 15.9, 7.0, 26.0, 17.5);
    private static final VoxelShape RIGHT_OPEN_LEAF =
            Block.box(5.5, 0.0, 16.6, 7.1, 26.0, 30.6);

    private static final VoxelShape LEFT_CLOSED = Shapes.or(LEFT_FRAME, LEFT_CLOSED_LEAF);
    private static final VoxelShape LEFT_OPEN = Shapes.or(LEFT_FRAME, LEFT_OPEN_LEAF);
    private static final VoxelShape RIGHT_CLOSED = Shapes.or(RIGHT_FRAME, RIGHT_CLOSED_LEAF);
    private static final VoxelShape RIGHT_OPEN = Shapes.or(RIGHT_FRAME, RIGHT_OPEN_LEAF);
    private static final VoxelShape LEFT_CLOSED_COLLISION =
            Shapes.or(LEFT_COLLISION_FRAME, LEFT_CLOSED_LEAF);
    private static final VoxelShape LEFT_OPEN_COLLISION =
            Shapes.or(LEFT_COLLISION_FRAME, LEFT_OPEN_LEAF);
    private static final VoxelShape RIGHT_CLOSED_COLLISION =
            Shapes.or(RIGHT_COLLISION_FRAME, RIGHT_CLOSED_LEAF);
    private static final VoxelShape RIGHT_OPEN_COLLISION =
            Shapes.or(RIGHT_COLLISION_FRAME, RIGHT_OPEN_LEAF);

    private JYMaintenanceDoorShapes() {
    }

    static VoxelShape get(Direction facing, DoorHingeSide hinge, boolean open) {
        return rotate(facing, northShape(hinge, open));
    }

    static VoxelShape getCollision(Direction facing, DoorHingeSide hinge, boolean open) {
        return rotate(facing, northCollisionShape(hinge, open));
    }

    static VoxelShape getJY290(Direction facing, DoorHingeSide hinge, boolean open) {
        return rotate(facing, northShape(hinge, open).move(0.0, 0.0, -1.0));
    }

    static VoxelShape getJY290Collision(
            Direction facing,
            DoorHingeSide hinge,
            boolean open
    ) {
        return rotate(
                facing,
                northCollisionShape(hinge, open).move(0.0, 0.0, -1.0)
        );
    }

    private static VoxelShape northShape(DoorHingeSide hinge, boolean open) {
        return hinge == DoorHingeSide.RIGHT
                ? open ? RIGHT_OPEN : RIGHT_CLOSED
                : open ? LEFT_OPEN : LEFT_CLOSED;
    }

    private static VoxelShape northCollisionShape(DoorHingeSide hinge, boolean open) {
        return hinge == DoorHingeSide.RIGHT
                ? open ? RIGHT_OPEN_COLLISION : RIGHT_CLOSED_COLLISION
                : open ? LEFT_OPEN_COLLISION : LEFT_CLOSED_COLLISION;
    }

    private static VoxelShape rotate(Direction facing, VoxelShape shape) {
        return TrainPanelShapes.rotateShape(Direction.NORTH, facing, shape);
    }
}
