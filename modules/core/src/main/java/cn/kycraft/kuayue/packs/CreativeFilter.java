package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Optional parcel metadata for a CreativeTab vehicle selector. */
public record CreativeFilter(
        String tab,
        String iconItem,
        List<String> itemTags,
        List<String> items
) implements Serializable {
    public CreativeFilter {
        tab = requireResourceLocation(tab, "creative_filter.tab");
        iconItem = iconItem == null
                ? null : requireResourceLocation(iconItem, "creative_filter.icon_item");
        itemTags = immutableLocations(itemTags, "creative_filter.item_tags");
        items = immutableLocations(items, "creative_filter.items");
        if (itemTags.isEmpty() && items.isEmpty()) {
            throw new IllegalArgumentException(
                    "creative_filter requires at least one item_tags or items entry");
        }
    }

    private static List<String> immutableLocations(List<String> values, String field) {
        if (values == null) return List.of();
        for (String value : values) requireResourceLocation(value, field);
        if (values.stream().distinct().count() != values.size()) {
            throw new IllegalArgumentException("Duplicate " + field + " selector");
        }
        return List.copyOf(values);
    }

    private static String requireResourceLocation(String value, String field) {
        Objects.requireNonNull(value, field);
        if (!PackPathRules.isResourceLocation(value)) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
        return value;
    }
}
