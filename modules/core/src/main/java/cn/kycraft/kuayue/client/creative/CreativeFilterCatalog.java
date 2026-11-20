package cn.kycraft.kuayue.client.creative;

import cn.kycraft.kuayue.packs.CreativeFilter;
import cn.kycraft.kuayue.packs.ParcelManifest;
import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.StartupPackSnapshot;
import cn.kycraft.kuayue.packs.origin.SystemOriginTagIds;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import cn.kycraft.kuayue.train.KuaYueTrainCatalog;
import cn.kycraft.kuayue.train.KuaYueTrainDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Client catalog projected from frozen parcel metadata; it never reads installation files. */
public final class CreativeFilterCatalog {
    private final Map<ResourceLocation, List<Entry>> byTab;
    private final List<Entry> allEntries;

    CreativeFilterCatalog(Map<ResourceLocation, List<Entry>> byTab) {
        Map<ResourceLocation, List<Entry>> copy = new LinkedHashMap<>();
        byTab.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        this.byTab = java.util.Collections.unmodifiableMap(copy);
        this.allEntries = this.byTab.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public static CreativeFilterCatalog current() {
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot().orElse(null);
        if (snapshot == null) return new CreativeFilterCatalog(Map.of());
        Map<ResourceLocation, List<Entry>> entries = new LinkedHashMap<>();
        var activePacks = PackRuntimeState.registeredPackIds();
        Set<String> trainPackIds = new HashSet<>();
        for (KuaYueTrainDefinition train : KuaYueTrainCatalog.current().all()) {
            ResourceLocation tab = train.creativeTab().orElse(null);
            if (tab == null) continue;
            String packId = train.contributors().isEmpty()
                    ? train.id().getNamespace()
                    : train.contributors().getLast().ownerId();
            if (train.contributors().isEmpty()) {
                trainPackIds.add(packId);
            } else {
                train.contributors().forEach(contributor ->
                        trainPackIds.add(contributor.ownerId()));
            }
            entries.computeIfAbsent(tab, ignored -> new ArrayList<>()).add(new Entry(
                    packId,
                    train.id(),
                    tab,
                    train.nameKey(),
                    train.descriptionKey(),
                    train.creativeIconItem().map(ResourceLocation::toString).orElse(null),
                    List.of(TagKey.create(
                            Registries.ITEM,
                            SystemOriginTagIds.train(train.id())
                    )),
                    Set.of()
            ));
        }
        for (StartupPackSnapshot.ManagedPackRoot root : snapshot.resourceRoots()) {
            if (!activePacks.contains(root.id())) continue;
            if (trainPackIds.contains(root.id())) continue;
            for (ParcelManifest parcel : root.parcels().values()) {
                CreativeFilter filter = parcel.creativeFilter();
                if (filter == null) continue;
                ResourceLocation tab = ResourceLocation.parse(filter.tab());
                entries.computeIfAbsent(tab, ignored -> new ArrayList<>()).add(new Entry(
                        root.id(),
                        ResourceLocation.parse(parcel.id()),
                        tab,
                        parcel.nameKey(),
                        parcel.descriptionKey(),
                        filter.iconItem(),
                        filter.itemTags().stream()
                                .map(id -> TagKey.create(
                                        Registries.ITEM,
                                        ResourceLocation.parse(id)
                                ))
                                .toList(),
                        filter.items().stream()
                                .map(ResourceLocation::parse)
                                .collect(java.util.stream.Collectors.toUnmodifiableSet())
                ));
            }
        }
        entries.values().forEach(list -> list.sort(
                java.util.Comparator.comparing(entry -> entry.subjectId().toString())
        ));
        return new CreativeFilterCatalog(entries);
    }

    public List<Entry> entries(ResourceLocation tab) {
        return byTab.getOrDefault(tab, List.of());
    }

    public List<Entry> allEntries() {
        return allEntries;
    }

    public record Entry(
            String packId,
            ResourceLocation subjectId,
            ResourceLocation tab,
            String nameKey,
            String descriptionKey,
            String iconItem,
            List<TagKey<Item>> itemTags,
            Set<ResourceLocation> items
    ) {
        public Entry {
            Objects.requireNonNull(packId, "packId");
            Objects.requireNonNull(subjectId, "subjectId");
            Objects.requireNonNull(tab, "tab");
            Objects.requireNonNull(nameKey, "nameKey");
            itemTags = List.copyOf(itemTags);
            items = Set.copyOf(items);
        }

        public ResourceLocation iconId() {
            return iconItem == null ? null : ResourceLocation.tryParse(iconItem);
        }
    }
}
