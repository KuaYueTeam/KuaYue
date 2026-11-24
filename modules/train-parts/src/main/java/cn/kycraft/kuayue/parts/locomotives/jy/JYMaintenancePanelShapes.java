package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelShapes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Collision derived from jy_maintenance_panel_left/right.bbmodel.
 *
 * <p>Coordinates are Blockbench pixels after applying the authored bind transforms and proxy
 * half-block X/Z offset.
 */
final class JYMaintenancePanelShapes {
    private static final VoxelShape LEFT = Shapes.or(
            Block.box(-0.1, 24.5, 15.9, 31.9, 29.0, 17.0),
            Block.box(2.4, 24.0, 15.4, 25.4, 25.0, 16.5),
            Block.box(2.4, 11.5, 15.4, 25.4, 12.5, 16.5),
            Block.box(25.4, 11.5, 15.4, 26.4, 25.0, 16.5),
            Block.box(1.4, 11.5, 15.4, 2.4, 25.0, 16.5),
            Block.box(-0.1, 0.0, 15.9, 31.9, 12.0, 17.0),
            Block.box(13.4, 12.0, 15.65, 14.4, 24.5, 16.75),
            Block.box(25.9, 12.0, 15.9, 31.9, 24.5, 17.0),
            Block.box(-0.1, 12.0, 15.9, 1.9, 24.5, 17.0),
            Block.box(2.4, 13.5, 15.5, 3.4, 23.0, 16.5),
            Block.box(2.4, 12.5, 15.5, 14.4, 13.5, 16.5),
            Block.box(2.4, 23.0, 15.5, 14.4, 24.0, 16.5),
            Block.box(12.4, 13.5, 15.5, 13.4, 23.0, 16.5),
            Block.box(14.4, 13.5, 15.5, 15.4, 23.0, 16.5),
            Block.box(14.4, 12.5, 15.5, 26.4, 13.5, 16.5),
            Block.box(14.4, 23.0, 15.5, 26.4, 24.0, 16.5),
            Block.box(24.4, 13.5, 15.5, 25.4, 23.0, 16.5)
    );

    private static final VoxelShape RIGHT = Shapes.or(
            Block.box(-16.0, 24.5, 15.8, 16.0, 29.0, 16.9),
            Block.box(-13.5, 24.0, 15.3, 9.5, 25.0, 16.4),
            Block.box(-13.5, 11.5, 15.3, 9.5, 12.5, 16.4),
            Block.box(9.5, 11.5, 15.3, 10.5, 25.0, 16.4),
            Block.box(-14.5, 11.5, 15.3, -13.5, 25.0, 16.4),
            Block.box(-16.0, 0.0, 15.8, 16.0, 12.0, 16.9),
            Block.box(-2.5, 12.0, 15.55, -1.5, 24.5, 16.65),
            Block.box(10.0, 12.0, 15.8, 16.0, 24.5, 16.9),
            Block.box(-16.0, 12.0, 15.8, -14.0, 24.5, 16.9),
            Block.box(-13.5, 13.5, 15.65, -12.5, 23.0, 16.45),
            Block.box(-13.5, 12.5, 15.65, -1.5, 13.5, 16.45),
            Block.box(-13.5, 23.0, 15.65, -1.5, 24.0, 16.45),
            Block.box(-3.5, 13.5, 15.65, -2.5, 23.0, 16.45),
            Block.box(-1.5, 13.5, 15.4, -0.5, 23.0, 16.2),
            Block.box(-1.5, 12.5, 15.4, 10.5, 13.5, 16.2),
            Block.box(-1.5, 23.0, 15.4, 10.5, 24.0, 16.2),
            Block.box(8.5, 13.5, 15.4, 9.5, 23.0, 16.2)
    );

    private JYMaintenancePanelShapes() {
    }

    static VoxelShape get(Direction facing, DoorHingeSide hinge) {
        return TrainPanelShapes.rotateShape(
                Direction.NORTH,
                facing,
                hinge == DoorHingeSide.RIGHT ? RIGHT : LEFT
        );
    }
}
