package cn.kycraft.kuayue.interlocking.graph;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.codec.ByteBufCodecs;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class InterlockingVirtualGraph<C extends InterlockingVirtualEdge> extends AbstractBaseGraph<InterlockingVirtualNode, C> {
    public InterlockingVirtualGraph(GraphType type) {
        super(null, null, type);
    }

    public InterlockingVirtualGraph() {
        this((new DefaultGraphType.Builder())
                .undirected()
                .allowMultipleEdges(false)
                .allowSelfLoops(false)
                .weighted(false)
                .build());
    }

    public static InterlockingVirtualGraph<InterlockingVirtualEdge> fromTracks(Collection<InterlockingVirtualTrack> tracks) {
        InterlockingVirtualGraph<InterlockingVirtualEdge> graph = new InterlockingVirtualGraph<>();
        for (InterlockingVirtualTrack track : tracks) {
            graph.addEdge(
                    track.getConnectionPointA(),
                    track.getConnectionPointB(),
                    InterlockingVirtualEdge.fromTrack(track)
            );
        }
        return graph;
    }

    public void write(CompoundTag nbt, DimensionPalette dimensions) {
        int indexer = 0;
        Map<InterlockingVirtualNode, Integer> vertexIndex = new HashMap<>();
        ListTag verticesTag = new ListTag();
        for (InterlockingVirtualNode node : vertexSet()) {
            CompoundTag nodeTag = new CompoundTag();
            vertexIndex.put(node, indexer);
            nodeTag.putInt("Index", indexer++);
            node.write(nodeTag, dimensions);
            verticesTag.add(nodeTag);
        }
        nbt.put("Vertices", verticesTag);

        ListTag edgesTag = new ListTag();
        for (C edge : edgeSet()) {
            CompoundTag edgeTag = new CompoundTag();
            edgeTag.putInt("From", vertexIndex.get(getEdgeSource(edge)));
            edgeTag.putInt("To", vertexIndex.get(getEdgeTarget(edge)));
            edgeTag.put("Data", edge.write(dimensions));
            edgesTag.add(edgeTag);
        }
        nbt.put("Edges", edgesTag);
    }

    public static <C extends InterlockingVirtualEdge> InterlockingVirtualGraph<C> read(
            CompoundTag nbt,
            DimensionPalette dimensions,
            BiFunction<CompoundTag, DimensionPalette, C> reader
    ) {
        InterlockingVirtualGraph<C> graph = new InterlockingVirtualGraph<>();
        Map<Integer, InterlockingVirtualNode> indexToNode = new HashMap<>();

        ListTag verticesTag = nbt.getList("Vertices", 10);
        for (int i = 0; i < verticesTag.size(); i++) {
            CompoundTag nodeTag = verticesTag.getCompound(i);
            int index = nodeTag.getInt("Index");
            InterlockingVirtualNode node = InterlockingVirtualNode.read(nodeTag, dimensions);
            indexToNode.put(index, node);
            graph.addVertex(node);
        }

        ListTag edgesTag = nbt.getList("Edges", 10);
        for (int i = 0; i < edgesTag.size(); i++) {
            CompoundTag edgeTag = edgesTag.getCompound(i);
            int fromIndex = edgeTag.getInt("From");
            int toIndex = edgeTag.getInt("To");
            C edge = reader.apply(edgeTag.getCompound("Data"), dimensions);
            graph.addEdge(indexToNode.get(fromIndex), indexToNode.get(toIndex), edge);
        }

        return graph;
    }
}
