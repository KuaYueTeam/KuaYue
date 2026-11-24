package cn.kycraft.kuayue.train;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Immutable train catalog with reverse indexes and one atomically replaceable process current value.
 */
public final class KuaYueTrainCatalog {
    private static final AtomicReference<KuaYueTrainCatalog> CURRENT =
            new AtomicReference<>(new KuaYueTrainCatalog(List.of()));

    private final Map<ResourceLocation, KuaYueTrainDefinition> byId;
    private final List<KuaYueTrainDefinition> all;
    private final Map<ResourceLocation, List<KuaYueTrainDefinition>> byBlock;
    private final Map<ResourceLocation, List<KuaYueTrainDefinition>> byItem;
    private final Map<ResourceLocation, List<KuaYueTrainDefinition>> byBogeyBlock;
    private final Map<ResourceLocation, List<KuaYueTrainDefinition>> byBogeyStyle;

    private KuaYueTrainCatalog(Collection<KuaYueTrainDefinition> definitions) {
        Map<ResourceLocation, KuaYueTrainDefinition> ids = new TreeMap<>(
                ResourceLocation::compareTo
        );
        for (KuaYueTrainDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            KuaYueTrainDefinition previous = ids.putIfAbsent(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate train id: " + definition.id());
            }
        }

        byId = Map.copyOf(ids);
        all = List.copyOf(ids.values());
        byBlock = index(all, KuaYueTrainDefinition::blocks);
        byItem = index(all, KuaYueTrainDefinition::items);
        byBogeyBlock = index(all, KuaYueTrainDefinition::bogeyBlocks);
        byBogeyStyle = index(all, KuaYueTrainDefinition::bogeyStyles);
    }

    public static KuaYueTrainCatalog of(
            Collection<KuaYueTrainDefinition> definitions
    ) {
        return new KuaYueTrainCatalog(definitions);
    }

    public static KuaYueTrainCatalog current() {
        return CURRENT.get();
    }

    public static KuaYueTrainCatalog install(KuaYueTrainCatalog catalog) {
        return CURRENT.getAndSet(Objects.requireNonNull(catalog, "catalog"));
    }

    public static void clear() {
        CURRENT.set(new KuaYueTrainCatalog(List.of()));
    }

    public static void resetForTests() {
        clear();
    }

    public Optional<KuaYueTrainDefinition> find(ResourceLocation id) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id")));
    }

    public Optional<KuaYueTrainDefinition> find(String id) {
        return find(ResourceLocation.parse(Objects.requireNonNull(id, "id")));
    }

    public List<KuaYueTrainDefinition> all() {
        return all;
    }

    public List<KuaYueTrainDefinition> byBlock(ResourceLocation id) {
        return lookup(byBlock, id);
    }

    public List<KuaYueTrainDefinition> byItem(ResourceLocation id) {
        return lookup(byItem, id);
    }

    public List<KuaYueTrainDefinition> byBogeyBlock(ResourceLocation id) {
        return lookup(byBogeyBlock, id);
    }

    public List<KuaYueTrainDefinition> byBogeyStyle(ResourceLocation id) {
        return lookup(byBogeyStyle, id);
    }

    public List<KuaYueTrainDefinition> byBlock(String id) {
        return byBlock(ResourceLocation.parse(id));
    }

    public List<KuaYueTrainDefinition> byItem(String id) {
        return byItem(ResourceLocation.parse(id));
    }

    public List<KuaYueTrainDefinition> byBogeyBlock(String id) {
        return byBogeyBlock(ResourceLocation.parse(id));
    }

    public List<KuaYueTrainDefinition> byBogeyStyle(String id) {
        return byBogeyStyle(ResourceLocation.parse(id));
    }

    private static List<KuaYueTrainDefinition> lookup(
            Map<ResourceLocation, List<KuaYueTrainDefinition>> index,
            ResourceLocation id
    ) {
        return index.getOrDefault(Objects.requireNonNull(id, "id"), List.of());
    }

    private static Map<ResourceLocation, List<KuaYueTrainDefinition>> index(
            Collection<KuaYueTrainDefinition> definitions,
            Function<KuaYueTrainDefinition, List<ResourceLocation>> values
    ) {
        Map<ResourceLocation, List<KuaYueTrainDefinition>> mutable = new LinkedHashMap<>();
        for (KuaYueTrainDefinition definition : definitions) {
            for (ResourceLocation id : values.apply(definition)) {
                mutable.computeIfAbsent(id, ignored -> new ArrayList<>()).add(definition);
            }
        }

        Map<ResourceLocation, List<KuaYueTrainDefinition>> immutable =
                new LinkedHashMap<>();
        mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return Map.copyOf(immutable);
    }
}
