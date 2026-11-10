package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data-driven Create bogey style and its size/block/visual variants.
 *
 * @param id             stable Create style resource location
 * @param cycleGroup     Create style cycle group
 * @param registryGroup  optional registration group for the style registration
 * @param translationKey localization key for the style display name
 * @param variants       mappings from size and block to a legacy visual profile
 */
public record BogeyStyleDefinition(
        String id,
        @SerializedName("cycle_group") String cycleGroup,
        @SerializedName("registry_group") String registryGroup,
        @SerializedName("translation_key") String translationKey,
        List<Variant> variants
) {
    /**
     * Creates and validates a bogey style definition.
     */
    public BogeyStyleDefinition {
        id = BogeySchemaStrings.requireId(id, "bogey_styles[].id");
        cycleGroup = BogeySchemaStrings.requireId(cycleGroup, "bogey_styles[].cycle_group");
        registryGroup = registryGroup == null
                ? null
                : BogeySchemaStrings.requireId(registryGroup, "bogey_styles[].registry_group");
        translationKey = BogeySchemaStrings.requireText(
                translationKey,
                "bogey_styles[].translation_key"
        );
        variants = variants == null ? List.of() : List.copyOf(variants);
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("bogey_styles[].variants must not be empty");
        }
    }

    /**
     * One size/block/visual mapping in a Create bogey style.
     *
     * @param size    referenced bogey size
     * @param block   referenced bogey block
     * @param visual  Java visual bridge identifier
     */
    public record Variant(
            String size,
            String block,
            String visual
    ) {
        /**
         * Creates and validates a style variant.
         */
        public Variant {
            size = BogeySchemaStrings.requireId(size, "bogey_styles[].variants[].size");
            block = BogeySchemaStrings.requireId(block, "bogey_styles[].variants[].block");
            visual = BogeySchemaStrings.requireId(visual, "bogey_styles[].variants[].visual");
        }
    }
}
