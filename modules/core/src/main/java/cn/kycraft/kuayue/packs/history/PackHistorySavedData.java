package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.SemanticVersion;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Version history for the packs that last successfully used an Overworld save.
 * Presence alone is not history: only {@link #initialized()} separates a known
 * empty history from a world that has not recorded one yet.
 */
public final class PackHistorySavedData extends SavedData {
    public static final String DATA_NAME = PackHistoryState.DATA_NAME;
    public static final int SCHEMA_VERSION = 1;
    public static final SavedData.Factory<PackHistorySavedData> FACTORY =
            new SavedData.Factory<>(PackHistorySavedData::new, PackHistorySavedData::load);

    private static final String SCHEMA_VERSION_FIELD = "schema_version";
    private static final String INITIALIZED_FIELD = "initialized";
    private static final String PACKS_FIELD = "packs";
    private static final String ID_FIELD = "id";
    private static final String VERSION_FIELD = "version";
    private static final String FORMAT_VERSION_FIELD = "format_version";
    private static final Set<String> TOP_LEVEL_FIELDS =
            Set.of(SCHEMA_VERSION_FIELD, INITIALIZED_FIELD, PACKS_FIELD);
    private static final Set<String> PACK_FIELDS =
            Set.of(ID_FIELD, VERSION_FIELD, FORMAT_VERSION_FIELD);

    private PackHistoryState state;

    /**
     * Factory constructor for a world that has no existing history.
     */
    public PackHistorySavedData() {
        state = PackHistoryState.missing();
    }

    private PackHistorySavedData(List<PackHistoryEntry> packs) {
        state = PackHistoryState.known(packs);
    }

    public static PackHistorySavedData initialized(
            Collection<? extends HistoryPack> packs
    ) {
        return new PackHistorySavedData(toEntries(packs));
    }

    public static PackHistorySavedData load(CompoundTag payload, HolderLookup.Provider registries) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(registries, "registries");
        try {
            requireTag(payload, SCHEMA_VERSION_FIELD, Tag.TAG_INT);
            int schemaVersion = payload.getInt(SCHEMA_VERSION_FIELD);
            if (schemaVersion != SCHEMA_VERSION) {
                throw new PackHistoryLoadException(
                        PackHistoryLoadException.Reason.UNSUPPORTED_SCHEMA,
                        "Unsupported KuaYue pack history schema: " + schemaVersion);
            }
            requireExactFields(payload, TOP_LEVEL_FIELDS, "pack history");

            requireTag(payload, INITIALIZED_FIELD, Tag.TAG_BYTE);
            if (!payload.getBoolean(INITIALIZED_FIELD)) {
                throw corrupt("Persisted pack history is not initialized");
            }

            requireTag(payload, PACKS_FIELD, Tag.TAG_LIST);
            ListTag rawPacks = (ListTag) payload.get(PACKS_FIELD);
            ListTag compounds = payload.getList(PACKS_FIELD, Tag.TAG_COMPOUND);
            if (rawPacks == null || rawPacks.size() != compounds.size()) {
                throw corrupt("Pack history contains a non-compound entry");
            }

            List<PackHistoryEntry> parsed = new ArrayList<>(compounds.size());
            for (int index = 0; index < compounds.size(); index++) {
                parsed.add(parsePack(compounds.getCompound(index), index));
            }
            return new PackHistorySavedData(parsed);
        } catch (PackHistoryLoadException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PackHistoryLoadException(
                    PackHistoryLoadException.Reason.CORRUPT,
                    "Invalid KuaYue pack history payload",
                    exception);
        }
    }

    @Override
    public CompoundTag save(CompoundTag payload, HolderLookup.Provider registries) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(registries, "registries");
        if (!state.initialized()) {
            throw new IllegalStateException(
                    "An uninitialized pack history must not be persisted");
        }

        for (String key : List.copyOf(payload.getAllKeys())) {
            payload.remove(key);
        }
        ListTag serializedPacks = new ListTag();
        for (PackHistoryEntry pack : state.packs()) {
            CompoundTag serialized = new CompoundTag();
            serialized.putString(ID_FIELD, pack.id());
            serialized.putString(VERSION_FIELD, pack.version().toString());
            serialized.putInt(FORMAT_VERSION_FIELD, pack.formatVersion());
            serializedPacks.add(serialized);
        }

        payload.putInt(SCHEMA_VERSION_FIELD, SCHEMA_VERSION);
        payload.putBoolean(INITIALIZED_FIELD, true);
        payload.put(PACKS_FIELD, serializedPacks);
        return payload;
    }

    public boolean initialized() {
        return state.initialized();
    }

    public List<PackHistoryEntry> packs() {
        return state.packs();
    }

    public PackHistoryState state() {
        return state;
    }

    /**
     * Replaces history after the world has passed compatibility checks.
     *
     * @return whether the stored history changed
     */
    public boolean initialize(Collection<? extends HistoryPack> newPacks) {
        PackHistoryState validated = PackHistoryState.known(toEntries(newPacks));
        if (state.equals(validated)) {
            return false;
        }
        state = validated;
        setDirty();
        return true;
    }

    private static PackHistoryEntry parsePack(CompoundTag tag, int index) {
        requireExactFields(tag, PACK_FIELDS, "pack history entry " + index);
        requireTag(tag, ID_FIELD, Tag.TAG_STRING);
        requireTag(tag, VERSION_FIELD, Tag.TAG_STRING);
        requireTag(tag, FORMAT_VERSION_FIELD, Tag.TAG_INT);
        try {
            return new PackHistoryEntry(
                    tag.getString(ID_FIELD),
                    SemanticVersion.parse(tag.getString(VERSION_FIELD)),
                    tag.getInt(FORMAT_VERSION_FIELD));
        } catch (IllegalArgumentException exception) {
            throw new PackHistoryLoadException(
                    PackHistoryLoadException.Reason.CORRUPT,
                    "Invalid pack history entry at index " + index,
                    exception);
        }
    }

    private static List<PackHistoryEntry> toEntries(
            Collection<? extends HistoryPack> packs
    ) {
        Objects.requireNonNull(packs, "packs");
        List<PackHistoryEntry> entries = new ArrayList<>(packs.size());
        for (HistoryPack pack : packs) {
            Objects.requireNonNull(pack, "pack");
            entries.add(new PackHistoryEntry(
                    pack.id(), pack.version(), pack.formatVersion()));
        }
        return List.copyOf(entries);
    }

    private static void requireTag(CompoundTag tag, String field, int type) {
        if (!tag.contains(field, type)) {
            throw corrupt("Missing or invalid field '" + field + "'");
        }
    }

    private static void requireExactFields(
            CompoundTag tag,
            Set<String> expected,
            String description
    ) {
        if (!tag.getAllKeys().equals(expected)) {
            throw corrupt(
                    "Unexpected fields in " + description + ": "
                            + tag.getAllKeys().stream()
                            .filter(field -> !expected.contains(field))
                            .sorted()
                            .toList());
        }
    }

    private static PackHistoryLoadException corrupt(String message) {
        return new PackHistoryLoadException(PackHistoryLoadException.Reason.CORRUPT, message);
    }
}
