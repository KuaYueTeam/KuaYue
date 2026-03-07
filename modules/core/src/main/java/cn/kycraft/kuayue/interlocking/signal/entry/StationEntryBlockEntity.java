package cn.kycraft.kuayue.interlocking.signal.entry;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class StationEntryBlockEntity extends SmartBlockEntity{
    private TrackTargetingBehaviour<StationEntry> edgePoint;

    public StationEntryBlockEntity(BlockPos blockPos, BlockState state) {
        super(SignalModule.STATION_ENTRY.getBlockEntity().getEntry(), blockPos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
        list.add(edgePoint = new TrackTargetingBehaviour<>(this, SignalModule.STATION_ENTRY.getEdgePoint().getEntry()));
    }
}
