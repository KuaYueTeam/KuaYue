package cn.kycraft.kuayue.parts.core.panel.base;

import cn.kycraft.kuayue.utils.DirectionUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrainPanelShapes {

    public static final VoxelShape SOUTH_AABB = Block.box(0, 0, 15, 16, 16, 17);
    public static final VoxelShape EAST_AABB = Block.box(15, 0, 0, 17, 16, 16);
    public static final VoxelShape NORTH_AABB = Block.box(0, 0, -1, 16, 16, 1);
    public static final VoxelShape WEST_AABB = Block.box(-1, 0, 0, 1, 16, 16);

    public static final VoxelShape DOOR_SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    public static final VoxelShape DOOR_NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    public static final VoxelShape DOOR_WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    public static final VoxelShape DOOR_EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    public static final VoxelShape DOUBLE_DOOR_CLOSE_NORTH_AABB = Block.box(-16.0D, 0.0D, 0.0D, 32.0D, 20.0D, 5.0D);
    public static final VoxelShape DOUBLE_DOOR_CLOSE_SOUTH_AABB = Block.box(-16.0D, 0.0D, 11.0D, 32.0D, 20.0D, 16.0D);
    public static final VoxelShape DOUBLE_DOOR_CLOSE_EAST_AABB = Block.box(11.0D, 0.0D, -16.0D, 16.0D, 20.0D, 32.0D);
    public static final VoxelShape DOUBLE_DOOR_CLOSE_WEST_AABB = Block.box(0.0D, 0.0D, -16.0D, 5.0D, 20.0D, 32.0D);

    public static final VoxelShape DOUBLE_DOOR_OPEN_NORTH_AABB = Shapes.or(
            Block.box(-24.0D, 0.0D, 0.0D, -8.0D, 20.0D, 5.0D),
            Block.box(24.0D, 0.0D, 0.0D, 40.0D, 20.0D, 5.0D));
    public static final VoxelShape DOUBLE_DOOR_OPEN_SOUTH_AABB = Shapes.or(
            Block.box(-24.0D, 0.0D, 11.0D, -8.0D, 20.0D, 16.0D),
            Block.box(24.0D, 0.0D, 11.0D, 40.0D, 20.0D, 16.0D));
    public static final VoxelShape DOUBLE_DOOR_OPEN_EAST_AABB = Shapes.or(
            Block.box(11.0D, 0.0D, -24.0D, 16.0D, 20.0D, -8.0D),
            Block.box(11.0D, 0.0D, 24.0D, 16.0D, 20.0D, 40.0D));
    public static final VoxelShape DOUBLE_DOOR_OPEN_WEST_AABB = Shapes.or(
            Block.box(0.0D, 0.0D, -24.0D, 5.0D, 20.0D, -8.0D),
            Block.box(0.0D, 0.0D, 24.0D, 5.0D, 20.0D, 40.0D));

    public static final VoxelShape FLOOR = Block.box(0, 8, 0, 16, 16, 16);
    public static final VoxelShape FLOOR_TWO_GRID_NORTH = Block.box(-16, 8, 0, 16, 16, 16);
    public static final VoxelShape FLOOR_TWO_GRID_SOUTH = Block.box(0, 8, 0, 32, 16, 16);
    public static final VoxelShape FLOOR_TWO_GRID_WEST = Block.box(0, 8, 0, 16, 16, 32);
    public static final VoxelShape FLOOR_TWO_GRID_EAST = Block.box(0, 8, -16, 16, 16, 16);

    public static final VoxelShape CARPORT_CENTER = Block.box(0, 0, 0, 16, 8, 16);
    public static final VoxelShape EXTEND_CARPORT_CENTER_EAST = Block.box(0, 0, 0, 24, 8, 16);
    public static final VoxelShape EXTEND_CARPORT_CENTER_WEST = Block.box(-8, 0, 0, 16, 8, 16);
    public static final VoxelShape EXTEND_CARPORT_CENTER_SOUTH = Block.box(0, 0, 0, 16, 8, 24);
    public static final VoxelShape EXTEND_CARPORT_CENTER_NORTH = Block.box(0, 0, -8, 16, 8, 16);
    public static final VoxelShape LADDER_SOUTH_AABB =
            Shapes.or(
                    Block.box(0.5, 0, 0.5, 15, 1, 9),
                    Block.box(0.5, 6, 5.5, 15, 7, 12.5),
                    Block.box(0.5, 12, 10.5, 15, 13, 15.5),
                    Block.box(15.25, 13.75, 2.25, 16, 16, 16),
                    Block.box(15.25, 9.5, 12, 16, 11.25, 14.25),
                    Block.box(15.25, 4.25, 2.25, 16, 13.75, 12),
                    Block.box(15.25, 0, 1, 16, 2.25, 10.25),
                    Block.box(15.25, 2, 1.75, 16, 4.25, 11),
                    Block.box(15.25, 7.25, 12, 16, 9.5, 13.25),
                    Block.box(15.25, 11.25, 12, 16, 13.75, 15),
                    Block.box(13.5, 10, 1, 16, 16, 2.5),
                    Block.box(0, 10, 1, 2.5, 16, 2.5),
                    Block.box(0, 13.75, 2.25, 0.75, 16, 16),
                    Block.box(0, 4.25, 2.25, 0.75, 13.75, 12),
                    Block.box(0, 11.25, 12, 0.75, 13.75, 15),
                    Block.box(0, 2, 1.75, 0.75, 4.25, 11),
                    Block.box(0, 0, 1, 0.75, 2.25, 10.25),
                    Block.box(0, 7.25, 12, 0.75, 9.5, 13.25),
                    Block.box(0, 9.5, 12, 0.75, 11.25, 14.25));
    public static final VoxelShape LADDER_WEST_AABB =
            Shapes.or(
                    Block.box(7, 0, 0.5, 15.5, 1, 15),
                    Block.box(3.5, 6, 0.5, 10.5, 7, 15),
                    Block.box(0.5, 12, 0.5, 5.5, 13, 15),
                    Block.box(0, 13.75, 15.25, 13.75, 16, 16),
                    Block.box(1.75, 9.5, 15.25, 4, 11.25, 16),
                    Block.box(4, 4.25, 15.25, 13.75, 13.75, 16),
                    Block.box(5.75, 0, 15.25, 15, 2.25, 16),
                    Block.box(5, 2, 15.25, 14.25, 4.25, 16),
                    Block.box(2.75, 7.25, 15.25, 4, 9.5, 16),
                    Block.box(1, 11.25, 15.25, 4, 13.75, 16),
                    Block.box(13.5, 10, 13.5, 15, 16, 16),
                    Block.box(13.5, 10, 0, 15, 16, 2.5),
                    Block.box(0, 13.75, 0, 13.75, 16, 0.75),
                    Block.box(4, 4.25, 0, 13.75, 13.75, 0.75),
                    Block.box(1, 11.25, 0, 4, 13.75, 0.75),
                    Block.box(5, 2, 0, 14.25, 4.25, 0.75),
                    Block.box(5.75, 0, 0, 15, 2.25, 0.75),
                    Block.box(2.75, 7.25, 0, 4, 9.5, 0.75),
                    Block.box(1.75, 9.5, 0, 4, 11.25, 0.75));
    public static final VoxelShape LADDER_NORTH_AABB =
            Shapes.or(
                    Block.box(1, 0, 7, 15.5, 1, 15.5),
                    Block.box(1, 6, 3.5, 15.5, 7, 10.5),
                    Block.box(1, 12, 0.5, 15.5, 13, 5.5),
                    Block.box(0, 13.75, 0, 0.75, 16, 13.75),
                    Block.box(0, 9.5, 1.75, 0.75, 11.25, 4),
                    Block.box(0, 4.25, 4, 0.75, 13.75, 13.75),
                    Block.box(0, 0, 5.75, 0.75, 2.25, 15),
                    Block.box(0, 2, 5, 0.75, 4.25, 14.25),
                    Block.box(0, 7.25, 2.75, 0.75, 9.5, 4),
                    Block.box(0, 11.25, 1, 0.75, 13.75, 4),
                    Block.box(0, 10, 13.5, 2.5, 16, 15),
                    Block.box(13.5, 10, 13.5, 16, 16, 15),
                    Block.box(15.25, 13.75, 0, 16, 16, 13.75),
                    Block.box(15.25, 4.25, 4, 16, 13.75, 13.75),
                    Block.box(15.25, 11.25, 1, 16, 13.75, 4),
                    Block.box(15.25, 2, 5, 16, 4.25, 14.25),
                    Block.box(15.25, 0, 5.75, 16, 2.25, 15),
                    Block.box(15.25, 7.25, 2.75, 16, 9.5, 4),
                    Block.box(15.25, 9.5, 1.75, 16, 11.25, 4));
    public static final VoxelShape LADDER_EAST_AABB =
            Shapes.or(
                    Block.box(0.5, 0, 1, 9, 1, 15.5),
                    Block.box(5.5, 6, 1, 12.5, 7, 15.5),
                    Block.box(10.5, 12, 1, 15.5, 13, 15.5),
                    Block.box(2.25, 13.75, 0, 16, 16, 0.75),
                    Block.box(12, 9.5, 0, 14.25, 11.25, 0.75),
                    Block.box(2.25, 4.25, 0, 12, 13.75, 0.75),
                    Block.box(1, 0, 0, 10.25, 2.25, 0.75),
                    Block.box(1.75, 2, 0, 11, 4.25, 0.75),
                    Block.box(12, 7.25, 0, 13.25, 9.5, 0.75),
                    Block.box(12, 11.25, 0, 15, 13.75, 0.75),
                    Block.box(1, 10, 0, 2.5, 16, 2.5),
                    Block.box(1, 10, 13.5, 2.5, 16, 16),
                    Block.box(2.25, 13.75, 15.25, 16, 16, 16),
                    Block.box(2.25, 4.25, 15.25, 12, 13.75, 16),
                    Block.box(12, 11.25, 15.25, 15, 13.75, 16),
                    Block.box(1.75, 2, 15.25, 11, 4.25, 16),
                    Block.box(1, 0, 15.25, 10.25, 2.25, 16),
                    Block.box(12, 7.25, 15.25, 13.25, 9.5, 16),
                    Block.box(12, 9.5, 15.25, 14.25, 11.25, 16));

    public static final VoxelShape METER_LADDER_SOUTH_AABB =
            Shapes.or(
                    Block.box(1, 0, 15, 15, 8, 16),
                    Block.box(0, 0, 0, 1, 8, 16),
                    Block.box(15, 0, 0, 16, 8, 16),
                    Block.box(0, 0, 1, 15, 1, 15));

    public static final VoxelShape METER_LADDER_WEST_AABB =
            Shapes.or(
                    Block.box(0, 0, 1, 1, 8, 15),
                    Block.box(0, 0, 0, 16, 8, 1),
                    Block.box(0, 0, 15, 16, 8, 16),
                    Block.box(0, 0, 1, 15, 1, 15));

    public static final VoxelShape METER_LADDER_NORTH_AABB =
            Shapes.or(
                    Block.box(1, 0, 0, 15, 8, 1),
                    Block.box(0, 0, 0, 1, 8, 16),
                    Block.box(15, 0, 0, 16, 8, 16),
                    Block.box(1, 0, 0, 15, 1, 15));

    public static final VoxelShape METER_LADDER_EAST_AABB =
            Shapes.or(
                    Block.box(15, 0, 1, 16, 8, 15),
                    Block.box(0, 0, 0, 16, 8, 1),
                    Block.box(0, 0, 15, 16, 8, 16),
                    Block.box(1, 0, 0, 15, 1, 15));

    public static VoxelShape HALF_HEIGHT_TOP_AABB =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    protected static final VoxelShape C70_DOOR_CLOSE_NORTH_AABB =
            Block.box(0, 0, -1, 32, 40, 2);

    protected static final VoxelShape C70_DOOR_CLOSE_WEST_AABB =
            Block.box(-1, 0, -16, 2, 40, 16);

    protected static final VoxelShape C70_DOOR_CLOSE_SOUTH_AABB =
            Block.box(-16, 0, 14, 16, 40, 17);

    protected static final VoxelShape C70_DOOR_CLOSE_EAST_AABB =
            Block.box(14, 0, 0, 17, 40, 32);

    protected static final VoxelShape C70_DOOR_OPEN_NORTH_AABB =
            Shapes.or(
                    Block.box(0, 0, -1, 2, 40, 2),
                    Block.box(30, 0, -1, 32, 40, 2),
                    Block.box(0, 38, -1, 32, 40, 2));

    protected static final VoxelShape C70_DOOR_OPEN_WEST_AABB =
            Shapes.or(
                    Block.box(-1, 0, -16, 2, 40, -14),
                    Block.box(-1, 0, 14, 2, 40, 16),
                    Block.box(-1, 38, -16, 2, 40, 16));

    protected static final VoxelShape C70_DOOR_OPEN_SOUTH_AABB =
            Shapes.or(
                    Block.box(-16, 0, 14, -14, 40, 17),
                    Block.box(14, 0, 14, 16, 40, 17),
                    Block.box(-16, 38, 14, 16, 40, 17));

    protected static final VoxelShape C70_DOOR_OPEN_EAST_AABB =
            Shapes.or(
                    Block.box(14, 0, 0, 17, 40, 2),
                    Block.box(14, 0, 30, 17, 40, 32),
                    Block.box(14, 38, 0, 17, 40, 32));

    protected static final VoxelShape JY290_DOOR_CLOSE_NORTH_AABB =
            Block.box(0, 0, 14, 32, 32, 16);

    protected static final VoxelShape JY290_DOOR_CLOSE_WEST_AABB =
            Block.box(14, 0, -16, 16, 32, 16);

    protected static final VoxelShape JY290_DOOR_CLOSE_SOUTH_AABB =
            Block.box(-16, 0, 0, 16, 32, 2);

    protected static final VoxelShape JY290_DOOR_CLOSE_EAST_AABB =
            Block.box(0, 0, 0, 2, 32, 32);

    protected static final VoxelShape JY290_DOOR_OPEN_NORTH_AABB =
            Shapes.or(
                    Block.box(0, 0, 14, 2, 32, 16),
                    Block.box(30, 0, 14, 32, 32, 16),
                    Block.box(0, 32, 14, 32, 32, 16));

    protected static final VoxelShape JY290_DOOR_OPEN_WEST_AABB =
            Shapes.or(
                    Block.box(14, 0, -16, 16, 32, -14),
                    Block.box(14, 0, 14, 16, 32, 16),
                    Block.box(14, 32, -16, 16, 32, 16));

    protected static final VoxelShape JY290_DOOR_OPEN_SOUTH_AABB =
            Shapes.or(
                    Block.box(-16, 0, 0, -14, 32, 2),
                    Block.box(14, 0, 0, 16, 32, 2),
                    Block.box(-16, 32, 0, 16, 32, 2));

    protected static final VoxelShape JY290_DOOR_OPEN_EAST_AABB =
            Shapes.or(
                    Block.box(0, 0, 0, 2, 32, 2),
                    Block.box(0, 0, 30, 2, 32, 32),
                    Block.box(0, 32, 0, 2, 32, 32));

    protected static final VoxelShape OverheadLinePillar_AABB =
            Shapes.or(
                    Shapes.box(0.1875, 0, 0.1875, 0.8125, 1, 0.8125));



    public static final VoxelShape CARRIAGEUNDERGROUND_NORTH =
            Shapes.or(
                    Shapes.box(0.4375, -0.25, -1.875, 1.1875, 0.5, 0),
                    Shapes.box(0.625, 0, 0, 0.75, 0.125, 0.125));

    protected static final VoxelShape OverheadSmallTruss_NS_AABB =
            Shapes.box(0.4375, 0, 0, 0.5625, 1, 1);

    protected static final VoxelShape OverheadSmallTruss_WE_AABB =
            Shapes.box(0, 0, 0.4375, 1, 1, 0.5625);
    protected static final VoxelShape OverheadBigTruss_NS_AABB =
            Shapes.box(0.25, 0, 0, 0.75, 1, 1);
    protected static final VoxelShape OverheadBigTruss_WE_AABB =
            Shapes.box(0, 0, 0.25, 1, 1, 0.75);

    protected static final VoxelShape OverheadPillarTruss_NS_AABB =
            Shapes.box(0.25, 0.25, 0, 0.75, 0.75, 1);

    protected static final VoxelShape OverheadPillarTruss_WE_AABB =
            Shapes.box(0, 0.25, 0.25, 1, 0.75, 0.75);

    public final static VoxelShape HALF_PANEL_SHAPE_EAST = Block.box(0, 0, 0, 8, 16, 16);

    public static final VoxelShape QUARTER_PANEL_SHAPE_EAST = Block.box(0, 0, 0, 4, 16, 16);

    public static final VoxelShape DF5_CARPORT_SHAPE_EAST = Shapes.or(
            Block.box(0, 0, 0, 8, 12, 16),
            Block.box(-8, 12, 0, 8, 16, 16));

    public static final VoxelShape DF5_CARPORT_COLLISION_SHAPE_EAST = Shapes.or(
            Block.box(0, 0, 0, 4, 12, 16),
            Block.box(-8, 12, 0, 4, 16, 16));

    public static final VoxelShape DF5_RADIATOR_GRID_SHAPE_EAST =
            Block.box(0, 0, 0, 8, 32, 16);

    public static final VoxelShape DF5_RADIATOR_GRID_COLLISION_SHAPE_EAST =
            Block.box(0, 0, 0, 4, 32, 16);

    public static final VoxelShape DF5_EQUIP_DOOR_1_SHAPE_EAST =
            Block.box(6, 0, 0, 8, 32, 22);

    public static final VoxelShape DF5_EQUIP_DOOR_1_COLLISION_SHAPE_EAST =
            Block.box(0, 0, 0, 4, 32, 22);

    public static final VoxelShape DF5_EQUIP_DOOR_1_OPEN_COLLISION_SHAPE_EAST = Shapes.or(
            Block.box(0, 0, 21, 4, 32, 22),
            Block.box(0, 0, 0, 4, 32, 1));

    public static final VoxelShape DF5_EQUIP_DOOR_2_SHAPE_SOUTH = Shapes.or(
            Block.box(0, 31, 0, 16, 32, 8),
            Block.box(6, 0, 6, 10, 31, 8));

    public static final VoxelShape DF5_EQUIP_DOOR_2_COLLISION_SHAPE_SOUTH = Shapes.or(
            Block.box(0, 31, 0, 16, 32, 4),
            Block.box(6, 0, 0, 10, 31, 4));

    public static final VoxelShape DF5_ENGINE_ACCESS_DOOR_SHAPE_EAST =
            Block.box(6, 0, 0, 8, 32, 16);

    public static final VoxelShape DF5_ENGINE_ACCESS_DOOR_COLLISION_SHAPE_EAST =
            Block.box(0, 0, 0, 4, 32, 16);

    public static final VoxelShape DF5_ENGINE_ACCESS_DOOR_OPEN_COLLISION_SHAPE_EAST = Shapes.or(
            Block.box(0, 0, 0, 4, 32, 1),
            Block.box(0, 0, 15, 4, 32, 16));

    public static final VoxelShape DF5_CABIN_DOOR_SHAPE_EAST =
            Block.box(6, 0, 8, 8, 32, 16);

    public static final VoxelShape DF5_CABIN_DOOR_OPEN_COLLISION_SHAPE_EAST =
            Block.box(7, 0, 0, 8, 32, 1);

    public static final VoxelShape DF5_COWCATCHER_LADDER_EAST =
            Block.box(0, 0, 0, 8, 16, 16);

    public static VoxelShape getShape(Direction direction) {
        return switch (direction) {
            case EAST -> EAST_AABB;
            case NORTH -> NORTH_AABB;
            case WEST -> WEST_AABB;
            case SOUTH -> SOUTH_AABB;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getDoorShape(Direction direction, DoorHingeSide side, boolean open) {
        if (!open) {
            return getCloseDoorShape(direction);
        }
        return switch (direction) {
            case EAST -> side != DoorHingeSide.LEFT ? DOOR_NORTH_AABB : DOOR_SOUTH_AABB;
            case SOUTH -> side != DoorHingeSide.LEFT ? DOOR_EAST_AABB : DOOR_WEST_AABB;
            case WEST -> side != DoorHingeSide.LEFT ? DOOR_SOUTH_AABB : DOOR_NORTH_AABB;
            case NORTH -> side != DoorHingeSide.LEFT ? DOOR_WEST_AABB : DOOR_EAST_AABB;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getDoubleDoorShape(Direction direction, boolean open) {
        if (!open) {
            return getDoubleDoorCloseShape(direction);
        }
        return switch (direction) {
            case EAST -> DOUBLE_DOOR_OPEN_EAST_AABB;
            case SOUTH -> DOUBLE_DOOR_OPEN_SOUTH_AABB;
            case WEST -> DOUBLE_DOOR_OPEN_WEST_AABB;
            case NORTH -> DOUBLE_DOOR_OPEN_NORTH_AABB;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getDoubleDoorCloseShape(Direction direction) {
        return switch (direction) {
            case EAST -> DOUBLE_DOOR_CLOSE_EAST_AABB;
            case SOUTH -> DOUBLE_DOOR_CLOSE_SOUTH_AABB;
            case WEST -> DOUBLE_DOOR_CLOSE_WEST_AABB;
            case NORTH -> DOUBLE_DOOR_CLOSE_NORTH_AABB;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getSlidingDoorShape(Direction direction, DoorHingeSide side, boolean open) {
        if (!open) return getCloseDoorShape(direction);
        return switch (direction) {
            case EAST -> DirectionUtil.sideMoveShape(DOOR_EAST_AABB, direction, 1, side == DoorHingeSide.RIGHT);
            case NORTH -> DirectionUtil.sideMoveShape(DOOR_NORTH_AABB, direction, 1, side == DoorHingeSide.RIGHT);
            case SOUTH -> DirectionUtil.sideMoveShape(DOOR_SOUTH_AABB, direction, 1, side == DoorHingeSide.RIGHT);
            case WEST -> DirectionUtil.sideMoveShape(DOOR_WEST_AABB, direction, 1, side == DoorHingeSide.RIGHT);
            default -> getCloseDoorShape(direction);
        };
    }

    public static VoxelShape getCloseDoorShape(Direction direction) {
        return switch (direction) {
            case EAST -> DOOR_EAST_AABB;
            case NORTH -> DOOR_NORTH_AABB;
            case SOUTH -> DOOR_SOUTH_AABB;
            case WEST -> DOOR_WEST_AABB;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getLadderShape(BlockState state) {
        boolean open = state.getValue(BlockStateProperties.OPEN);
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (!open) return Shapes.block();
        return switch (direction) {
            case EAST -> LADDER_EAST_AABB;
            case WEST -> LADDER_WEST_AABB;
            case SOUTH -> LADDER_SOUTH_AABB;
            case NORTH -> LADDER_NORTH_AABB;
            default -> LADDER_EAST_AABB;
        };
    }

    public static VoxelShape getMeterLadderShape(BlockState state) {
        boolean open = state.getValue(BlockStateProperties.OPEN);
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (!open)
            return HALF_HEIGHT_TOP_AABB;
        return switch (direction) {
            case EAST -> METER_LADDER_EAST_AABB;
            case WEST -> METER_LADDER_WEST_AABB;
            case SOUTH -> METER_LADDER_SOUTH_AABB;
            case NORTH -> METER_LADDER_NORTH_AABB;
            default -> METER_LADDER_EAST_AABB;
        };
    }

    public static VoxelShape getDoubleRotateDoorShape(boolean open, DoorHingeSide hinge, Direction direction) {
        if (!open)
            return getDoubleRotateDoorCloseShape(hinge, direction);
        if (hinge == DoorHingeSide.RIGHT) {
            return switch (direction) {
                case NORTH -> C70_DOOR_OPEN_NORTH_AABB;
                case SOUTH -> C70_DOOR_OPEN_SOUTH_AABB;
                case WEST -> C70_DOOR_OPEN_WEST_AABB;
                case EAST -> C70_DOOR_OPEN_EAST_AABB;
                default -> C70_DOOR_OPEN_NORTH_AABB;
            };
        }
        return switch (direction) {
            case NORTH -> C70_DOOR_OPEN_NORTH_AABB.move(-1, 0, 0);
            case SOUTH -> C70_DOOR_OPEN_SOUTH_AABB.move(1, 0, 0);
            case WEST -> C70_DOOR_OPEN_WEST_AABB.move(0, 0, 1);
            case EAST -> C70_DOOR_OPEN_EAST_AABB.move(0, 0, -1);
            default -> C70_DOOR_OPEN_NORTH_AABB.move(1, 0, 0);
        };
    }

    public static VoxelShape getDoubleRotateDoorCloseShape(DoorHingeSide hinge, Direction direction) {
        if (hinge == DoorHingeSide.RIGHT) {
            return switch (direction) {
                case NORTH -> C70_DOOR_CLOSE_NORTH_AABB;
                case SOUTH -> C70_DOOR_CLOSE_SOUTH_AABB;
                case WEST -> C70_DOOR_CLOSE_WEST_AABB;
                case EAST -> C70_DOOR_CLOSE_EAST_AABB;
                default -> C70_DOOR_CLOSE_NORTH_AABB;
            };
        }
        return switch (direction) {
            case NORTH -> C70_DOOR_CLOSE_NORTH_AABB.move(-1, 0, 0);
            case SOUTH -> C70_DOOR_CLOSE_SOUTH_AABB.move(1, 0, 0);
            case WEST -> C70_DOOR_CLOSE_WEST_AABB.move(0, 0, 1);
            case EAST -> C70_DOOR_CLOSE_EAST_AABB.move(0, 0, -1);
            default -> C70_DOOR_CLOSE_NORTH_AABB.move(1, 0, 0);
        };
    }

    public static VoxelShape getJY290DoorShape(boolean open, DoorHingeSide hinge, Direction direction) {
        if (!open)
            return getJY290DoorCloseShape(hinge, direction);
        if (hinge == DoorHingeSide.RIGHT) {
            return switch (direction) {
                case NORTH -> JY290_DOOR_OPEN_NORTH_AABB;
                case SOUTH -> JY290_DOOR_OPEN_SOUTH_AABB;
                case WEST -> JY290_DOOR_OPEN_WEST_AABB;
                case EAST -> JY290_DOOR_OPEN_EAST_AABB;
                default -> JY290_DOOR_OPEN_NORTH_AABB;
            };
        }
        return switch (direction) {
            case NORTH -> JY290_DOOR_OPEN_NORTH_AABB.move(-1, 0, 0);
            case SOUTH -> JY290_DOOR_OPEN_SOUTH_AABB.move(1, 0, 0);
            case WEST -> JY290_DOOR_OPEN_WEST_AABB.move(0, 0, 1);
            case EAST -> JY290_DOOR_OPEN_EAST_AABB.move(0, 0, -1);
            default -> JY290_DOOR_OPEN_NORTH_AABB.move(1, 0, 0);
        };
    }

    public static VoxelShape getJY290DoorCloseShape(DoorHingeSide hinge, Direction direction) {
        if (hinge == DoorHingeSide.RIGHT) {
            return switch (direction) {
                case NORTH -> JY290_DOOR_CLOSE_NORTH_AABB;
                case SOUTH -> JY290_DOOR_CLOSE_SOUTH_AABB;
                case WEST -> JY290_DOOR_CLOSE_WEST_AABB;
                case EAST -> JY290_DOOR_CLOSE_EAST_AABB;
                default -> JY290_DOOR_CLOSE_NORTH_AABB;
            };
        }
        return switch (direction) {
            case NORTH -> JY290_DOOR_CLOSE_NORTH_AABB.move(-1, 0, 0);
            case SOUTH -> JY290_DOOR_CLOSE_SOUTH_AABB.move(1, 0, 0);
            case WEST -> JY290_DOOR_CLOSE_WEST_AABB.move(0, 0, 1);
            case EAST -> JY290_DOOR_CLOSE_EAST_AABB.move(0, 0, -1);
            default -> JY290_DOOR_CLOSE_NORTH_AABB.move(1, 0, 0);
        };
    }

    public static final VoxelShape CARRIAGEUNDERGROUND_WEST;
    public static final VoxelShape CARRIAGEUNDERGROUND_SOUTH;
    public static final VoxelShape CARRIAGEUNDERGROUND_EAST;

    static {
        CARRIAGEUNDERGROUND_EAST = rotateShape(Direction.NORTH, Direction.EAST, CARRIAGEUNDERGROUND_NORTH);
        CARRIAGEUNDERGROUND_SOUTH = rotateShape(Direction.NORTH, Direction.SOUTH, CARRIAGEUNDERGROUND_NORTH);
        CARRIAGEUNDERGROUND_WEST = rotateShape(Direction.NORTH, Direction.WEST, CARRIAGEUNDERGROUND_NORTH);
    }

    public static VoxelShape getCarriageUndergroundShape(Direction direction) {
        return switch (direction) {
            case EAST -> CARRIAGEUNDERGROUND_EAST;
            case NORTH -> CARRIAGEUNDERGROUND_NORTH;
            case WEST -> CARRIAGEUNDERGROUND_WEST;
            case SOUTH -> CARRIAGEUNDERGROUND_SOUTH;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getOverheadLinePillarShape(Direction direction) {
        // 根据不同朝向返回旋转后的形状
        return switch (direction) {
            case NORTH -> rotateShape(Direction.NORTH, direction, OverheadLinePillar_AABB);
            case SOUTH -> rotateShape(Direction.SOUTH,direction, OverheadLinePillar_AABB);
            case WEST -> rotateShape(Direction.WEST, direction, OverheadLinePillar_AABB);
            case EAST -> OverheadLinePillar_AABB;
            default -> OverheadLinePillar_AABB;
        };
    }

    public static VoxelShape getOverheadSmallTrussShape(Direction direction) {
        return switch (direction) {
            case EAST, WEST -> OverheadSmallTruss_WE_AABB;
            case NORTH, SOUTH -> OverheadSmallTruss_NS_AABB;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getOverheadBigTrussShape(Direction direction) {
        return switch (direction) {
            case EAST, WEST -> OverheadBigTruss_WE_AABB;
            case NORTH, SOUTH -> OverheadBigTruss_NS_AABB;
            default -> Shapes.block();
        };
    }

    public static VoxelShape getOverheadPillarTrussShape(Direction direction) {
        return switch (direction) {
            case EAST, WEST -> OverheadPillarTruss_WE_AABB;
            case NORTH, SOUTH -> OverheadPillarTruss_NS_AABB;
            default -> Shapes.block();
        };
    }

    //以下为自动旋转碰撞箱方法
    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};

        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.joinUnoptimized(
                    buffer[1],
                    Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX),
                    BooleanOp.OR
            ));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }

        return buffer[0];
    }
}
