package cn.kycraft.kuayue.packs.network;

import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.PackPathRules;
import cn.kycraft.kuayue.packs.SemanticVersion;
import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.StartupPackSnapshot;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Canonical ID/version/format set used by the configuration-phase gate. */
public record PackSetDeclaration(List<Entry> entries) {
    public static final int MAX_ENTRIES = 256;

    public PackSetDeclaration {
        Objects.requireNonNull(entries, "entries");
        entries = entries.stream()
                .sorted(Comparator.comparing(Entry::id))
                .toList();
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many pack entries");
        }
        Set<String> ids = new HashSet<>();
        for (Entry entry : entries) {
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("Duplicate pack ID: " + entry.id());
            }
        }
    }

    public static PackSetDeclaration current() {
        if (!PackRuntimeState.isRegistrationSelectionFinalized()) {
            throw new IllegalStateException("Pack registration selection is not finalized");
        }
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot()
                .orElseThrow(() -> new IllegalStateException("Pack snapshot is not initialized"));
        Set<String> active = PackRuntimeState.registeredPackIds();
        List<Entry> entries = new ArrayList<>();
        for (String id : active) {
            PackManifest manifest = snapshot.knownManifests().get(id);
            if (manifest == null) {
                throw new IllegalStateException("Active pack has no manifest: " + id);
            }
            entries.add(new Entry(
                    manifest.id(),
                    manifest.version().toString(),
                    manifest.formatVersion()
            ));
        }
        return new PackSetDeclaration(entries);
    }

    public boolean compatibleWith(PackSetDeclaration other) {
        return entries.equals(other.entries);
    }

    public record Entry(String id, String version, int formatVersion) {
        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(version, "version");
            if (!PackPathRules.isPackId(id)) {
                throw new IllegalArgumentException("Invalid pack ID: " + id);
            }
            SemanticVersion.parse(version);
            if (formatVersion != PackManifest.FORMAT_VERSION) {
                throw new IllegalArgumentException("Unsupported pack format: " + formatVersion);
            }
        }
    }
}
