package cn.kycraft.kuayue.interlocking.signal.circuit;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.observer.TrackObserver;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import lib.kasuga.KasugaLib;
import lib.kasuga.create.content.train.graph.EdgeExtraData;
import lib.kasuga.create.content.train.graph.GraphExtraData;
import lib.kasuga.create.content.train.graph.RailwayManager;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TrainSignalCollector {
    public static Set<UUID> collectInitiallyOccupiedSignalBlocks(Train train) {
        TravellingPoint trailingPoint = train.carriages.getLast()
                .getTrailingPoint();
        TrackNode node1 = trailingPoint.node1;
        TrackNode node2 = trailingPoint.node2;
        TrackEdge edge = trailingPoint.edge;

        if (edge == null) return null;

        Set<UUID> uuids = new HashSet<>();

        double position = trailingPoint.position;
        EdgeData signalData = edge.getEdgeData();

        GraphExtraData extraData = KasugaLib.getBean(RailwayManager.class).getData().withGraph(train.graph);
        EdgeExtraData signalDataExtra = extraData.getOrComputeEdgeData(edge);


        TravellingPoint signalScout = new TravellingPoint(node1, node2, edge, position, false);
        MutableObject<UUID> prevGroup = new MutableObject<>(null);

        EdgePointType<TrackInsulator> TYPE = SignalModule.INSULATOR.getEdgePoint().getEntry();

        if (signalDataExtra.hasCustomBoundaryInThisEdge(SignalModule.INSULATOR_SEGMENT_ID)) {
            TrackInsulator nextBoundary = signalData.next(TYPE, position);
            if (nextBoundary == null) {
                double d = 0;
                TrackInsulator prev = null;
                TrackInsulator current = signalData.next(TYPE, 0);
                while (current != null) {
                    prev = current;
                    d = current.getLocationOn(edge);
                    current = signalData.next(TYPE, d);
                }
                if (prev != null) {
                    UUID group = prev.getGroupId(prev.isPrimary(node2));
                    uuids.add(group);
                }

            } else {
                UUID group = nextBoundary.getGroupId(nextBoundary.isPrimary(node1));
                uuids.add(group);
                prevGroup.setValue(group);
            }

        } else {
            UUID groupId = signalDataExtra.getBoundaryFeature(SignalModule.INSULATOR_SEGMENT_ID);
            if(groupId != null){
                if(groupId == EdgeExtraData.passiveBoundaryGroup)
                    groupId = train.graph.id;
                uuids.add(groupId);
                prevGroup.setValue(groupId);
            }
        }

        train.forEachTravellingPointBackwards((tp, d) -> {
            signalScout.travel(train.graph, d, signalScout.follow(tp), (distance, couple) -> {
                if (!(couple.getFirst() instanceof TrackInsulator signal))
                    return false;
                couple.getSecond()
                        .map((s)->signal.getGroupId(signal.isPrimary(s)))
                        .forEach(id -> {
                            if (id.equals(prevGroup.getValue()))
                                return;
                            uuids.add(id);
                            prevGroup.setValue(id);
                        });
                return false;
            }, signalScout.ignoreTurns());
        });

        return uuids;

    }
}
