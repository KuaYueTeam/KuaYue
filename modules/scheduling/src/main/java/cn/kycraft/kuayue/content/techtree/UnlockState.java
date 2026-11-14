package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Per-player tech tree progress, persisted through a NeoForge data attachment. */
public final class UnlockState {

    public final Set<NodeLocation> unlocked = new LinkedHashSet<>();
    public final Set<ResourceLocation> unlockedGroups = new LinkedHashSet<>();
    public final Set<ResourceLocation> visibleGroups = new LinkedHashSet<>();
    public final Set<NodeLocation> visibleNodes = new LinkedHashSet<>();

    public boolean hasUnlocked(NodeLocation location) {
        return unlocked.contains(location);
    }

    public void copyFrom(UnlockState other) {
        unlocked.clear();
        unlockedGroups.clear();
        visibleGroups.clear();
        visibleNodes.clear();
        unlocked.addAll(other.unlocked);
        unlockedGroups.addAll(other.unlockedGroups);
        visibleGroups.addAll(other.visibleGroups);
        visibleNodes.addAll(other.visibleNodes);
    }

    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("nodes", strings(unlocked.stream().map(NodeLocation::toString).toList()));
        tag.put("unlock_groups", strings(unlockedGroups.stream().map(ResourceLocation::toString).toList()));
        tag.put("visible_groups", strings(visibleGroups.stream().map(ResourceLocation::toString).toList()));
        tag.put("visible_nodes", strings(visibleNodes.stream().map(NodeLocation::toString).toList()));
    }

    public void read(CompoundTag tag, HolderLookup.Provider registries) {
        unlocked.clear();
        unlockedGroups.clear();
        visibleGroups.clear();
        visibleNodes.clear();
        readStrings(tag, "nodes", raw -> unlocked.add(NodeLocation.parseFull(raw)));
        readStrings(tag, "unlock_groups", raw -> unlockedGroups.add(ResourceLocation.parse(raw)));
        readStrings(tag, "visible_groups", raw -> visibleGroups.add(ResourceLocation.parse(raw)));
        readStrings(tag, "visible_nodes", raw -> visibleNodes.add(NodeLocation.parseFull(raw)));
    }

    public void writeNetwork(FriendlyByteBuf buf) {
        writeSet(buf, unlocked, (value, out) -> value.write(out));
        writeSet(buf, unlockedGroups, (value, out) -> out.writeResourceLocation(value));
        writeSet(buf, visibleGroups, (value, out) -> out.writeResourceLocation(value));
        writeSet(buf, visibleNodes, (value, out) -> value.write(out));
    }

    public void readNetwork(FriendlyByteBuf buf) {
        readSet(buf, unlocked, NodeLocation::read);
        readSet(buf, unlockedGroups, FriendlyByteBuf::readResourceLocation);
        readSet(buf, visibleGroups, FriendlyByteBuf::readResourceLocation);
        readSet(buf, visibleNodes, NodeLocation::read);
    }

    private static ListTag strings(java.util.Collection<String> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        return list;
    }

    private static void readStrings(CompoundTag tag, String key, java.util.function.Consumer<String> consumer) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        list.forEach(child -> {
            try {
                consumer.accept(child.getAsString());
            } catch (RuntimeException ignored) {
                // A namespace removed from the pack must not invalidate the whole profile.
            }
        });
    }

    private static <T> void writeSet(FriendlyByteBuf buf, java.util.Collection<T> values,
                                     java.util.function.BiConsumer<T, FriendlyByteBuf> writer) {
        buf.writeVarInt(values.size());
        values.forEach(value -> writer.accept(value, buf));
    }

    private static <T> void readSet(FriendlyByteBuf buf, java.util.Collection<T> target,
                                    java.util.function.Function<FriendlyByteBuf, T> reader) {
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            target.add(reader.apply(buf));
        }
    }
}
