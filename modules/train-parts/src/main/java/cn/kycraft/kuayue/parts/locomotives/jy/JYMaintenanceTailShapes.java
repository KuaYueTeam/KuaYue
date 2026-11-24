package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelShapes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Collision derived from jy_maintenance_tail.bbmodel.
 *
 * <p>Zero-height Y witness cubes are intentionally excluded. Coordinates include the authored group
 * transforms and the proxy half-block X/Z offset.
 */
final class JYMaintenanceTailShapes {
    private static final VoxelShape NORTH = Shapes.or(
            Block.box(-16.0, -9.0, 10.0, 32.0, 0.0, 16.0),
            Block.box(31.8336, -2.0, 9.1664, 32.8336, 0.0, 16.1664),
            Block.box(31.6336, 0.0, 10.1664, 32.4336, 11.0, 16.1664),
            Block.box(-16.8336, -2.0, 9.1664, -15.8336, 0.0, 16.1664),
            Block.box(-16.4336, 0.0, 10.1664, -15.6336, 11.0, 16.1664),
            Block.box(15.8336, -2.0, 9.1664, 31.8336, 0.0, 10.1664),
            Block.box(15.8336, 0.0, 9.7664, 31.8336, 11.0, 10.5664),
            Block.box(-0.1664, -2.0, 9.1664, 15.8336, 0.0, 10.1664),
            Block.box(-0.1664, 0.0, 9.7664, 15.8336, 11.0, 10.5664),
            Block.box(-16.1664, -2.0, 9.1664, -0.1664, 0.0, 10.1664),
            Block.box(-16.1664, 0.0, 9.7664, -0.1664, 11.0, 10.5664)
    );

    private JYMaintenanceTailShapes() {
    }

    static VoxelShape get(Direction facing) {
        return TrainPanelShapes.rotateShape(Direction.NORTH, facing, NORTH);
    }
}
