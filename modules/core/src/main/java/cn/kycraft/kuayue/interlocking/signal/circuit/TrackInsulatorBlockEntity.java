package cn.kycraft.kuayue.interlocking.signal.circuit;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TrackInsulatorBlockEntity extends SmartBlockEntity {
    private TrackTargetingBehaviour<TrackInsulator> edgePoint;
    public TrackInsulatorBlockEntity(BlockPos pos, BlockState state) {
        super(SignalModule.INSULATOR.getBlockEntity().getEntry(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(edgePoint = new TrackTargetingBehaviour<>(this, SignalModule.INSULATOR.getEdgePoint().getEntry()));
    }
}
