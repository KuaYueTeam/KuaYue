package cn.kycraft.kuayue.interlocking.graph;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class InterlockingVirtualNode {
    TrackNodeLocation location;

    @Nullable UUID edgePointId;

    @Nullable
    EdgePointType<?> edgePointType;

    public InterlockingVirtualNode(TrackNodeLocation location, @Nullable UUID edgePointId, @Nullable EdgePointType<?> edgePointType) {
        this.location = location;
        this.edgePointId = edgePointId;
        this.edgePointType = edgePointType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof InterlockingVirtualNode that)) return false;
        return Objects.equals(location, that.location) && Objects.equals(edgePointId, that.edgePointId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, edgePointId);
    }

    public void write(CompoundTag nodeTag, DimensionPalette dimensions) {
        nodeTag.put("Location", location.write(dimensions));
        if(edgePointId != null) {
            nodeTag.putIntArray("EdgePointId", UUIDUtil.uuidToIntArray(edgePointId));
        }
        if(edgePointType != null) {
            nodeTag.putString("EdgePointType", edgePointType.getId().toString());
        }
    }

    public static InterlockingVirtualNode read(CompoundTag nodeTag, DimensionPalette dimensions) {
        TrackNodeLocation location = TrackNodeLocation.read(nodeTag.getCompound("Location"), dimensions);
        UUID edgePointId = null;
        if(nodeTag.contains("EdgePointId")) {
            edgePointId = UUIDUtil.uuidFromIntArray(nodeTag.getIntArray("EdgePointId"));
        }
        EdgePointType<?> edgePointType = null;
        if(nodeTag.contains("EdgePointType")) {
            ResourceLocation typeId = ResourceLocation.tryParse(nodeTag.getString("EdgePointType"));
            edgePointType = EdgePointType.TYPES.get(typeId);
        }
        return new InterlockingVirtualNode(location, edgePointId, edgePointType);
    }
}
