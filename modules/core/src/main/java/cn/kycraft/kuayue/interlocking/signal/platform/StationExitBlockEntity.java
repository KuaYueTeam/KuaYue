package cn.kycraft.kuayue.interlocking.signal.platform;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class StationExitBlockEntity extends SmartBlockEntity {
    private static TrackTargetingBehaviour<StationExit> edgePoint;
    public StationExitBlockEntity(BlockPos pos, BlockState state) {
        super(SignalModule.STATION_EXIT.getBlockEntity().getEntry(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
        list.add(edgePoint = new TrackTargetingBehaviour<>(this, SignalModule.STATION_EXIT.getEdgePoint().getEntry()));
    }
}
