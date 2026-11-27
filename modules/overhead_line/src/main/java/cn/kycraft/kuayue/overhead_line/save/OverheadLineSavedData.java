package cn.kycraft.kuayue.overhead_line.save;

import cn.kycraft.kuayue.overhead_line.block.line.OverheadLineConnection;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineEndpoint;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineWireKind;
import lib.kasuga.inject.auto_configure.Saved;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class OverheadLineSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCHEMA_VERSION = 1;
    public static final Saved.Impl<OverheadLineSavedData> STORAGE = new Saved.Impl<>(
            "kuayue_overhead_line", new Factory<>(OverheadLineSavedData::new, OverheadLineSavedData::load));

    public enum AddResult {
        ADDED, DUPLICATE, SAME_SUPPORT, FROM_FULL, TO_FULL, BOTH_FULL, STORAGE_UNAVAILABLE
    }

    private final Set<StoredConnection> connections = new LinkedHashSet<>();
    private CompoundTag unreadableData;

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (unreadableData != null) return unreadableData.copy();
        tag.putInt("schema_version", SCHEMA_VERSION);
        var list = new ListTag();
        for (var connection : connections) {
            var entry = new CompoundTag();
            entry.putLong("from_pos", connection.fromPos.asLong());
            entry.putInt("from_index", connection.fromIndex);
            entry.putLong("to_pos", connection.toPos.asLong());
            entry.putInt("to_index", connection.toIndex);
            entry.putString("wire", switch (connection.kind) {
                case OVERHEAD_LINE_WIRE -> "overhead_line_wire";
                case ELECTRONIC_WIRE -> "electronic_wire";
            });
            list.add(entry);
        }
        tag.put("connections", list);
        return tag;
    }

    public static OverheadLineSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new OverheadLineSavedData();
        try {
            if (!tag.contains("schema_version", Tag.TAG_INT) || tag.getInt("schema_version") != SCHEMA_VERSION) {
                throw new IllegalArgumentException("unsupported schema_version: " + tag.get("schema_version"));
            }
            if (!(tag.get("connections") instanceof ListTag list)) {
                throw new IllegalArgumentException("missing connections list");
            }
            for (Tag item : list) {
                if (!(item instanceof CompoundTag entry)
                        || !entry.contains("from_pos", Tag.TAG_LONG) || !entry.contains("to_pos", Tag.TAG_LONG)
                        || !entry.contains("from_index", Tag.TAG_INT) || !entry.contains("to_index", Tag.TAG_INT)) {
                    throw new IllegalArgumentException("invalid connection endpoints");
                }
                var from = BlockPos.of(entry.getLong("from_pos"));
                var to = BlockPos.of(entry.getLong("to_pos"));
                int fromIndex = entry.getInt("from_index");
                int toIndex = entry.getInt("to_index");
                if (from.equals(to) || fromIndex < 0 || toIndex < 0) {
                    throw new IllegalArgumentException("invalid connection at " + from + " -> " + to);
                }
                var kind = switch (entry.getString("wire")) {
                    case "overhead_line_wire" -> OverheadLineWireKind.OVERHEAD_LINE_WIRE;
                    case "electronic_wire" -> OverheadLineWireKind.ELECTRONIC_WIRE;
                    default -> throw new IllegalArgumentException("unknown wire: " + entry.get("wire"));
                };
                if (Long.compare(from.asLong(), to.asLong()) > 0) {
                    data.connections.add(new StoredConnection(to, toIndex, from, fromIndex, kind));
                } else {
                    data.connections.add(new StoredConnection(from, fromIndex, to, toIndex, kind));
                }
            }
        } catch (IllegalArgumentException error) {
            LOGGER.error("Unable to load overhead line data. Editing this dimension's connections is disabled.", error);
            data.connections.clear();
            data.unreadableData = tag.copy();
        }
        return data;
    }

    public AddResult add(OverheadLineConnection link, int fromMax, int toMax) {
        if (unreadableData != null) return AddResult.STORAGE_UNAVAILABLE;
        var stored = StoredConnection.from(link);
        var from = stored.fromPos();
        var to = stored.toPos();
        if (from.equals(to)) return AddResult.SAME_SUPPORT;
        if (connections.contains(stored)) return AddResult.DUPLICATE;

        int fromCount = 0;
        int toCount = 0;
        for (var existing : connections) {
            if (existing.touches(from)) fromCount++;
            if (existing.touches(to)) toCount++;
        }
        if (fromCount >= fromMax && toCount >= toMax) return AddResult.BOTH_FULL;
        if (fromCount >= fromMax) return AddResult.FROM_FULL;
        if (toCount >= toMax) return AddResult.TO_FULL;
        connections.add(stored);
        setDirty();
        return AddResult.ADDED;
    }

    public List<OverheadLineConnection> removeAt(ResourceKey<Level> dimension, BlockPos pos) {
        if (unreadableData != null) return List.of();
        var removed = connections.stream().filter(connection -> connection.touches(pos)).toList();
        if (removed.isEmpty()) return List.of();
        connections.removeAll(removed);
        setDirty();
        return removed.stream().map(connection -> connection.inDimension(dimension)).toList();
    }

    public Stream<OverheadLineConnection> connections(ResourceKey<Level> dimension) {
        return connections.stream().map(connection -> connection.inDimension(dimension));
    }

    private record StoredConnection(BlockPos fromPos, int fromIndex, BlockPos toPos, int toIndex, OverheadLineWireKind kind) {
        static StoredConnection from(OverheadLineConnection link) {
            return new StoredConnection(link.from().blockPos().immutable(), link.from().pointIndex(),
                    link.to().blockPos().immutable(), link.to().pointIndex(), link.kind());
        }

        OverheadLineConnection inDimension(ResourceKey<Level> dimension) {
            return new OverheadLineConnection(new OverheadLineEndpoint(dimension, fromPos, fromIndex),
                    new OverheadLineEndpoint(dimension, toPos, toIndex), kind);
        }

        boolean touches(BlockPos pos) {
            return fromPos.equals(pos) || toPos.equals(pos);
        }
    }
}
