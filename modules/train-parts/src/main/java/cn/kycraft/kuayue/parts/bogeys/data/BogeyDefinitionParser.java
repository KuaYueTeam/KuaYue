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
    private static final Set<String> MOTION_FIELDS = Set.of(
            "id", "type", "scale", "sign", "clip"
    );
    private static final Set<String> MODEL_FIELDS = Set.of("id", "y_offset", "parts");
    private static final Set<String> MODEL_PART_FIELDS = Set.of(
            "id", "model", "local", "motion"
    );
    private static final Set<String> LOCAL_OPERATION_FIELDS = Set.of(
            "translate", "rotate", "scale"
    );
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
    private static final Set<String> VARIANT_FIELDS = Set.of("size", "block", "model");

    /**
     * Parses one {@code bogey_sizes} entry.
     */
    public BogeySizeDefinition parseSize(JsonObject json) {
        rejectUnknownFields(json, SIZE_FIELDS, "bogey_sizes[]");
        return DataJson.GSON.fromJson(json, BogeySizeDefinition.class);
    }

    /**
     * Parses one {@code bogey_motions} entry.
     */
    public BogeyMotionDefinition parseMotion(JsonObject json) {
        rejectUnknownFields(json, MOTION_FIELDS, "bogey_motions[]");
        JsonObject normalized = json.deepCopy();
        if (!normalized.has("scale")) {
            normalized.addProperty("scale", 1.0f);
        }
        if (!normalized.has("sign")) {
            normalized.addProperty("sign", 1.0f);
        }
        if (!normalized.has("type")
                || !normalized.get("type").isJsonPrimitive()
                || !normalized.getAsJsonPrimitive("type").isString()) {
            throw new IllegalArgumentException("bogey_motions[].type must be a string");
        }
        String type = normalized.get("type").getAsString();
        if (!Set.of("wheel_spin", "rod_rotation", "formula").contains(type)) {
            throw new IllegalArgumentException(
                    "Unsupported bogey motion type: " + type
            );
        }
        if (type.equals("formula")) {
            if (!normalized.has("clip")
                    || !normalized.get("clip").isJsonPrimitive()
                    || !normalized.getAsJsonPrimitive("clip").isString()
                    || normalized.get("clip").getAsString().isBlank()) {
                throw new IllegalArgumentException(
                        "bogey_motions[].clip must be a non-blank string for formula motions"
                );
            }
        } else if (normalized.has("clip")) {
            throw new IllegalArgumentException(
                    "bogey_motions[].clip is only valid for formula motions"
            );
        }
        return DataJson.GSON.fromJson(normalized, BogeyMotionDefinition.class);
    }

    /**
     * Parses one {@code bogey_models} entry.
     */
    public BogeyModelDefinition parseModel(JsonObject json) {
        rejectUnknownFields(json, MODEL_FIELDS, "bogey_models[]");
        JsonObject normalized = json.deepCopy();
        if (!normalized.has("y_offset")) {
            normalized.addProperty("y_offset", -1.5078125f);
        }
        for (JsonElement part : requireArray(normalized, "parts", "bogey_models[]")) {
            if (!part.isJsonObject()) {
                throw new IllegalArgumentException(
                        "bogey_models[].parts[] entries must be JSON objects"
                );
            }
            JsonObject partObject = part.getAsJsonObject();
            rejectUnknownFields(
                    partObject,
                    MODEL_PART_FIELDS,
                    "bogey_models[].parts[]"
            );
            if (partObject.has("local")) {
                if (!partObject.get("local").isJsonArray()) {
                    throw new IllegalArgumentException(
                            "bogey_models[].parts[].local must be an array"
                    );
                }
                for (JsonElement operation : partObject.getAsJsonArray("local")) {
                    if (!operation.isJsonObject()) {
                        throw new IllegalArgumentException(
                                "bogey_models[].parts[].local[] entries must be JSON objects"
                        );
                    }
                    rejectUnknownFields(
                            operation.getAsJsonObject(),
                            LOCAL_OPERATION_FIELDS,
                            "bogey_models[].parts[].local[]"
                    );
                }
            }
        }
        return DataJson.GSON.fromJson(normalized, BogeyModelDefinition.class);
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

    /**
     * Parses and validates one motion document.
     */
    public List<BogeyMotionDefinition> parseMotionFile(JsonObject root) {
        rejectUnknownFields(root, Set.of("bogey_motions"), "bogey motion document");
        List<BogeyMotionDefinition> motions =
                parseEntries(root, "bogey_motions", this::parseMotion);
        uniqueIds(
                motions.stream().map(BogeyMotionDefinition::id).toList(),
                "bogey_motions[].id"
        );
        return motions;
    }

    /**
     * Parses and validates one model document against known motion IDs.
     */
    public List<BogeyModelDefinition> parseModelFile(
            JsonObject root,
            Set<String> motionIds
    ) {
        rejectUnknownFields(root, Set.of("bogey_models"), "bogey model document");
        List<BogeyModelDefinition> models =
                parseEntries(root, "bogey_models", this::parseModel);
        uniqueIds(
                models.stream().map(BogeyModelDefinition::id).toList(),
                "bogey_models[].id"
        );
        for (BogeyModelDefinition model : models) {
            for (BogeyModelDefinition.Part part : model.parts()) {
                if (part.motion() != null) {
                    requireReference(
                            motionIds,
                            part.motion(),
                            "model " + model.id() + " part " + part.id() + " motion"
                    );
                }
            }
        }
        return models;
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
