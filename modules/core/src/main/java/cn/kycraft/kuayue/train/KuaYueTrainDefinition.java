package cn.kycraft.kuayue.train;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.source.LogicalPath;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Fully merged, immutable train domain definition with source provenance.
 */
public record KuaYueTrainDefinition(
        ResourceLocation id,
        String category,
        String nameKey,
        String descriptionKey,
        List<ResourceLocation> blocks,
        List<ResourceLocation> items,
        List<ResourceLocation> itemTags,
        List<ResourceLocation> bogeySizes,
        List<ResourceLocation> bogeyMotions,
        List<ResourceLocation> bogeyModels,
        List<ResourceLocation> bogeyBlocks,
        List<ResourceLocation> bogeyStyles,
        List<ResourceLocation> bogeyStyleVariants,
        JsonObject fields,
        List<Contributor> contributors,
        Map<String, Contributor> fieldProvenance
) {
    public KuaYueTrainDefinition {
        Objects.requireNonNull(id, "id");
        category = requireNonBlank(category, "category");
        nameKey = requireNonBlank(nameKey, "nameKey");
        descriptionKey = requireNonBlank(descriptionKey, "descriptionKey");
        blocks = immutableLocations(blocks, "blocks");
        items = immutableLocations(items, "items");
        itemTags = immutableLocations(itemTags, "itemTags");
        bogeySizes = immutableLocations(bogeySizes, "bogeySizes");
        bogeyMotions = immutableLocations(bogeyMotions, "bogeyMotions");
        bogeyModels = immutableLocations(bogeyModels, "bogeyModels");
        bogeyBlocks = immutableLocations(bogeyBlocks, "bogeyBlocks");
        bogeyStyles = immutableLocations(bogeyStyles, "bogeyStyles");
        bogeyStyleVariants = immutableLocations(
                bogeyStyleVariants,
                "bogeyStyleVariants"
        );
        fields = Objects.requireNonNull(fields, "fields").deepCopy();
        contributors = List.copyOf(contributors);
        fieldProvenance = Map.copyOf(fieldProvenance);
    }

    @Override
    public JsonObject fields() {
        return fields.deepCopy();
    }

    public JsonObject toJson() {
        JsonObject json = fields.deepCopy();
        json.addProperty(TrainDataDrivenTypes.ID, id.toString());
        return json;
    }

    public Optional<ResourceLocation> creativeTab() {
        return optionalLocation(TrainDataDrivenTypes.CREATIVE, TrainDataDrivenTypes.TAB);
    }

    public Optional<ResourceLocation> creativeIconItem() {
        return optionalLocation(
                TrainDataDrivenTypes.CREATIVE,
                TrainDataDrivenTypes.ICON_ITEM
        );
    }

    public OptionalInt creativeFilterOrder() {
        JsonElement creative = fields.get(TrainDataDrivenTypes.CREATIVE);
        if (creative == null || !creative.isJsonObject()) {
            return OptionalInt.empty();
        }
        JsonElement order = creative.getAsJsonObject().get(TrainDataDrivenTypes.FILTER_ORDER);
        return order != null && order.isJsonPrimitive()
                && order.getAsJsonPrimitive().isNumber()
                ? OptionalInt.of(order.getAsInt())
                : OptionalInt.empty();
    }

    private Optional<ResourceLocation> optionalLocation(String parent, String field) {
        var parentElement = fields.get(parent);
        if (parentElement == null || !parentElement.isJsonObject()) {
            return Optional.empty();
        }
        var value = parentElement.getAsJsonObject().get(field);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            return Optional.empty();
        }
        return Optional.of(ResourceLocation.parse(value.getAsString()));
    }

    private static List<ResourceLocation> immutableLocations(
            List<ResourceLocation> locations,
            String field
    ) {
        Objects.requireNonNull(locations, field);
        Set<ResourceLocation> unique = new HashSet<>();
        for (ResourceLocation location : locations) {
            if (!unique.add(Objects.requireNonNull(location, field + " entry"))) {
                throw new IllegalArgumentException(field + " must not contain duplicates");
            }
        }
        return List.copyOf(locations);
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /**
     * The exact source layer that contributed to the merged train.
     */
    public record Contributor(
            String ownerId,
            String sourceId,
            String rootId,
            int sourceOrder,
            LogicalPath logicalPath,
            String jsonPointer,
            int declarationOrder
    ) {
        public Contributor {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(rootId, "rootId");
            Objects.requireNonNull(logicalPath, "logicalPath");
            Objects.requireNonNull(jsonPointer, "jsonPointer");
        }
    }
}
