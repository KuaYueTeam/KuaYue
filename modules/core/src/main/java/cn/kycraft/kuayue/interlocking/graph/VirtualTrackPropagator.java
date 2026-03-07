package cn.kycraft.kuayue.interlocking.graph;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class VirtualTrackPropagator {

    public static ArrayList<InterlockingVirtualTrack> collect(
            TrackGraph graph,
            TrackEdge edge,
            double position,
            BiFunction<TrackEdge, Double, TrackEdgePoint> internalBoundaryQuery
    ){
        return collect(
                graph,
                edge,
                position,
                internalBoundaryQuery,
                (e)->internalBoundaryQuery.apply(e,0D),
                new HashSet<>(),
                new HashMap<>(),
                false
        );
    }
    public static ArrayList<InterlockingVirtualTrack> collect(
            TrackGraph graph,
            TrackEdge edge,
            double position,
            BiFunction<TrackEdge, Double, TrackEdgePoint> internalBoundaryQuery,
            Function<TrackEdge, TrackEdgePoint> internalBoundaryCheck,
            Set<TrackEdge> visited,
            HashMap<TrackNode, InterlockingVirtualTrack> virtualTrack,
            boolean noPropagate
    ) {
        ArrayList<InterlockingVirtualTrack> result = new ArrayList<>();
        TrackEdge opposite = graph.getConnectionsFrom(edge.node2).get(edge.node1);
        Couple<TrackEdgePoint> points = Couple.create(
                internalBoundaryQuery.apply(edge, position),
                internalBoundaryQuery.apply(opposite, opposite.getLength() - position)
        );

        if(points.both(Objects::nonNull)) {
            InterlockingVirtualTrack immediate = new InterlockingVirtualTrack(edge.node1, edge.node2);
            immediate.edgePointA = points.getFirst();
            immediate.edgePointB = points.getSecond();
            immediate.frozenA = true;
            immediate.frozenB = true;
            immediate.addEdge(edge, opposite);
            result.add(immediate);
            return result;
        } else if(points.either(Objects::nonNull)) {
            for (boolean s : Iterate.falseAndTrue) {
                TrackEdgePoint curPoint = points.get(s);

                //noinspection ConstantValue
                if(curPoint == null) continue;

                if(noPropagate) {
                    for (boolean b : Iterate.falseAndTrue) {
                        Couple<TrackNode> c = Couple.create(edge.node1, edge.node2);
                        if(b) c = c.swap();
                        InterlockingVirtualTrack ivt = virtualTrack.get(c.getFirst());
                        if(ivt == null)
                            continue;
                        ivt.updateEdgePoint(c.getFirst(), c.getSecond(), edge, opposite);
                        ivt.cutFrom(c.getSecond(), curPoint);
                        result.add(ivt);
                        return result;
                    }
                } else {
                    InterlockingVirtualTrack immediate = new InterlockingVirtualTrack(
                            edge.node1,
                            edge.node2
                    );
                    if(s) {
                        immediate.edgePointB = curPoint;
                        immediate.frozenB = true;
                    } else {
                        immediate.edgePointA = curPoint;
                        immediate.frozenA = true;
                    }
                    visited.add(edge);
                    visited.add(opposite);
                    immediate.addEdge(edge, opposite);
                    result.add(immediate);
                    TrackNode next = s ? edge.node1 : edge.node2;
                    virtualTrack.put(next, immediate);
                    Map<TrackNode, TrackEdge> frontier = graph.getConnectionsFrom(next);
                    for (TrackEdge value : frontier.values()) {
                        if(visited.contains(value))
                            continue;
                        result.addAll(collect(
                                graph,
                                value,
                                value.node1 == next ? 0D : value.getLength(),
                                internalBoundaryQuery,
                                internalBoundaryCheck,
                                visited,
                                virtualTrack,
                                true
                        ));
                    }
                    return result;
                }
            }
        }

        return collect(
                graph,
                new ArrayDeque<>(List.of(
                        Couple.create(edge.node1, edge.node2),
                        Couple.create(edge.node2, edge.node1)
                )),
                visited,
                virtualTrack,
                internalBoundaryCheck
        );
    }
    public static ArrayList<InterlockingVirtualTrack> collect(
            TrackGraph graph,
            Deque<Couple<TrackNode>> frontier,
            Set<TrackEdge> visited,
            HashMap<TrackNode, InterlockingVirtualTrack> virtualTrack,
            Function<TrackEdge, TrackEdgePoint> internalBoundaryCheck
    ) {

        ArrayList<InterlockingVirtualTrack> finalVirtualTracks = new ArrayList<>();

        Couple<TrackNode> initialCouple = frontier.getFirst();

        TrackEdge firstEdge = graph.getConnectionsFrom(initialCouple.getFirst()).get(initialCouple.getSecond());
        TrackEdge firstOppositeEdge = graph.getConnectionsFrom(initialCouple.getSecond()).get(initialCouple.getFirst());

        InterlockingVirtualTrack firstIVT = new InterlockingVirtualTrack(initialCouple.getFirst(), initialCouple.getSecond());
        finalVirtualTracks.add(firstIVT);
        firstIVT.addEdge(firstEdge, firstOppositeEdge);

        virtualTrack.put(initialCouple.getFirst(), firstIVT);
        virtualTrack.put(initialCouple.getSecond(), firstIVT);

        visited.add(firstEdge);
        visited.add(firstOppositeEdge);


        while (!frontier.isEmpty()) {
            Couple<TrackNode> couple = frontier.pollFirst();

            TrackNode currentNode = couple.getFirst();
            TrackNode prevNode = couple.getSecond();

            TrackNode iterNode = currentNode;
            TrackNode iterPrevNode = prevNode;

            Map<TrackNode, TrackEdge> connections = graph.getConnectionsFrom(iterNode);

            for (Map.Entry<TrackNode, TrackEdge> entry : connections.entrySet()) {
                TrackNode nextNode = entry.getKey();
                TrackEdge edge = entry.getValue();

                if (nextNode == iterPrevNode)
                    continue;

                if (!visited.add(edge))
                    continue;

                TrackEdge oppositeEdge = graph.getConnectionsFrom(nextNode)
                        .get(iterNode);
                visited.add(oppositeEdge);

                InterlockingVirtualTrack ivt = virtualTrack.get(iterNode);


                TrackEdgePoint cutOffEdgePoint = internalBoundaryCheck.apply(edge.node1 == iterNode ? edge : oppositeEdge);

                if(cutOffEdgePoint != null){
                    ivt.updateEdgePoint(iterNode, nextNode, edge, oppositeEdge);
                    ivt.cutFrom(nextNode, cutOffEdgePoint);
                    continue;
                }

                if(connections.size() <= 2 && !edge.isTurn() && !ivt.isFrozen(iterNode)) {
                    ivt.updateEdgePoint(iterNode, nextNode, edge, oppositeEdge);
                    if(graph.getConnectionsFrom(nextNode).size() <= 2) {
                        virtualTrack.put(nextNode, ivt);
                    }
                    virtualTrack.remove(iterNode);
                } else {
                    virtualTrack.computeIfAbsent(nextNode, (s)->{
                        InterlockingVirtualTrack newIvt = new InterlockingVirtualTrack(iterNode, nextNode);
                        newIvt.frozenA = true;
                        if(edge.isTurn() || oppositeEdge.isTurn()) {
                            newIvt.frozenB = true;
                        }
                        newIvt.addEdge(edge, oppositeEdge);
                        finalVirtualTracks.add(newIvt);

                        return newIvt;
                    });
                }

                frontier.add(Couple.create(nextNode, currentNode));
            }
        }

        return finalVirtualTracks;
    }

//    public static List<InterlockingVirtualTrack> propagate(
//            TrackGraph graph,
//            TrackEdge edge,
//            double position
//    ){
//
//    }
}
