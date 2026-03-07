package cn.kycraft.kuayue.interlocking.graph;

import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.createmod.catnip.data.Couple;

import java.util.*;


public class InterlockingVirtualTrack {
    TrackNode locationA;
    TrackEdgePoint edgePointA;
    boolean frozenA;

    TrackNode locationB;
    TrackEdgePoint edgePointB;
    boolean frozenB;

    protected ArrayDeque<Couple<TrackEdge>> edges = new ArrayDeque<>();

    public InterlockingVirtualTrack(TrackNode locationA, TrackNode locationB) {
        this.locationA = locationA;
        this.locationB = locationB;
    }

    public void updateEdgePoint(TrackNode iterNode, TrackNode nextNode, TrackEdge edge, TrackEdge opposite) {
        Couple<TrackEdge> edgesCouple = Couple.create(edge, opposite);
        if(locationA == iterNode) {
            if(edge.node1 == iterNode) {
                edgesCouple = edgesCouple.swap();
            }
            locationA = nextNode;
            edges.addFirst(edgesCouple);
        } else if(locationB == iterNode) {
            if(edge.node1 == iterNode) {
                edgesCouple = edgesCouple.swap();
            }
            locationB = nextNode;
            edges.addLast(edgesCouple.swap());
        } else {
            throw new IllegalStateException("Unexpected node in InterlockingVirtualTrack");
        }
    }

    public boolean isFrozen(TrackNode iterNode) {
        if(locationA == iterNode) {
            return frozenA;
        } else if(locationB == iterNode) {
            return frozenB;
        } else {
            throw new IllegalStateException("Unexpected node in InterlockingVirtualTrack");
        }
    }

    public void cutFrom(TrackNode iterNode, TrackEdgePoint cutOffEdgePoint) {
        if(locationA == iterNode) {
            edgePointA = cutOffEdgePoint;
            frozenA = true;
        } else if(locationB == iterNode) {
            edgePointB = cutOffEdgePoint;
            frozenB = true;
        } else {
            throw new IllegalStateException("Unexpected node in InterlockingVirtualTrack");
        }
    }

    public void addEdge(TrackEdge edge, TrackEdge opposite) {
        this.edges.add(Couple.create(edge, opposite));
    }

    protected void assertCheck() {
        TrackNode node = locationA;
        ArrayDeque<Couple<TrackEdge>> checkedEdges = new ArrayDeque<>();
        int index = 0;
        for (Couple<TrackEdge> edge : this.edges) {
            if(edge.getFirst().node2 == node) {
                checkedEdges.add(edge.swap());
                continue;
            }
            if(edge.getFirst().node1 != node) {
                throw new IllegalStateException("Edge does not connect to current node in InterlockingVirtualTrack index=" + index);
            }
            checkedEdges.add(edge);
            node = edge.getFirst().node2;
            index ++;
        }
        this.edges = checkedEdges;
        if(node != locationB)
            throw new IllegalStateException("Final node does not match locationB in InterlockingVirtualTrack");
    }


    public List<InterlockingVirtualTrack> split(Collection<EdgePointType<?>> point) {
        this.assertCheck();
        List<Couple<TrackEdge>> previous = new ArrayList<>();
        TrackEdgePoint previousPoint = edgePointA;

        List<InterlockingVirtualTrack> virtualTracks = new ArrayList<>();


        for (Couple<TrackEdge> edge : this.edges) {
            previous.add(edge);
            double length = edge.getFirst().getLength(), startPoint = 0.0D, stopPoint = length;
            if(edge.getFirst().node1 == locationA && edgePointA != null) {
                startPoint = edgePointA.getLocationOn(edge.getFirst());
            }
            if(edge.getFirst().node2 == locationB && edgePointB != null) {
                stopPoint = edgePointB.getLocationOn(edge.getFirst());
            }
            EdgeData data = edge.getFirst().getEdgeData();
            double currentPos = startPoint;
            while(true) {
                TrackEdgePoint nextPoint = data.next(currentPos);
                if(nextPoint == null)
                    break;
                if(!point.contains(nextPoint.getType()))
                    continue;
                double pos = nextPoint.getLocationOn(edge.getFirst());
                if(pos > stopPoint)
                    break;
                InterlockingVirtualTrack virtualTrack = new InterlockingVirtualTrack(
                        previous.getFirst().getFirst().node1,
                        previous.getLast().getFirst().node2
                );
                virtualTrack.edgePointA = previousPoint;
                virtualTrack.edgePointB = nextPoint;
                previousPoint = nextPoint;
                virtualTrack.addEdges(previous);
                virtualTracks.add(virtualTrack);
                previous.clear();
                if(pos != stopPoint) {
                    previous.add(edge);
                } else {
                    previousPoint = null;
                }
                currentPos = pos;
            }
        }


        if(!previous.isEmpty()) {
            InterlockingVirtualTrack virtualTrack = new InterlockingVirtualTrack(
                    previous.getFirst().getFirst().node1,
                    previous.getLast().getFirst().node2
            );
            virtualTrack.edgePointA = previousPoint;
            virtualTrack.edgePointB = edgePointB;
            virtualTrack.addEdges(previous);
            virtualTracks.add(virtualTrack);
            previous.clear();
        }


        return virtualTracks;
    }

    private void addEdges(List<Couple<TrackEdge>> previous) {
        this.edges.addAll(previous);
    }

    public InterlockingVirtualNode getConnectionPointA() {
        return new InterlockingVirtualNode(locationA.getLocation(), edgePointA == null ? null : edgePointA.id, edgePointA == null ? null : edgePointA.getType());
    }

    public InterlockingVirtualNode getConnectionPointB() {
        return new InterlockingVirtualNode(locationB.getLocation(), edgePointB == null ? null : edgePointB.id, edgePointB == null ? null : edgePointB.getType());
    }
}
