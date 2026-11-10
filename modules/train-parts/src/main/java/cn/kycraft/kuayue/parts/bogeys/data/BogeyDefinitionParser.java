package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.json.DataJson;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Parses and validates the registration-stage bogey schemas without Minecraft runtime state.
 */
public final class BogeyDefinitionParser {
    private static final Set<String> FILE_FIELDS = Set.of(
            "bogey_sizes", "bogey_blocks", "bogey_styles"
    );
    private static final Set<String> SIZE_FIELDS = Set.of("id", "wheel_radius");
    private static final Set<String> BLOCK_FIELDS = Set.of(
            "id", "size", "registry_group", "default_style", "properties", "physics"
    );
    private static final Set<String> PHYSICS_FIELDS = Set.of(
            "primary_track_type",
            "valid_track_types",
            "wheel_point_spacing",
            "connector_anchor_offset",
            "allow_single_bogey_carriage"
    );
    private static final Set<String> STYLE_FIELDS = Set.of(
            "id", "cycle_group", "registry_group", "translation_key", "variants"
    );
    private static final Set<String> VARIANT_FIELDS = Set.of("size", "block", "visual");

    /**
     * Parses one {@code bogey_sizes} entry.
     */
    public BogeySizeDefinition parseSize(JsonObject json) {
        rejectUnknownFields(json, SIZE_FIELDS, "bogey_sizes[]");
        return DataJson.GSON.fromJson(json, BogeySizeDefinition.class);
    }

    /**
     * Parses one {@code bogey_blocks} entry.
     */
    public BogeyBlockDefinition parseBlock(JsonObject json) {
        rejectUnknownFields(json, BLOCK_FIELDS, "bogey_blocks[]");
        JsonObject physics = requireObject(json, "physics", "bogey_blocks[]");
        rejectUnknownFields(physics, PHYSICS_FIELDS, "bogey_blocks[].physics");
        return DataJson.GSON.fromJson(json, BogeyBlockDefinition.class);
    }

    /**
     * Parses one {@code bogey_styles} entry.
     */
    public BogeyStyleDefinition parseStyle(JsonObject json) {
        rejectUnknownFields(json, STYLE_FIELDS, "bogey_styles[]");
        for (JsonElement variant : requireArray(json, "variants", "bogey_styles[]")) {
            if (!variant.isJsonObject()) {
                throw new IllegalArgumentException(
                        "bogey_styles[].variants[] entries must be JSON objects"
                );
            }
            rejectUnknownFields(
                    variant.getAsJsonObject(),
                    VARIANT_FIELDS,
                    "bogey_styles[].variants[]"
            );
        }
        return DataJson.GSON.fromJson(json, BogeyStyleDefinition.class);
    }

    /**
     * Parses and cross-checks one complete bogey definition document.
     */
    public BogeyDefinitionFile parseFile(JsonObject root) {
        rejectUnknownFields(root, FILE_FIELDS, "bogey document");
        BogeyDefinitionFile file = new BogeyDefinitionFile(
                parseEntries(root, "bogey_sizes", this::parseSize),
                parseEntries(root, "bogey_blocks", this::parseBlock),
                parseEntries(root, "bogey_styles", this::parseStyle)
        );
        Set<String> sizeIds = uniqueIds(
                file.sizes().stream().map(BogeySizeDefinition::id).toList(),
                "bogey_sizes[].id"
        );
        Set<String> blockIds = uniqueIds(
                file.blocks().stream().map(BogeyBlockDefinition::id).toList(),
                "bogey_blocks[].id"
        );
        Set<String> styleIds = uniqueIds(
                file.styles().stream().map(BogeyStyleDefinition::id).toList(),
                "bogey_styles[].id"
        );

        Map<String, BogeyBlockDefinition> blocksById = new LinkedHashMap<>();
        for (BogeyBlockDefinition block : file.blocks()) {
            requireReference(sizeIds, block.size(), "bogey block " + block.id() + " size");
            requireReference(styleIds, block.defaultStyle(),
                    "bogey block " + block.id() + " default style");
            blocksById.put(block.id(), block);
        }

        Set<String> styledBlocks = new HashSet<>();
        for (BogeyStyleDefinition style : file.styles()) {
            Set<String> styleSizes = new HashSet<>();
            for (BogeyStyleDefinition.Variant variant : style.variants()) {
                requireReference(sizeIds, variant.size(), "style " + style.id() + " size");
                requireReference(blockIds, variant.block(), "style " + style.id() + " block");
                if (!styleSizes.add(variant.size())) {
                    throw new IllegalArgumentException(
                            "style " + style.id() + " configures size twice: " + variant.size()
                    );
                }
                if (!styledBlocks.add(variant.block())) {
                    throw new IllegalArgumentException(
                            "bogey block appears in more than one style variant: " + variant.block()
                    );
                }
                BogeyBlockDefinition block = blocksById.get(variant.block());
                if (!block.size().equals(variant.size())) {
                    throw new IllegalArgumentException(
                            "style " + style.id() + " size " + variant.size()
                                    + " does not match block " + block.id() + " size "
                                    + block.size()
                    );
                }
                if (!block.defaultStyle().equals(style.id())) {
                    throw new IllegalArgumentException(
                            "block " + block.id() + " default style is "
                                    + block.defaultStyle() + ", not " + style.id()
                    );
                }
            }
        }
        if (!styledBlocks.equals(blockIds)) {
            Set<String> missing = new HashSet<>(blockIds);
            missing.removeAll(styledBlocks);
            throw new IllegalArgumentException(
                    "bogey blocks are missing from style variants: " + missing
            );
        }
        return file;
    }

    private <T> List<T> parseEntries(
            JsonObject root,
            String field,
            Function<JsonObject, T> parser
    ) {
        if (!root.has(field)) {
            return List.of();
        }
        if (!root.get(field).isJsonArray()) {
            throw new IllegalArgumentException("bogey document." + field + " must be an array");
        }
        List<T> result = new java.util.ArrayList<>();
        for (JsonElement element : root.getAsJsonArray(field)) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException(
                        "bogey document." + field + "[] entries must be JSON objects"
                );
            }
            result.add(parser.apply(element.getAsJsonObject()));
        }
        return List.copyOf(result);
    }

    private static Set<String> uniqueIds(List<String> ids, String field) {
        Set<String> result = new HashSet<>();
        for (String id : ids) {
            if (!result.add(id)) {
                throw new IllegalArgumentException(field + " contains duplicate id " + id);
            }
        }
        return Set.copyOf(result);
    }

    private static void requireReference(Set<String> ids, String id, String description) {
        if (!ids.contains(id)) {
            throw new IllegalArgumentException(description + " references unknown id " + id);
        }
    }

    private static JsonObject requireObject(JsonObject parent, String field, String path) {
        if (!parent.has(field) || !parent.get(field).isJsonObject()) {
            throw new IllegalArgumentException(path + "." + field + " must be an object");
        }
        return parent.getAsJsonObject(field);
    }

    private static com.google.gson.JsonArray requireArray(
            JsonObject parent,
            String field,
            String path
    ) {
        if (!parent.has(field) || !parent.get(field).isJsonArray()) {
            throw new IllegalArgumentException(path + "." + field + " must be an array");
        }
        return parent.getAsJsonArray(field);
    }

    private static void rejectUnknownFields(JsonObject json, Set<String> allowed, String path) {
        for (String field : json.keySet()) {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException(
                        path + " contains unsupported field '" + field
                                + "'; supported fields are " + allowed
                );
            }
        }
    }
}
