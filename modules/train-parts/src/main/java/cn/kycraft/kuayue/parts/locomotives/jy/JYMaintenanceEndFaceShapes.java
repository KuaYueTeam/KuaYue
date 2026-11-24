package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelShapes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Code-authored collision profiles derived from jy_maintenance_endface.bbmodel.
 *
 * <p>Coordinates are Blockbench pixels in the north-facing bind pose. bone14 rotates
 * from the static +90-degree closed pose to its 0-degree open pose through door_open.
 */
final class JYMaintenanceEndFaceShapes {
    private static final VoxelShape FRAME = Shapes.or(
            Block.box(-13.5, 11.5, 14.3, -12.5, 25.0, 15.4),
            Block.box(4.5, 11.5, 14.3, 5.5, 25.0, 15.4),
            Block.box(-12.5, 11.5, 14.3, 4.5, 12.5, 15.4),
            Block.box(-12.5, 24.0, 14.3, 4.5, 25.0, 15.4),
            Block.box(-12.5, 12.5, 15.0, 4.5, 13.5, 15.1),
            Block.box(-12.5, 23.0, 15.0, 4.5, 24.0, 15.1),
            Block.box(-12.5, 13.5, 15.0, -11.5, 23.0, 15.1),
            Block.box(3.5, 13.5, 15.0, 4.5, 23.0, 15.1),
            Block.box(-13.5, 0.0, 14.3, 11.9119, 11.5, 16.5619),
            Block.box(4.5, 0.0, 14.3, 11.9119, 25.0, 16.5619)
    );
    private static final VoxelShape CLOSED_DOOR =
            Block.box(11.9119, 0.0, 14.9619, 25.9119, 26.0, 16.5619);
    private static final VoxelShape OPEN_DOOR =
            Block.box(11.4381, 0.0, 15.9119, 13.0381, 26.0, 29.9119);
    private static final VoxelShape NORTH_CLOSED = Shapes.or(FRAME, CLOSED_DOOR);
    private static final VoxelShape NORTH_OPEN = Shapes.or(FRAME, OPEN_DOOR);

    private JYMaintenanceEndFaceShapes() {
    }

    static VoxelShape get(Direction facing, boolean open) {
        return TrainPanelShapes.rotateShape(
                Direction.NORTH,
                facing,
                open ? NORTH_OPEN : NORTH_CLOSED
        );
    }
}
