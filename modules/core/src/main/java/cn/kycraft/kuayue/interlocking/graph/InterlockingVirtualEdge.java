package cn.kycraft.kuayue.interlocking.graph;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackEdge;
import lib.kasuga.create.content.train.graph.TrackEdgeLocation;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class InterlockingVirtualEdge {
    List<TrackEdgeLocation> edgeLocations;

    InterlockingVirtualEdge(List<TrackEdgeLocation> edgeLocations) {
        this.edgeLocations = edgeLocations;
    }

    public static InterlockingVirtualEdge fromTrack(InterlockingVirtualTrack track) {
        List<TrackEdgeLocation> locations = new ArrayList<>();
        for (Couple<TrackEdge> edge : track.edges) {
            locations.add(TrackEdgeLocation.fromEdge(edge.getFirst()));
        }
        return new InterlockingVirtualEdge(locations);
    }

    public static InterlockingVirtualEdge read(CompoundTag data, DimensionPalette dimensions) {
        ListTag edgesTag = data.getList("EdgeLocations", Tag.TAG_COMPOUND);
        List<TrackEdgeLocation> locations = new ArrayList<>();
        for (Tag edgeTag : edgesTag) {
            locations.add(TrackEdgeLocation.read((CompoundTag) edgeTag, dimensions));
        }
        return new InterlockingVirtualEdge(locations);
    }

    public CompoundTag write(DimensionPalette dimensions) {
        CompoundTag edgeTag = new CompoundTag();
        ListTag edgesTag = new ListTag();
        for (TrackEdgeLocation location : edgeLocations) {
            edgesTag.add(location.write(dimensions));
        }
        edgeTag.put("EdgeLocations", edgesTag);
        return edgeTag;
    }
}
