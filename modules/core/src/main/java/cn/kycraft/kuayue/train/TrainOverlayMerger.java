package cn.kycraft.kuayue.train;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.pipeline.PreparedContent;
import lib.kasuga.registration.data_driven.pipeline.LocatedDefinition;
import lib.kasuga.registration.data_driven.TypeHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Folds train patches from the lowest to the highest source priority.
 */
public final class TrainOverlayMerger {
    private static final String MEMBERS_POINTER = "/" + TrainDataDrivenTypes.MEMBERS;
    private static final List<String> REQUIRED_MEMBER_LISTS = List.of(
            TrainDataDrivenTypes.BLOCKS,
            TrainDataDrivenTypes.ITEMS,
            TrainDataDrivenTypes.ITEM_TAGS,
            TrainDataDrivenTypes.BOGEY_SIZES,
            TrainDataDrivenTypes.BOGEY_MOTIONS,
            TrainDataDrivenTypes.BOGEY_MODELS,
            TrainDataDrivenTypes.BOGEY_BLOCKS,
            TrainDataDrivenTypes.BOGEY_STYLES,
            TrainDataDrivenTypes.BOGEY_STYLE_VARIANTS
    );

    public List<KuaYueTrainDefinition> mergeAll(
            List<LocatedDefinition<TrainPatch>> locatedPatches
    ) {
        Objects.requireNonNull(locatedPatches, "locatedPatches");
        Map<String, List<LocatedDefinition<TrainPatch>>> byId = new TreeMap<>();
        for (LocatedDefinition<TrainPatch> located : locatedPatches) {
            Objects.requireNonNull(located, "located patch");
            byId.computeIfAbsent(located.definition().id(), ignored -> new ArrayList<>())
                    .add(located);
        }

        List<KuaYueTrainDefinition> definitions = new ArrayList<>(byId.size());
        for (List<LocatedDefinition<TrainPatch>> group : byId.values()) {
            definitions.add(merge(group));
        }
        return List.copyOf(definitions);
    }

    public KuaYueTrainCatalog compile(PreparedContent prepared) {
        Objects.requireNonNull(prepared, "prepared");
        List<LocatedDefinition<TrainPatch>> patches = new ArrayList<>();
        for (LocatedDefinition<?> located : prepared.definitions()) {
            if (located.definition() instanceof TrainPatch patch) {
                patches.add(asTrainPatch(located, patch));
            }
        }
        return KuaYueTrainCatalog.of(mergeAll(patches));
    }

    public KuaYueTrainDefinition merge(List<LocatedDefinition<TrainPatch>> locatedPatches) {
        if (locatedPatches == null || locatedPatches.isEmpty()) {
            throw new IllegalArgumentException("at least one train patch is required");
        }

        List<LocatedDefinition<TrainPatch>> ordered = new ArrayList<>(locatedPatches);
        ordered.sort(Comparator
                .comparingInt(LocatedDefinition<TrainPatch>::sourceOrder)
                .thenComparingInt(LocatedDefinition<TrainPatch>::declarationOrder));

        String id = ordered.getFirst().definition().id();
        JsonObject merged = new JsonObject();
        List<KuaYueTrainDefinition.Contributor> contributors = new ArrayList<>(ordered.size());
        Map<String, KuaYueTrainDefinition.Contributor> fieldProvenance =
                new LinkedHashMap<>();
        Set<String> owners = new HashSet<>();

        for (LocatedDefinition<TrainPatch> located : ordered) {
            TrainPatch patch = Objects.requireNonNull(located.definition(), "patch");
            if (!id.equals(patch.id())) {
                throw new IllegalArgumentException(
                        "cannot merge train " + id + " with patch for " + patch.id()
                );
            }
            String owner = located.source().ownerId();
            if (!owners.add(owner)) {
                throw new MergeException(
                        id,
                        List.of("/id: owner '" + owner + "' declares this train more than once")
                );
            }

            KuaYueTrainDefinition.Contributor contributor =
                    new KuaYueTrainDefinition.Contributor(
                            owner,
                            located.source().sourceId(),
                            located.source().rootId(),
                            located.sourceOrder(),
                            located.logicalPath(),
                            located.jsonPointer(),
                            located.declarationOrder()
                    );
            mergeObject(merged, patch.declaredFields(), "", fieldProvenance, contributor);
            contributors.add(contributor);
        }

        JsonObject normalized = normalizeObject(merged).getAsJsonObject();
        validateComplete(id, normalized);
        JsonObject members = normalized.getAsJsonObject(TrainDataDrivenTypes.MEMBERS);

        return new KuaYueTrainDefinition(
                ResourceLocation.parse(id),
                normalized.get(TrainDataDrivenTypes.CATEGORY).getAsString(),
                normalized.get(TrainDataDrivenTypes.NAME_KEY).getAsString(),
                normalized.get(TrainDataDrivenTypes.DESCRIPTION_KEY).getAsString(),
                locations(members, TrainDataDrivenTypes.BLOCKS),
                locations(members, TrainDataDrivenTypes.ITEMS),
                locations(members, TrainDataDrivenTypes.ITEM_TAGS),
                locations(members, TrainDataDrivenTypes.BOGEY_SIZES),
                locations(members, TrainDataDrivenTypes.BOGEY_MOTIONS),
                locations(members, TrainDataDrivenTypes.BOGEY_MODELS),
                locations(members, TrainDataDrivenTypes.BOGEY_BLOCKS),
                locations(members, TrainDataDrivenTypes.BOGEY_STYLES),
                locations(members, TrainDataDrivenTypes.BOGEY_STYLE_VARIANTS),
                normalized,
                contributors,
                fieldProvenance
        );
    }

    private static void mergeObject(
            JsonObject target,
            JsonObject source,
            String pointer,
            Map<String, KuaYueTrainDefinition.Contributor> provenance,
            KuaYueTrainDefinition.Contributor contributor
    ) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            JsonElement incoming = entry.getValue();
            JsonElement existing = target.get(entry.getKey());
            String childPointer = pointer + "/" + escape(entry.getKey());
            if (existing != null
                    && existing.isJsonObject()
                    && incoming.isJsonObject()) {
                mergeObject(
                        existing.getAsJsonObject(),
                        incoming.getAsJsonObject(),
                        childPointer,
                        provenance,
                        contributor
                );
            } else {
                target.add(entry.getKey(), incoming.deepCopy());
                recordProvenance(childPointer, incoming, provenance, contributor);
            }
        }
    }

    private static void recordProvenance(
            String pointer,
            JsonElement value,
            Map<String, KuaYueTrainDefinition.Contributor> provenance,
            KuaYueTrainDefinition.Contributor contributor
    ) {
        if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                recordProvenance(
                        pointer + "/" + escape(entry.getKey()),
                        entry.getValue(),
                        provenance,
                        contributor
                );
            }
        } else {
            provenance.put(pointer, contributor);
        }
    }

    private static JsonElement normalize(JsonElement element) {
        if (element.isJsonObject()) {
            return normalizeObject(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            Set<String> seen = new LinkedHashSet<>();
            for (JsonElement child : element.getAsJsonArray()) {
                JsonElement normalized = normalize(child);
                if (seen.add(normalized.toString())) {
                    result.add(normalized);
                }
            }
            return result;
        }
        return element.deepCopy();
    }

    private static JsonObject normalizeObject(JsonObject object) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            result.add(entry.getKey(), normalize(entry.getValue()));
        }
        return result;
    }

    private static void validateComplete(String id, JsonObject merged) {
        List<String> violations = new ArrayList<>();
        validateRequiredText(merged, TrainDataDrivenTypes.CATEGORY, "/category", violations);
        validateRequiredText(merged, TrainDataDrivenTypes.NAME_KEY, "/name_key", violations);
        validateRequiredText(
                merged,
                TrainDataDrivenTypes.DESCRIPTION_KEY,
                "/description_key",
                violations
        );
        JsonElement membersElement = merged.get(TrainDataDrivenTypes.MEMBERS);
        if (membersElement == null || !membersElement.isJsonObject()) {
            violations.add(MEMBERS_POINTER + ": expected an object");
        } else {
            JsonObject members = membersElement.getAsJsonObject();
            for (String field : REQUIRED_MEMBER_LISTS) {
                String pointer = MEMBERS_POINTER + "/" + field;
                JsonElement value = members.get(field);
                if (value == null) {
                    violations.add(pointer + ": required field was not declared by any patch");
                } else if (!value.isJsonArray()) {
                    violations.add(pointer + ": expected an array");
                } else {
                    validateResourceArray(value.getAsJsonArray(), pointer, violations);
                }
            }
        }

        validateCreative(merged.get(TrainDataDrivenTypes.CREATIVE), violations);

        if (!violations.isEmpty()) {
            throw new MergeException(id, violations);
        }
    }

    private static void validateRequiredText(
            JsonObject object,
            String field,
            String pointer,
            List<String> violations
    ) {
        JsonElement value = object.get(field);
        if (value == null) {
            violations.add(pointer + ": required field was not declared by any patch");
        } else if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            violations.add(pointer + ": expected a non-blank string");
        }
    }

    private static void validateCreative(JsonElement element, List<String> violations) {
        if (element == null) {
            return;
        }
        if (!element.isJsonObject()) {
            violations.add("/creative: expected an object");
            return;
        }
        JsonObject creative = element.getAsJsonObject();
        validateRequiredResource(
                creative,
                TrainDataDrivenTypes.TAB,
                "/creative/tab",
                violations
        );
        validateOptionalResource(
                creative,
                TrainDataDrivenTypes.ICON_ITEM,
                "/creative/icon_item",
                violations
        );
    }

    private static void validateRequiredResource(
            JsonObject object,
            String field,
            String pointer,
            List<String> violations
    ) {
        JsonElement value = object.get(field);
        if (value == null) {
            violations.add(pointer + ": required field was not declared by any patch");
            return;
        }
        validateResourceValue(value, pointer, violations);
    }

    private static void validateOptionalResource(
            JsonObject object,
            String field,
            String pointer,
            List<String> violations
    ) {
        JsonElement value = object.get(field);
        if (value != null) {
            validateResourceValue(value, pointer, violations);
        }
    }

    private static void validateResourceValue(
            JsonElement value,
            String pointer,
            List<String> violations
    ) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            violations.add(pointer + ": expected a namespaced string resource id");
            return;
        }
        try {
            TrainPatchParser.parseExplicitId(value.getAsString(), pointer);
        } catch (IllegalArgumentException exception) {
            violations.add(exception.getMessage());
        }
    }

    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    private static void validateResourceArray(
            JsonArray array,
            String pointer,
            List<String> violations
    ) {
        int index = 0;
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                violations.add(pointer + "/" + index
                        + ": expected a namespaced string resource id");
            } else {
                try {
                    TrainPatchParser.parseExplicitId(element.getAsString(), pointer + "/" + index);
                } catch (IllegalArgumentException exception) {
                    violations.add(exception.getMessage());
                }
            }
            index++;
        }
    }

    private static List<ResourceLocation> locations(JsonObject members, String field) {
        JsonArray array = members.getAsJsonArray(field);
        List<ResourceLocation> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            result.add(ResourceLocation.parse(element.getAsString()));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static LocatedDefinition<TrainPatch> asTrainPatch(
            LocatedDefinition<?> located,
            TrainPatch patch
    ) {
        return new LocatedDefinition<TrainPatch>(
                located.key(),
                (TypeHandler<TrainPatch>) located.handler(),
                patch,
                located.source(),
                located.sourceOrder(),
                located.logicalPath(),
                located.jsonPointer(),
                located.declarationOrder()
        );
    }

    public static final class MergeException extends IllegalArgumentException {
        private final String trainId;
        private final List<String> violations;

        public MergeException(String trainId, List<String> violations) {
            super("cannot compile train " + trainId + ": " + String.join("; ", violations));
            this.trainId = Objects.requireNonNull(trainId, "trainId");
            this.violations = List.copyOf(violations);
        }

        public String trainId() {
            return trainId;
        }

        public List<String> violations() {
            return violations;
        }
    }
}
