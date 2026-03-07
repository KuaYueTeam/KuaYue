package cn.kycraft.kuayue.interlocking.signal.circuit;

import com.simibubi.create.content.trains.entity.Train;
import lib.kasuga.create.content.train.signal.CustomTrackSegment;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrackCircuitSegment extends CustomTrackSegment {
    public TrackCircuitSegment(UUID segmentId) {
        super(segmentId);
    }

    protected Set<Train> occupyTrains = new HashSet<>();

    @Override
    public void onEarlyGlobalTick() {
        occupyTrains.clear();
    }

    public void occupy(Train train) {
        this.occupyTrains.add(train);
    }

    public boolean hasOccupied() {
        return !occupyTrains.isEmpty();
    }

    public void receive(Set<Object> signalReceived) {

    }

    public Set<Train> getOccupyTrains() {
        return occupyTrains;
    }
}
